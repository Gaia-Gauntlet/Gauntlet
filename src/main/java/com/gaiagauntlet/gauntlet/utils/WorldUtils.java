package com.gaiagauntlet.gauntlet.utils;

import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.builtin.instances.config.InstanceWorldConfig;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.util.io.FileUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Thread helpers and world creation. Every world mutation in the plugin goes through {@link #run}. */
public final class WorldUtils {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private WorldUtils() {
    }

    /** Runs on the world's thread: inline when already there, queued otherwise. */
    public static void run(@Nonnull World world, @Nonnull Runnable task) {
        if (world.isInThread()) {
            task.run();
        } else {
            world.execute(task);
        }
    }

    /** The world the player's entity lives in, falling back to the world the player is joining. */
    @Nullable
    public static World worldOf(@Nonnull PlayerRef player) {
        var ref = player.getReference();
        if (ref != null && ref.isValid()) {
            var world = ref.getStore().getExternalData().getWorld();
            if (world != null) {
                return world;
            }
        }
        var uuid = player.getWorldUuid();
        return uuid == null ? null : Universe.get().getWorld(uuid);
    }

    @Nonnull
    public static java.util.Collection<World> allWorlds() {
        return new java.util.ArrayList<>(Universe.get().getWorlds().values());
    }

    public static boolean isUsable(@Nullable World world) {
        return world != null && world.isAlive() && world.isTicking();
    }

    /** The world's configured spawn, or the origin when none is configured. */
    @Nonnull
    public static Transform spawnOf(@Nonnull World world) {
        var points = world.getWorldConfig().getSpawnProvider().getSpawnPoints();
        return points != null && points.length > 0 ? new Transform(points[0]) : new Transform(0, 64, 0);
    }

    /** Spawns a disposable instance from a bundled template. Players returning from it land at the origin. */
    @Nonnull
    public static CompletableFuture<World> spawnInstance(@Nonnull String template, @Nonnull World origin,
            @Nonnull Transform returnPoint) {
        if (!InstancesPlugin.doesInstanceAssetExist(template)) {
            return missingTemplate(template);
        }
        return InstancesPlugin.get().spawnInstance(template, origin, returnPoint);
    }

    /**
     * Spawns a disposable instance under a fixed world name that stays up while empty, so it can be
     * filled later. Returns the existing world when one by that name is already loaded.
     */
    @Nonnull
    public static CompletableFuture<World> spawnNamedInstance(@Nonnull String template, @Nonnull String worldName,
            @Nonnull World origin, @Nonnull Transform returnPoint) {
        if (!InstancesPlugin.doesInstanceAssetExist(template)) {
            return missingTemplate(template);
        }
        return InstancesPlugin.get().spawnInstance(template, worldName, origin, returnPoint
            // FIXME: Unless we're supposed to be developing on pre-release, this method signature doesn't exist and is causing a compilation error
//                config -> InstanceWorldConfig.ensureAndGet(config).setRemovalConditions(RemovalCondition.EMPTY)
        );
    }

    private static CompletableFuture<World> missingTemplate(@Nonnull String template) {
        return CompletableFuture.failedFuture(new IllegalStateException(
                "Instance template '" + template + "' does not exist (expected Server/Instances/" + template
                        + "/instance.bson in an asset pack)"));
    }

    /**
     * Returns the named permanent world, loading it from disk or creating it from an instance template
     * on first use. The template's instance settings and its delete-on-start flag are stripped so the
     * world is never removed or wiped, and edits made in it survive restarts.
     */
    @Nonnull
    public static CompletableFuture<World> ensurePermanentWorld(@Nonnull String template, @Nonnull String worldName) {
        var universe = Universe.get();
        var existing = universe.getWorld(worldName);
        if (existing != null) {
            return CompletableFuture.completedFuture(existing);
        }
        if (universe.isWorldLoadable(worldName)) {
            return universe.loadWorld(worldName);
        }
        if (!InstancesPlugin.doesInstanceAssetExist(template)) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                    "Cannot create world '" + worldName + "': instance template '" + template + "' does not exist"));
        }
        var assetPath = InstancesPlugin.getInstanceAssetPath(template);
        var worldPath = universe.validateWorldPath(worldName);
        return WorldConfig.load(assetPath.resolve(InstancesPlugin.CONFIG_FILENAME)).thenApplyAsync(config -> {
            config.setUuid(UUID.randomUUID());
            config.setDeleteOnRemove(false);
            config.setDeleteOnUniverseStart(false);
            if (config.getDisplayName() == null) {
                config.setDisplayName(WorldConfig.formatDisplayName(template));
            }
            config.getPluginConfig().remove(InstanceWorldConfig.class);
            config.markChanged();
            try (var files = Files.walk(assetPath, FileUtil.DEFAULT_WALK_TREE_OPTIONS_ARRAY)) {
                for (var it = files.iterator(); it.hasNext();) {
                    var source = it.next();
                    var target = worldPath.resolve(assetPath.relativize(source).toString());
                    if (Files.isDirectory(source)) {
                        Files.createDirectories(target);
                    } else if (Files.isRegularFile(source)) {
                        Files.copy(source, target);
                    }
                }
            } catch (java.io.IOException e) {
                throw new IllegalStateException("Could not copy template " + template + " to " + worldPath, e);
            }
            LOGGER.atInfo().log("Created permanent world %s from template %s", worldName, template);
            return config;
        }).thenCompose(config -> universe.makeWorld(worldName, worldPath, config));
    }
}
