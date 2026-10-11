package com.gaiagauntlet.gauntlet.games.EliminationZone.zones.utils;

import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.LootFountainRule;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfig;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.ZoneDefinition;
import com.gaiagauntlet.gauntlet.utils.BlockUtils;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.SetBlockSettings;
import com.hypixel.hytale.server.core.universe.world.World;
import org.joml.Vector3i;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Thins the loot fountains the map authored so each fresh arena keeps a random
 * subset per zone and
 * tier, as the zone's rules say. Runs once per arena: the chunks under every
 * zone are loaded first,
 * then the scan and the removals happen on the arena thread.
 */
public final class LootFountains {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int MIN_TIER = 1;
    private static final int MAX_TIER = 5;
    private static final String BLOCK_PREFIX = "GG_LootFountain_T";
    private static final int REMOVE_SETTINGS = SetBlockSettings.NO_SEND_PARTICLES | SetBlockSettings.NO_SEND_AUDIO
            | SetBlockSettings.NO_DROP_ITEMS;

    private LootFountains() {
    }

    /**
     * Loads every chunk the zones cover, then thins the fountains once they are all
     * in. Completes once every zone is thinned.
     */
    @Nonnull
    public static CompletableFuture<Void> randomize(@Nonnull World arena, String sessionId) {
        var indexes = new java.util.LinkedHashSet<Long>();
        for (var zone : EZGameConfig.get(arena, sessionId).getZones()) {
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
        if (indexes.isEmpty())
            return CompletableFuture.completedFuture(null);

        var loads = new ArrayList<CompletableFuture<?>>();
        var chunkStore = arena.getChunkStore();
        for (long index : indexes) {
            loads.add(chunkStore.getChunkReferenceAsync(index).exceptionally(e -> null));
        }
        LOGGER.atInfo().log("Loading %d chunks of %s before thinning loot fountains", loads.size(), arena.getName());
        return CompletableFuture.allOf(loads.toArray(CompletableFuture[]::new))
                .thenCompose(ignored -> thin(arena, sessionId));
    }

    @Nonnull
    private static CompletableFuture<Void> thin(@Nonnull World arena, String game) {
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
            return CompletableFuture.completedFuture(null);
        }
        // one zone per task so the arena keeps ticking between zones
        var zones = new ArrayList<CompletableFuture<Void>>();
        for (var zone : EZGameConfig.get(arena, game).getZones()) {
            zones.add(CompletableFuture.runAsync(() -> thinZone(arena, zone, tierByBlockId), arena)
                    .exceptionally(error -> {
                        LOGGER.atWarning().withCause(error).log("Thinning the loot fountains of zone '%s' failed",
                                zone.id());
                        return null;
                    }));
        }
        return CompletableFuture.allOf(zones.toArray(CompletableFuture[]::new));
    }

    private static void thinZone(@Nonnull World arena, @Nonnull ZoneDefinition zone,
            @Nonnull Map<Integer, Integer> tierByBlockId) {
        var rules = new HashMap<Integer, LootFountainRule>();
        for (var rule : zone.lootFountains()) {
            if (rule.enabled() && rule.tier() >= MIN_TIER && rule.tier() <= MAX_TIER) {
                rules.putIfAbsent(rule.tier(), rule);
            }
        }
        if (rules.isEmpty()) {
            return;
        }
        var found = discover(arena, zone, rules, tierByBlockId);
        for (var entry : rules.entrySet()) {
            int tier = entry.getKey();
            var rule = entry.getValue();
            var candidates = found.getOrDefault(tier, new ArrayList<>());
            int min = Math.max(0, rule.min());
            int max = Math.max(min, rule.max());
            int keep = Math.min(candidates.size(),
                    min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1));
            Collections.shuffle(candidates, ThreadLocalRandom.current());
            int removed = 0;
            for (var position : candidates.subList(keep, candidates.size())) {
                if (BlockUtils.setBlock(arena, position.x(), position.y(), position.z(), BlockType.EMPTY_KEY, REMOVE_SETTINGS)) {
                    removed++;
                }
            }
            LOGGER.atInfo().log("Zone '%s' tier %d: kept %d of %d loot fountains", zone.id(), tier,
                    candidates.size() - removed, candidates.size());
        }
    }

    /**
     * Every fountain block of a ruled tier inside the zone, except the protected
     * positions.
     */
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
        var wanted = new IntArrayList();
        tierByBlockId.forEach((blockId, tier) -> {
            if (rules.containsKey(tier)) {
                wanted.add((int) blockId);
            }
        });

        int minY = Math.max(ChunkUtil.MIN_Y, (int) Math.floor(zone.minY()));
        int maxY = (int) Math.floor(zone.maxY());
        int minChunkX = ChunkUtil.chunkCoordinate((int) Math.floor(zone.centerX() - zone.outerRadius()));
        int maxChunkX = ChunkUtil.chunkCoordinate((int) Math.floor(zone.centerX() + zone.outerRadius()));
        int minChunkZ = ChunkUtil.chunkCoordinate((int) Math.floor(zone.centerZ() - zone.outerRadius()));
        int maxChunkZ = ChunkUtil.chunkCoordinate((int) Math.floor(zone.centerZ() + zone.outerRadius()));
        int minSection = ChunkUtil.indexSection(minY);
        int maxSection = ChunkUtil.indexSection(maxY);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                int worldMinX = ChunkUtil.minBlock(chunkX);
                int worldMinZ = ChunkUtil.minBlock(chunkZ);
                for (int sectionIndex = minSection; sectionIndex <= maxSection; sectionIndex++) {
                    int sectionMinY = ChunkUtil.minBlock(sectionIndex);
                    var section = BlockUtils.getBlockSection(arena, worldMinX, sectionMinY, worldMinZ);
                    if (section == null || !section.containsAny(wanted)) {
                        continue;
                    }
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
