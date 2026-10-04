package com.gaiagauntlet.gauntlet.games.EliminationZone.zones.utils;

import com.gaiagauntlet.gg.zone.ZoneDefinition;
import com.gaiagauntlet.gg.zone.Zones;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Thins the loot fountains the map authored so each fresh arena keeps a random subset per zone and
 * tier, as the zone's rules say. Runs once per arena: the chunks under every zone are loaded first,
 * then the scan and the removals happen on the arena thread.
 */
public final class LootFountains {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int MIN_TIER = 1;
    private static final int MAX_TIER = 5;
    private static final String BLOCK_PREFIX = "GG_LootFountain_T";

    private LootFountains() {
    }

    /** Loads every chunk the zones cover, then thins the fountains once they are all in. */
    public static void randomize(@Nonnull World arena) {
        var indexes = new java.util.LinkedHashSet<Long>();
        for (var zone : Zones.get().zones()) {
            if (zone.lootFountains().isEmpty()) {
                continue;
            }
            int minChunkX = ChunkUtil.chunkCoordinate((int) Math.floor(zone.centerX() - zone.outerRadius()));
            int maxChunkX = ChunkUtil.chunkCoordinate((int) Math.floor(zone.centerX() + zone.outerRadius()));
            int minChunkZ = ChunkUtil.chunkCoordinate((int) Math.floor(zone.centerZ() - zone.outerRadius()));
            int maxChunkZ = ChunkUtil.chunkCoordinate((int) Math.floor(zone.centerZ() + zone.outerRadius()));
            for (int x = minChunkX; x <= maxChunkX; x++) {
                for (int z = minChunkZ; z <= maxChunkZ; z++) {
                    indexes.add(ChunkUtil.indexChunk(x, z));
                }
            }
        }
        if (indexes.isEmpty()) {
            return;
        }
        var loads = new ArrayList<java.util.concurrent.CompletableFuture<?>>();
        for (long index : indexes) {
            loads.add(arena.getChunkAsync(index).exceptionally(e -> null));
        }
        LOGGER.atInfo().log("Loading %d chunks of %s before thinning loot fountains", loads.size(), arena.getName());
        java.util.concurrent.CompletableFuture.allOf(loads.toArray(java.util.concurrent.CompletableFuture[]::new))
                .whenComplete((ignored, error) -> arena.execute(() -> thin(arena)));
    }

    private static void thin(@Nonnull World arena) {
        var tierByBlockId = new HashMap<Integer, Integer>();
        for (int tier = MIN_TIER; tier <= MAX_TIER; tier++) {
            var id = BLOCK_PREFIX + tier;
            if (BlockType.getAssetMap().getAsset(id) == null) {
                LOGGER.atWarning().log("Loot fountain block %s is missing", id);
            } else {
                tierByBlockId.put(BlockType.getAssetMap().getIndex(id), tier);
            }
        }
        if (tierByBlockId.isEmpty()) {
            return;
        }
        int removedTotal = 0;
        for (var zone : Zones.get().zones()) {
            var rules = new HashMap<Integer, LootFountainRule>();
            for (var rule : zone.lootFountains()) {
                if (rule.enabled() && rule.tier() >= MIN_TIER && rule.tier() <= MAX_TIER) {
                    rules.putIfAbsent(rule.tier(), rule);
                }
            }
            if (rules.isEmpty()) {
                continue;
            }
            var found = discover(arena, zone, rules, tierByBlockId);
            for (var entry : rules.entrySet()) {
                int tier = entry.getKey();
                var rule = entry.getValue();
                var candidates = found.getOrDefault(tier, List.of());
                int min = Math.max(0, rule.min());
                int max = Math.max(min, rule.max());
                int keep = Math.min(candidates.size(), min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1));
                Collections.shuffle(candidates, ThreadLocalRandom.current());
                int removed = 0;
                for (var position : candidates.subList(keep, candidates.size())) {
                    var chunk = arena.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(position.x(), position.z()));
                    if (chunk != null) {
                        chunk.setBlock(position.x(), position.y(), position.z(), BlockType.EMPTY_ID, BlockType.EMPTY, 0,
                                FillerBlockUtil.NO_FILLER, 0);
                        removed++;
                    }
                }
                removedTotal += removed;
                LOGGER.atInfo().log("Zone '%s' tier %d: kept %d of %d loot fountains", zone.id(), tier, candidates.size() - removed, candidates.size());
            }
        }
        LOGGER.atInfo().log("Loot fountain randomization removed %d blocks from %s", removedTotal, arena.getName());
    }

    /** Every fountain block of a ruled tier inside the zone, except the protected positions. */
    @Nonnull
    private static Map<Integer, List<Vector3i>> discover(@Nonnull World arena, @Nonnull ZoneDefinition zone,
            @Nonnull Map<Integer, LootFountainRule> rules, @Nonnull Map<Integer, Integer> tierByBlockId) {
        var result = new HashMap<Integer, List<Vector3i>>();
        var protectedByTier = new HashMap<Integer, Set<Vector3i>>();
        rules.forEach((tier, rule) -> {
            var positions = new HashSet<Vector3i>();
            for (var p : rule.alwaysSpawn()) {
                if (p.length == 3) {
                    positions.add(new Vector3i(p[0], p[1], p[2]));
                }
            }
            protectedByTier.put(tier, positions);
        });
        IntList wanted = new IntArrayList();
        tierByBlockId.forEach((blockId, tier) -> {
            if (rules.containsKey(tier)) {
                wanted.add((int) blockId);
            }
        });
        int minY = Math.max(ChunkUtil.MIN_Y, (int) Math.floor(zone.minY()));
        int maxY = Math.min(ChunkUtil.HEIGHT - 1, (int) Math.floor(zone.maxY()));
        int minChunkX = ChunkUtil.chunkCoordinate((int) Math.floor(zone.centerX() - zone.outerRadius()));
        int maxChunkX = ChunkUtil.chunkCoordinate((int) Math.floor(zone.centerX() + zone.outerRadius()));
        int minChunkZ = ChunkUtil.chunkCoordinate((int) Math.floor(zone.centerZ() - zone.outerRadius()));
        int maxChunkZ = ChunkUtil.chunkCoordinate((int) Math.floor(zone.centerZ() + zone.outerRadius()));
        int minSection = ChunkUtil.indexSection(minY);
        int maxSection = ChunkUtil.indexSection(maxY);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                var chunk = arena.getChunkIfLoaded(ChunkUtil.indexChunk(chunkX, chunkZ));
                if (chunk == null) {
                    continue;
                }
                int worldMinX = ChunkUtil.minBlock(chunkX);
                int worldMinZ = ChunkUtil.minBlock(chunkZ);
                for (int sectionIndex = minSection; sectionIndex <= maxSection; sectionIndex++) {
                    var section = chunk.getBlockChunk().getSectionAtBlockY(ChunkUtil.minBlock(sectionIndex));
                    if (section == null || !section.containsAny(wanted)) {
                        continue;
                    }
                    int sectionMinY = ChunkUtil.minBlock(sectionIndex);
                    section.find(wanted, (blockIndex, blockId) -> {
                        int x = worldMinX + ChunkUtil.xFromIndex(blockIndex);
                        int y = sectionMinY + ChunkUtil.yFromIndex(blockIndex);
                        int z = worldMinZ + ChunkUtil.zFromIndex(blockIndex);
                        var tier = tierByBlockId.get(blockId);
                        if (tier == null || y < minY || y > maxY || !contains(zone, x + 0.5, z + 0.5)) {
                            return;
                        }
                        var position = new Vector3i(x, y, z);
                        if (!protectedByTier.get(tier).contains(position)) {
                            result.computeIfAbsent(tier, ignored -> new ArrayList<>()).add(position);
                        }
                    });
                }
            }
        }
        return result;
    }

    private static boolean contains(@Nonnull ZoneDefinition zone, double x, double z) {
        double dx = x - zone.centerX();
        double dz = z - zone.centerZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        return distance >= zone.innerRadius() && distance <= zone.outerRadius() && zone.isInSweep(dx, dz);
    }
}
