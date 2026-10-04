package com.gaiagauntlet.gauntlet.games.EliminationZone.zones;

import com.gaiagauntlet.gg.zone.ZoneDefinition;
import com.hypixel.hytale.assetstore.map.AssetMapWithIndexes;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockFace;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.universe.world.SetBlockSettings;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Repaints the ground behind a closing edge with void blocks. Each advance walks the columns that
 * became void since the last one, queues every occupied block in them, and replaces those blocks
 * through the zones file's mapping. Chunks that are not loaded are fetched in the background and
 * the blocks wait until they arrive. State is per match; the replacement caches live for the server.
 */
public final class VoidTerrain {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int PAINT_SETTINGS = SetBlockSettings.NO_SEND_PARTICLES | SetBlockSettings.NO_SEND_AUDIO
            | SetBlockSettings.NO_DROP_ITEMS;
    private static final int MAX_INFLIGHT_LOADS = 16;
    private static final int MAX_PINNED_CHUNKS = 256;
    private static final int MAX_LOAD_ATTEMPTS = 3;

    /** A zone and how far its edge has come in: everything from the edge to the outer radius is void. */
    public record Band(@Nonnull ZoneDefinition zone, double edgeRadius) {
    }

    /** Whether a block-center point is in the void right now. */
    public interface VoidTest {
        boolean isInVoid(double x, double y, double z);
    }

    private record Replacement(@Nonnull BlockType type, int index) {
    }

    private record FluidReplacement(@Nullable Replacement block, int fluidIndex, byte level) {
        boolean isFluid() {
            return fluidIndex != Fluid.EMPTY_ID;
        }
    }

    private static final Map<Long, Replacement> BLOCK_CACHE = new HashMap<>();
    private static final Map<Integer, FluidReplacement> FLUID_CACHE = new HashMap<>();

    private final LongSet painted = new LongOpenHashSet();
    private final Map<String, Double> lastEdge = new HashMap<>();
    private final ArrayDeque<Vector3i> spreadQueue = new ArrayDeque<>();
    private int paintedBlocks;

    private final LongSet abandoned = new LongOpenHashSet();
    private final LongSet inflight = new LongOpenHashSet();
    private final Long2IntOpenHashMap failures = new Long2IntOpenHashMap();
    private final LinkedHashMap<Long, WorldChunk> pinned = new LinkedHashMap<>(64, 0.75f, true);
    private int loadedChunks;
    private int failedChunks;

    public void reset() {
        for (var chunk : pinned.values()) {
            chunk.removeKeepLoaded();
        }
        pinned.clear();
        painted.clear();
        lastEdge.clear();
        spreadQueue.clear();
        abandoned.clear();
        inflight.clear();
        failures.clear();
        if (paintedBlocks > 0 || loadedChunks > 0) {
            LOGGER.atInfo().log("Void terrain painted %d blocks and pulled in %d chunks (%d failed loads)",
                    paintedBlocks, loadedChunks, failedChunks);
        }
        paintedBlocks = 0;
        loadedChunks = 0;
        failedChunks = 0;
    }

    public int paintedBlocks() {
        return paintedBlocks;
    }

    public int queuedBlocks() {
        return spreadQueue.size();
    }

    /** Queues the columns that became void since the last advance and paints what can be painted now. */
    public void advance(@Nonnull World world, @Nonnull List<Band> bands, @Nonnull VoidTest test) {
        var file = Zones.get().file();
        var voidType = resolveVoidBlock(file.voidBlock());
        if (voidType == null) {
            return;
        }
        var newColumns = new LongArrayList();
        for (var band : bands) {
            var zone = band.zone();
            double previous = lastEdge.getOrDefault(zone.id(), zone.outerRadius());
            double current = band.edgeRadius();
            if (current < previous) {
                forEachColumnInBand(zone, current, previous + 1, (x, z) -> {
                    long column = packColumn(x, z);
                    if (!painted.contains(column) && test.isInVoid(x + 0.5, zone.minY() + 1, z + 0.5)) {
                        newColumns.add(column);
                    }
                });
            }
        }
        lastEdge.clear();
        for (var band : bands) {
            lastEdge.put(band.zone().id(), band.edgeRadius());
        }
        var fluids = new FluidCursor(world);
        for (int i = 0; i < newColumns.size(); i++) {
            long column = newColumns.getLong(i);
            int x = unpackX(column);
            int z = unpackZ(column);
            var chunk = chunkFor(world, x, z);
            if (chunk == null) {
                continue;
            }
            painted.add(column);
            int top = topFor(bands, x, z);
            for (int y = ChunkUtil.MIN_Y; y <= top; y++) {
                if (test.isInVoid(x + 0.5, y + 0.5, z + 0.5)
                        && (chunk.getBlock(x, y, z) != BlockType.EMPTY_ID || fluids.isFluid(x, y, z))) {
                    spreadQueue.add(new Vector3i(x, y, z));
                }
            }
        }
        drain(world, voidType, file.voidBlockMap(), fluids);
    }

    private static int topFor(@Nonnull List<Band> bands, int x, int z) {
        int top = ChunkUtil.MIN_Y;
        for (var band : bands) {
            var zone = band.zone();
            if (zone.isInSweep(x + 0.5 - zone.centerX(), z + 0.5 - zone.centerZ())) {
                top = Math.max(top, (int) Math.min(ChunkUtil.HEIGHT - 1, zone.maxY()));
            }
        }
        return top;
    }

    private void drain(@Nonnull World world, @Nonnull BlockType voidType, @Nonnull Map<String, String> mappings,
            @Nonnull FluidCursor fluids) {
        var retry = new ArrayList<Vector3i>();
        while (!spreadQueue.isEmpty()) {
            var block = spreadQueue.removeFirst();
            var chunk = chunkFor(world, block.x(), block.z());
            if (chunk == null) {
                long index = ChunkUtil.indexChunkFromBlock(block.x(), block.z());
                if (!abandoned.contains(index)) {
                    retry.add(block);
                }
                continue;
            }
            int blockId = chunk.getBlock(block.x(), block.y(), block.z());
            int fluidId = fluids.idAt(block.x(), block.y(), block.z());
            if (blockId == BlockType.EMPTY_ID && fluidId == Fluid.EMPTY_ID) {
                continue;
            }
            boolean changed = false;
            int rotation = 0;
            if (blockId != BlockType.EMPTY_ID) {
                rotation = chunk.getRotationIndex(block.x(), block.y(), block.z());
                var replacement = blockReplacement(blockId, rotation, mappings, voidType);
                if (replacement != null && blockId != replacement.index()) {
                    chunk.setBlock(block.x(), block.y(), block.z(), replacement.index(), replacement.type(), rotation,
                            FillerBlockUtil.NO_FILLER, PAINT_SETTINGS);
                    changed = true;
                }
            }
            if (fluidId != Fluid.EMPTY_ID) {
                var replacement = fluidReplacement(fluidId, mappings, voidType);
                if (replacement != null && replacement.isFluid()) {
                    if (replacement.fluidIndex() != fluidId) {
                        changed |= fluids.set(block.x(), block.y(), block.z(), replacement.fluidIndex(), replacement.level());
                    }
                } else {
                    changed |= fluids.clear(block.x(), block.y(), block.z());
                    if (blockId == BlockType.EMPTY_ID && replacement != null && replacement.block() != null) {
                        chunk.setBlock(block.x(), block.y(), block.z(), replacement.block().index(), replacement.block().type(),
                                0, FillerBlockUtil.NO_FILLER, PAINT_SETTINGS);
                        changed = true;
                    }
                }
            }
            if (changed) {
                paintedBlocks++;
            }
        }
        spreadQueue.addAll(retry);
    }

    // Chunk access

    @Nullable
    private WorldChunk chunkFor(@Nonnull World world, int blockX, int blockZ) {
        long index = ChunkUtil.indexChunkFromBlock(blockX, blockZ);
        var chunk = world.getChunkIfLoaded(index);
        if (chunk == null) {
            chunk = world.loadChunkIfInMemory(index);
        }
        if (chunk != null) {
            pin(index, chunk);
            return chunk;
        }
        request(world, index);
        return null;
    }

    private void request(@Nonnull World world, long index) {
        if (abandoned.contains(index) || inflight.size() >= MAX_INFLIGHT_LOADS || !inflight.add(index)) {
            return;
        }
        world.getChunkAsync(index).whenComplete((chunk, error) -> world.execute(() -> completed(index, chunk, error)));
    }

    private void completed(long index, @Nullable WorldChunk chunk, @Nullable Throwable error) {
        if (!inflight.remove(index)) {
            return;
        }
        if (chunk == null) {
            failedChunks++;
            int attempts = failures.addTo(index, 1) + 1;
            if (attempts >= MAX_LOAD_ATTEMPTS) {
                failures.remove(index);
                abandoned.add(index);
                LOGGER.atWarning().withCause(error).log("Chunk %d, %d would not load after %d attempts; that ground stays unpainted",
                        ChunkUtil.xOfChunkIndex(index), ChunkUtil.zOfChunkIndex(index), MAX_LOAD_ATTEMPTS);
            }
            return;
        }
        loadedChunks++;
        failures.remove(index);
        pin(index, chunk);
    }

    private void pin(long index, @Nonnull WorldChunk chunk) {
        var previous = pinned.put(index, chunk);
        if (previous == null) {
            chunk.addKeepLoaded();
        } else if (previous != chunk) {
            previous.removeKeepLoaded();
            chunk.addKeepLoaded();
        }
        Iterator<Map.Entry<Long, WorldChunk>> eldest = pinned.entrySet().iterator();
        while (pinned.size() > MAX_PINNED_CHUNKS && eldest.hasNext()) {
            eldest.next().getValue().removeKeepLoaded();
            eldest.remove();
        }
    }

    // Replacement lookup

    @Nullable
    private static BlockType resolveVoidBlock(@Nonnull String id) {
        if (id.isBlank()) {
            return null;
        }
        var type = BlockType.getAssetMap().getAsset(id);
        if (type == null) {
            LOGGER.atWarning().log("VoidBlock '%s' is not a known block; the void paints nothing", id);
        }
        return type;
    }

    @Nullable
    private static Replacement blockReplacement(int blockId, int rotation, @Nonnull Map<String, String> mappings,
            @Nonnull BlockType fallback) {
        long key = ((long) blockId << 32) | (rotation & 0xffffffffL);
        var cached = BLOCK_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        var source = BlockType.getAssetMap().getAsset(blockId);
        var sourceId = source == null ? "" : source.getId();
        var target = mappings.get(sourceId);
        if (target == null || target.isBlank()) {
            target = mappings.get(categoryOf(source, sourceId, rotation));
        }
        if (target == null || target.isBlank()) {
            target = mappings.getOrDefault("Default", fallback.getId());
        }
        var replacement = lookup(target, fallback);
        if (replacement != null) {
            BLOCK_CACHE.put(key, replacement);
        }
        return replacement;
    }

    @Nullable
    private static FluidReplacement fluidReplacement(int fluidId, @Nonnull Map<String, String> mappings, @Nonnull BlockType fallback) {
        var cached = FLUID_CACHE.get(fluidId);
        if (cached != null) {
            return cached;
        }
        var fluid = Fluid.getAssetMap().getAsset(fluidId);
        var target = fluid == null ? null : mappings.get(fluid.getId());
        if (target == null || target.isBlank()) {
            target = mappings.get("Fluid");
        }
        if (target == null || target.isBlank()) {
            target = mappings.getOrDefault("Default", fallback.getId());
        }
        FluidReplacement replacement;
        int fluidIndex = Fluid.getAssetMap().getIndex(target);
        if (fluidIndex != AssetMapWithIndexes.NOT_FOUND && fluidIndex != Fluid.EMPTY_ID) {
            var asset = Fluid.getAssetMap().getAsset(fluidIndex);
            byte level = (byte) (asset == null ? 1 : Math.max(1, asset.getMaxFluidLevel()));
            replacement = new FluidReplacement(null, fluidIndex, level);
        } else {
            var block = lookup(target, fallback);
            replacement = block == null ? null : new FluidReplacement(block, Fluid.EMPTY_ID, (byte) 0);
        }
        if (replacement != null) {
            FLUID_CACHE.put(fluidId, replacement);
        }
        return replacement;
    }

    @Nullable
    private static Replacement lookup(@Nonnull String targetId, @Nonnull BlockType fallback) {
        var target = BlockType.getAssetMap().getAsset(targetId);
        int index = BlockType.getAssetMap().getIndex(targetId);
        if (target == null || index == AssetMapWithIndexes.NOT_FOUND) {
            LOGGER.atWarning().log("VoidBlockMap target '%s' is unknown; using '%s'", targetId, fallback.getId());
            target = fallback;
            index = BlockType.getAssetMap().getIndex(fallback.getId());
        }
        return index == AssetMapWithIndexes.NOT_FOUND ? null : new Replacement(target, index);
    }

    @Nonnull
    private static String categoryOf(@Nullable BlockType source, @Nonnull String sourceId, int rotation) {
        var id = sourceId.toLowerCase(Locale.ROOT);
        if (id.contains("rubble")) return "Rubble";
        if (id.contains("trapdoor")) return "Trapdoor";
        if (id.contains("pipe_corner")) return "PipeCorner";
        if (id.contains("roof_flat")) return "RoofFlat";
        if (id.contains("roof_shallow")) return "RoofShallow";
        if (id.contains("roof_steep")) return "RoofSteep";
        if (id.contains("half") || id.contains("slab")) return "Half";
        if (id.contains("stair")) return "Stairs";
        if (id.contains("fence")) return "Fence";
        if (id.contains("pipe")) return "Pipe";
        if (id.contains("roof")) return "Roof";
        if (id.contains("plant") || id.contains("flower") || id.contains("grass") || id.contains("crop")
                || id.contains("bush") || id.contains("leaves") || id.contains("vine") || id.contains("mushroom")) {
            return supportCategory(source, rotation, "Plant");
        }
        // Mushroom stems contain "branch" too, which is why plants are checked first.
        if (id.contains("branch_corner")) return "BranchCorner";
        if (id.contains("branch")) return "Branch";
        if (source != null && source.getLight() != null) {
            return supportCategory(source, rotation, "Light");
        }
        return "Solid";
    }

    @Nonnull
    private static String supportCategory(@Nullable BlockType source, int rotation, @Nonnull String base) {
        if (source == null) {
            return base;
        }
        Map<BlockFace, ?> support = source.getSupport(rotation);
        if (support == null || support.isEmpty()) {
            return base;
        }
        if (support.containsKey(BlockFace.NORTH) || support.containsKey(BlockFace.SOUTH)
                || support.containsKey(BlockFace.EAST) || support.containsKey(BlockFace.WEST)) {
            return base + "Wall";
        }
        if (support.containsKey(BlockFace.DOWN)) {
            return base + "Ground";
        }
        if (support.containsKey(BlockFace.UP)) {
            return base + "Ceiling";
        }
        return base;
    }

    // Geometry

    private interface ColumnSink {
        void accept(int x, int z);
    }

    /** Visits every block column between the two radii inside the zone's sweep. */
    private static void forEachColumnInBand(@Nonnull ZoneDefinition zone, double innerRadius, double outerRadius,
            @Nonnull ColumnSink sink) {
        if (!Double.isFinite(outerRadius) || outerRadius <= 0.0 || !Double.isFinite(innerRadius)) {
            return;
        }
        double inner = Math.max(0.0, Math.min(innerRadius, outerRadius));
        double cx = zone.centerX();
        double cz = zone.centerZ();
        int minZ = (int) Math.floor(cz - outerRadius - 0.5);
        int maxZ = (int) Math.ceil(cz + outerRadius + 0.5);
        for (int z = minZ; z <= maxZ; z++) {
            double dz = z + 0.5 - cz;
            double outerHalf = halfSpan(outerRadius, dz);
            if (Double.isNaN(outerHalf)) {
                continue;
            }
            int outerLo = (int) Math.ceil(cx - outerHalf - 0.5);
            int outerHi = (int) Math.floor(cx + outerHalf - 0.5);
            double innerHalf = inner > 0.0 ? halfSpan(inner, dz) : Double.NaN;
            if (Double.isNaN(innerHalf)) {
                sweep(zone, z, outerLo, outerHi, sink);
                continue;
            }
            int innerLo = (int) Math.ceil(cx - innerHalf - 0.5);
            int innerHi = (int) Math.floor(cx + innerHalf - 0.5);
            sweep(zone, z, outerLo, innerLo - 1, sink);
            sweep(zone, z, innerHi + 1, outerHi, sink);
        }
    }

    private static void sweep(@Nonnull ZoneDefinition zone, int z, int lo, int hi, @Nonnull ColumnSink sink) {
        double dz = z + 0.5 - zone.centerZ();
        for (int x = lo; x <= hi; x++) {
            if (zone.isInSweep(x + 0.5 - zone.centerX(), dz)) {
                sink.accept(x, z);
            }
        }
    }

    private static double halfSpan(double radius, double dz) {
        double squared = radius * radius - dz * dz;
        return squared <= 0.0 ? Double.NaN : Math.sqrt(squared);
    }

    private static long packColumn(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static int unpackX(long column) {
        return (int) (column >> 32);
    }

    private static int unpackZ(long column) {
        return (int) column;
    }

    /** Reads and writes fluids, keeping the last chunk section in hand. */
    private static final class FluidCursor {
        private final World world;
        private int sectionX = Integer.MIN_VALUE;
        private int sectionY = Integer.MIN_VALUE;
        private int sectionZ = Integer.MIN_VALUE;
        @Nullable private FluidSection section;

        FluidCursor(@Nonnull World world) {
            this.world = world;
        }

        @Nullable
        private FluidSection sectionAt(int x, int y, int z) {
            int chunkX = ChunkUtil.chunkCoordinate(x);
            int chunkY = ChunkUtil.chunkCoordinate(y);
            int chunkZ = ChunkUtil.chunkCoordinate(z);
            if (chunkX != sectionX || chunkY != sectionY || chunkZ != sectionZ) {
                sectionX = chunkX;
                sectionY = chunkY;
                sectionZ = chunkZ;
                Ref<ChunkStore> ref = world.getChunkStore().getChunkSectionReference(chunkX, chunkY, chunkZ);
                section = ref == null ? null : ref.getStore().getComponent(ref, FluidSection.getComponentType());
            }
            return section;
        }

        int idAt(int x, int y, int z) {
            if (y < ChunkUtil.MIN_Y || y >= ChunkUtil.HEIGHT) {
                return Fluid.EMPTY_ID;
            }
            var fluids = sectionAt(x, y, z);
            return fluids == null ? Fluid.EMPTY_ID : fluids.getFluidId(x, y, z);
        }

        boolean isFluid(int x, int y, int z) {
            return idAt(x, y, z) != Fluid.EMPTY_ID;
        }

        boolean clear(int x, int y, int z) {
            return set(x, y, z, Fluid.EMPTY_ID, (byte) 0);
        }

        boolean set(int x, int y, int z, int fluidId, byte level) {
            if (y < ChunkUtil.MIN_Y || y >= ChunkUtil.HEIGHT) {
                return false;
            }
            var fluids = sectionAt(x, y, z);
            return fluids != null && fluids.setFluid(x, y, z, fluidId, level);
        }
    }
}
