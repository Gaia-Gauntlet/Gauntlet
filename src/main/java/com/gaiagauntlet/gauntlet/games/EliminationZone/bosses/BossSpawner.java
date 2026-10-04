package com.gaiagauntlet.gauntlet.games.EliminationZone.bosses;

import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.components.BossMarkerComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.components.BossScalingComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.components.BossesComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.utils.BossUtils;
import com.hypixel.hytale.builtin.triggervolumes.TriggerVolumesPlugin;
import com.hypixel.hytale.builtin.triggervolumes.manager.VolumeEntry;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Puts bosses into the arena at spawn points the map authored as trigger volumes tagged
 * {@code gg.boss.spawn.<role>}. A boss spawns only in a zone that is neither closing nor sealed.
 * Every method runs on the arena thread.
 */
public final class BossSpawner {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String TAG_PREFIX = "gg.boss.spawn.";
    private static final String SOUND_SPAWN = "SFX_Rex_Alerted";
    private static final String SOUND_DEFEAT = "SFX_Rex_Death";

    /** A spawn point read from a volume. */
    public record SpawnPoint(@Nonnull String bossId, @Nonnull Vector3d position, @Nonnull String zoneId) {
    }

    private BossSpawner() {
    }

    /** Spawns the named boss at one of its points in an open zone. Throws with an admin-facing reason when it cannot. */
    public static void spawn(@Nonnull String game, @Nonnull World arena, @Nonnull String bossId) {
        var boss = BossUtils.getBoss(bossId);
        if (boss == null) {
            throw new IllegalArgumentException("No GaiaBoss role '" + bossId + "'. Bosses: " + String.join(", ", BossUtils.getBossNames()));
        }
        if (!boss.isEnabled()) {
            throw new IllegalStateException("Boss " + boss.roleId() + " is disabled in its role");
        }

        BossesComponent bosses = BossesComponent.TYPE.of(game);

        if (bosses.isActive(boss.roleId()) || bosses.getPending().contains(boss.roleId())) {
            throw new IllegalStateException(boss.roleId() + " is already in the arena");
        }
        int limit = GlobalStore.get().settingsOf(game).get(Settings.BOSS_MAX_ACTIVE);
        if (bosses.count() >= limit) {
            throw new IllegalStateException("Boss limit reached (" + limit + ")");
        }
        var points = new ArrayList<SpawnPoint>();
        for (var point : spawnPoints(arena)) {
            if (isUsable(game, boss, point)) {
                points.add(point);
            }
        }
        if (points.isEmpty()) {
            throw new IllegalStateException("No open spawn point for " + boss.roleId()
                    + " (tag a volume " + TAG_PREFIX + boss.roleId() + " in a zone that is not closing)");
        }
        var point = points.get(ThreadLocalRandom.current().nextInt(points.size()));

        // TODO:
//        var chunk = arena.getChunkIfLoaded(chunkIndex);
//        if (chunk != null) {
//            place(game, arena, boss, point, chunk);
//            return;
//        }
        bosses.getPending().add(boss.roleId());
        GaiaLog.atInfo().log(String.format("Loading the spawn chunk for %s", boss.roleId())).withGameId(game);

//        arena.getChunkAsync(chunkIndex).whenComplete((loaded, error) -> arena.execute(() -> {
//            bosses.getPending().remove(boss.roleId());
//            if (loaded == null || error != null) {
//                GaiaLog.atWarning().log("The spawn chunk for " + boss.roleId() + " would not load")
//                    .withGameId(game);
//                return;
//            }
//            if (!isZoneOpen(game, point.zoneId())) {
//                GaiaLog.atWarning().log(boss.roleId() + " not spawned: " + point.zoneId() + " started closing")
//                    .withGameId(game);
//                return;
//            }
//            place(game, arena, boss, point, loaded);
//        }));
    }

    /**
     * Spawns a random enabled boss that has an open spawn point and is not already out.
     * Bosses take turns, so none comes up again until every other one has.
     */
    public static void spawnRandom(@Nonnull String game, @Nonnull World arena) {
        BossesComponent bosses = BossesComponent.TYPE.of(game);
        var points = spawnPoints(arena);
        var candidates = new ArrayList<String>();
        for (var boss : BossUtils.getBosses().values()) {
            if (!boss.isEnabled() || bosses.isActive(boss.roleId()) || bosses.getPending().contains(boss.roleId())) {
                continue;
            }
            for (var point : points) {
                if (isUsable(game, boss, point)) {
                    candidates.add(boss.roleId());
                    break;
                }
            }
        }
        if (candidates.isEmpty()) {
            throw new IllegalStateException("No boss can spawn right now: every candidate is out, disabled, or in a closing zone");
        }
        spawn(game, arena, bosses.drawFrom(candidates));
    }

    private static void place(@Nonnull String game, @Nonnull World arena, @Nonnull BossScalingComponent boss, @Nonnull SpawnPoint point,
                              @Nonnull WorldChunk chunk) {
        var npc = NPCPlugin.get();
        int roleIndex = npc.getIndex(boss.roleId());
        if (roleIndex < 0) {
            GaiaLog.atWarning().log("NPC role " + boss.roleId() + " cannot be spawned").withGameId(game);
            return;
        }
        chunk.addKeepLoaded();
        var store = arena.getEntityStore().getStore();
        var spawned = npc.spawnEntity(store, roleIndex, point.position(), new Rotation3f(0f, 0f, 0f), null,
                (entity, ref, s) -> {
                    var model = s.getComponent(ref, ModelComponent.getComponentType());
                    if (model != null && boss.physicalScale() != 1f) {
                        var scaled = Model.createScaledModel(ModelAsset.getAssetMap().getAsset(model.getModel().getModelAssetId()), boss.physicalScale());
                        entity.setInitialModelScale(boss.physicalScale());
                        s.removeComponent(ref, ModelComponent.getComponentType());
                        s.addComponent(ref, ModelComponent.getComponentType(), new ModelComponent(scaled));
                    }
                    s.addComponent(ref, BossMarkerComponent.getComponentType(), new BossMarkerComponent(game, boss.roleId()));
                });
        if (spawned == null) {
            chunk.removeKeepLoaded();
            GaiaLog.atWarning().log("Spawning " + boss.roleId() + " failed").withGameId(game);
            return;
        }
        Ref<EntityStore> ref = spawned.first();

        BossesComponent.TYPE.of(game).add(new BossesComponent.Active(boss.roleId(), ref, point.zoneId()));

        LOGGER.atInfo().log("[%s] %s spawned in %s", game, boss.roleId(), point.zoneId());
        GaiaLog.atWarning().log("Boss " + boss.roleId() + " spawned in " + point.zoneId()).withGameId(game);
        Announce.title(arena, Message.raw("BOSS APPEARED"),
                Message.raw(boss.displayText() + " has appeared in " + point.zoneId() + "!"), SOUND_SPAWN);
        Events.dispatch(new BossEvents.Spawned(game, arena, boss, point.zoneId()));
    }

    /** Called by the death system when a marked boss dies. */
    static void onDefeated(@Nonnull String game, @Nonnull World arena, @Nonnull String bossId) {
        BossesComponent.TYPE.of(game).remove(bossId);
        var boss = BossUtils.getBoss(bossId);
        var name = boss == null ? bossId.replace('_', ' ') : boss.displayText();
        LOGGER.atInfo().log("[%s] %s defeated", game, bossId);
        GaiaLog.atWarning().log("Boss " + bossId + " defeated").withGameId(game);
        Announce.title(arena, Message.raw("BOSS DEFEATED"), Message.raw(name + " has been defeated!"), SOUND_DEFEAT);
        Events.dispatch(new BossEvents.Defeated(game, arena, bossId));
    }

    /** True when the point is one of this boss's, in a zone it may use that is open. */
    private static boolean isUsable(@Nonnull String game, @Nonnull BossScalingComponent boss, @Nonnull SpawnPoint point) {
        return BossScalingComponent.normalize(point.bossId()).equals(BossScalingComponent.normalize(boss.roleId()))
                && boss.allowsZone(point.zoneId()) && isZoneOpen(game, point.zoneId());
    }

    /** Every boss spawn volume in the world with the zone it sits in. */
    @Nonnull
    public static List<SpawnPoint> spawnPoints(@Nonnull World arena) {
        var manager = arena.getEntityStore().getStore().getResource(TriggerVolumesPlugin.get().getManagerResourceType());
        var points = new ArrayList<SpawnPoint>();
        for (VolumeEntry volume : manager.getVolumes()) {
            for (var tag : volume.getRawTags().keySet()) {
                if (!tag.startsWith(TAG_PREFIX) || tag.length() <= TAG_PREFIX.length()) {
                    continue;
                }
                var min = new Vector3d();
                var max = new Vector3d();
                volume.getShape().getWorldAABB(volume.getPosition(), min, max);
                var center = new Vector3d((min.x() + max.x()) / 2, (min.y() + max.y()) / 2, (min.z() + max.z()) / 2);
                points.add(new SpawnPoint(tag.substring(TAG_PREFIX.length()), center, zoneAt(center)));
            }
        }
        return points;
    }

    @Nonnull
    private static String zoneAt(@Nonnull Vector3d position) {
        // TODO
//        for (ZoneDefinition zone : Zones.get().zones()) {
//            double dx = position.x() - zone.centerX();
//            double dz = position.z() - zone.centerZ();
//            double distance = Math.sqrt(dx * dx + dz * dz);
//            if (distance >= zone.innerRadius() && distance <= zone.outerRadius() && zone.isInSweep(dx, dz)) {
//                return zone.id();
//            }
//        }
        return "";
    }

    /** True unless the zone feature says the zone is closing or sealed. */
    private static boolean isZoneOpen(@Nonnull String game, @Nonnull String zoneId) {
        return true;
        // TODO
//        if (zoneId.isEmpty() || !game.has(ZoneComponent.TYPE)) {
//            return true;
//        }
//        var zones = ZoneComponent.TYPE.of(game);
//        if (!zones.isActive()) {
//            return true;
//        }
//        var active = zones.activeZone();
//        if (active != null && active.id().equals(zoneId)) {
//            return false;
//        }
//        return zones.closedZones().stream().noneMatch(z -> z.id().equals(zoneId));
    }
}
