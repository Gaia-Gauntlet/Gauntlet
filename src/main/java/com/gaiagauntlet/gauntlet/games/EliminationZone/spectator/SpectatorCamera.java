package com.gaiagauntlet.gauntlet.games.EliminationZone.spectator;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spectator.components.SpectatorComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.entity.EliminatedComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.utils.TeamUtils;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.player.CameraManager;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.gamemode.GameModeTypes;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** The third-person follow camera for spectators, and choosing whom to follow. Run on the arena thread. */
public final class SpectatorCamera {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    static final float DISTANCE = 4f;
    static final float MIN_PIVOT_HEIGHT = 1f;
    static final float MIN_EXIT_STANDOFF = 0.5f;
    static final float WALL_MARGIN = 1f;

    /**
     * Each spectator's current target. Kept here rather than on the component, whose stored copy is
     * not the instance a system is handed.
     */
    private static final Map<Ref<EntityStore>, Ref<EntityStore>> FOLLOWING = new ConcurrentHashMap<>();

    private SpectatorCamera() {
    }

    /** Points the spectator's camera at the target and teleports them to it so chunks and voice follow. */
    public static void follow(@Nonnull Ref<EntityStore> spectator, @Nonnull Ref<EntityStore> target,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        var player = accessor.getComponent(spectator, PlayerRef.getComponentType());
        var targetId = accessor.getComponent(target, NetworkId.getComponentType());
        if (player == null || targetId == null) {
            return;
        }
        FOLLOWING.put(spectator, target);
        var targetTransform = accessor.getComponent(target, TransformComponent.getComponentType());
        if (targetTransform != null) {
            accessor.putComponent(spectator, Teleport.getComponentType(),
                    Teleport.createForPlayer(targetTransform.getPosition(), targetTransform.getRotation()));
        }
        var settings = new ServerCameraSettings();
        settings.attachedToType = AttachedToType.EntityId;
        settings.attachedToEntityId = targetId.getId();
        settings.followAttachedEntity = true;
        settings.eyeOffset = true;
        settings.isFirstPerson = false;
        var eyeHeight = ModelComponent.getEyeHeight(target, accessor);
        if (eyeHeight < MIN_PIVOT_HEIGHT) {
            settings.positionOffset = new Position(0, MIN_PIVOT_HEIGHT - eyeHeight, 0);
        }
        settings.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffsetRaycast;
        settings.distance = DISTANCE;
        settings.rotationType = RotationType.Custom;
        settings.applyLookType = ApplyLookType.Rotation;
        if (targetTransform != null) {
            var rotation = targetTransform.getRotation();
            settings.rotation = new Direction(rotation.yaw(), rotation.pitch(), rotation.roll());
        }
        player.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.Custom, true, settings));
    }

    /**
     * Follows the first player {@link #nextTarget} allows, so a new spectator starts on a living
     * teammate when there is one. Returns whom it followed, or null when nobody is left to watch.
     */
    @Nullable
    public static Ref<EntityStore> followFirst(@Nonnull String game, @Nonnull Ref<EntityStore> spectator,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        var target = nextTarget(game, spectator, accessor, null, true);
        if (target != null) {
            follow(spectator, target, accessor);
            return target;
        }
        var self = accessor.getComponent(spectator, PlayerRef.getComponentType());
        LOGGER.atInfo().log("[%s] Nobody alive to spectate for %s", game, self == null ? "?" : self.getUsername());
        return null;
    }

    /**
     * The next or previous player to watch. While any teammate is alive only teammates can be watched;
     * once the whole team is out, anyone alive can. Players who are themselves spectating are skipped.
     */
    @Nullable
    public static Ref<EntityStore> nextTarget(@Nonnull String session, @Nonnull Ref<EntityStore> spectator,
            @Nonnull ComponentAccessor<EntityStore> accessor, @Nullable Ref<EntityStore> current, boolean forward) {
        var self = accessor.getComponent(spectator, PlayerRef.getComponentType());
        if (self == null) return null;


        var players = GauntletUtils.playersFor(session);
        var world = accessor.getExternalData().getWorld();
        var pool = new ArrayList<Ref<EntityStore>>();
        var eliminated = accessor.getComponent(spectator, EliminatedComponent.getComponentType());
        var spectatorTeam = TeamUtils.withTeamFor(world, session, self.getUuid());
        for (var pass = 0; pass < 2 && pool.isEmpty(); pass++) {
            for (var candidate : players) {
                var candidateTeam = TeamUtils.withTeamFor(world, session, candidate.getUuid());
                if (eliminated != null || candidate.getUuid().equals(self.getUuid())) continue;

                var sameTeam = candidateTeam == null || (spectatorTeam != null && spectatorTeam.getId().equals(candidateTeam.getId()));
                if (pass == 0 && !sameTeam) continue;

                var player = findInWorld(world, candidate.getUuid());
                var ref = player == null ? null : player.getReference();
                if (ref != null && ref.isValid() && ref.getStore() == spectator.getStore()
                        && accessor.getComponent(ref, SpectatorComponent.getComponentType()) == null) {
                    pool.add(ref);
                }
            }
        }
        if (pool.isEmpty()) return null;

        var index = current == null ? -1 : pool.indexOf(current);
        var next = index < 0 ? 0 : Math.floorMod(index + (forward ? 1 : -1), pool.size());
        return pool.get(next);
    }

    /** Whom the spectator's camera is following, or null when it follows nobody. */
    @Nullable
    public static Ref<EntityStore> following(@Nonnull Ref<EntityStore> spectator) {
        return FOLLOWING.get(spectator);
    }

    /** True for a spectator who is not competing, or whose whole team is out: they may fly freely. */
    public static boolean canFreefly(World world, @Nonnull String session, @Nonnull Ref<EntityStore> spectator,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        var self = accessor.getComponent(spectator, PlayerRef.getComponentType());
        if (self == null) return false;

        var participants = GauntletUtils.playersFor(session);
        var teams = TeamUtils.withTeamList(world, session);
        var spectatorTeam = TeamUtils.withTeamFor(world, session, self.getUuid());

        return spectatorTeam == null || !TeamUtils.hasAlive(spectatorTeam);
    }

    /**
     * Detaches the spectator's camera from whomever it follows, leaving them to fly on their own in the
     * view their game mode locks, as the server's own free spectator camera does.
     */
    public static void release(@Nonnull Ref<EntityStore> spectator, @Nonnull ComponentAccessor<EntityStore> accessor) {
        FOLLOWING.remove(spectator);
        var player = accessor.getComponent(spectator, PlayerRef.getComponentType());
        if (player == null) {
            return;
        }
        var mode = GameModeTypes.getCurrentType(spectator, accessor);
        var locked = mode == null ? null : mode.getLockedCameraView();
        if (locked != null) {
            player.getPacketHandler().writeNoCache(new SetServerCamera(locked, true, null));
            return;
        }
        var camera = accessor.getComponent(spectator, CameraManager.getComponentType());
        if (camera != null) {
            camera.resetCamera(player);
        }
    }

    /** Forgets the spectator's target, for one who stopped spectating or left. */
    public static void forget(@Nonnull Ref<EntityStore> spectator) {
        FOLLOWING.remove(spectator);
    }

    /** Every spectator whose camera is following the target. */
    @Nonnull
    public static List<Ref<EntityStore>> followersOf(@Nonnull Ref<EntityStore> target) {
        var followers = new ArrayList<Ref<EntityStore>>();
        for (var entry : FOLLOWING.entrySet()) {
            if (!entry.getKey().isValid()) {
                FOLLOWING.remove(entry.getKey());
            } else if (Objects.equals(entry.getValue(), target)) {
                followers.add(entry.getKey());
            }
        }
        return followers;
    }

    /** Leaves spectating: exits the game mode and moves the player to where their camera was, in front of any wall. */
    public static void stop(@Nonnull Ref<EntityStore> spectator, @Nullable Ref<EntityStore> target,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        GameModeTypes.exit(spectator, accessor);
        if (target == null || !target.isValid() || target.getStore() != spectator.getStore()) {
            return;
        }
        var targetTransform = accessor.getComponent(target, TransformComponent.getComponentType());
        if (targetTransform == null) {
            return;
        }
        var head = accessor.getComponent(spectator, HeadRotation.getComponentType());
        var cameraRotation = head != null ? head.getRotation() : targetTransform.getRotation();
        var pivotHeight = Math.max(ModelComponent.getEyeHeight(target, accessor), MIN_PIVOT_HEIGHT);
        var pivot = new Vector3d(targetTransform.getPosition()).add(0, pivotHeight, 0);
        var backward = Transform.getDirection(cameraRotation.pitch(), cameraRotation.yaw()).negate();
        double distance = DISTANCE;
        var hit = TargetUtil.getTargetBlock(accessor.getExternalData().getWorld(),
                (blockTypeId, fluidId) -> BlockType.blocksLineOfSight(blockTypeId),
                pivot.x, pivot.y, pivot.z, backward.x, backward.y, backward.z, DISTANCE);
        if (hit != null) {
            distance = Math.max(MIN_EXIT_STANDOFF,
                    new Vector3d(hit.x + 0.5, hit.y + 0.5, hit.z + 0.5).distance(pivot) - WALL_MARGIN);
        }
        var position = new Vector3d(backward).mul(distance).add(pivot);
        accessor.putComponent(spectator, Teleport.getComponentType(), Teleport.createForPlayer(position, cameraRotation));
    }

    @Nullable
    private static PlayerRef findInWorld(@Nonnull World world, @Nonnull java.util.UUID uuid) {
        for (var player : world.getPlayerRefs()) {
            if (player.getUuid().equals(uuid)) {
                return player;
            }
        }
        return null;
    }
}
