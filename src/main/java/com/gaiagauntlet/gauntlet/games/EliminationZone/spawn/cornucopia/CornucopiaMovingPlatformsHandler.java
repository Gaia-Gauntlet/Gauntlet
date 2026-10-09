package com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.cornucopia;

import com.gaiagauntlet.gauntlet.core.utils.EZLog;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfig;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.lobby.EZSpawnComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state.EZStates;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.components.RisingBlockComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestate.events.MatchStateEvent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.ContextComponent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * I would love if we could make this triggered via the Trigger Volume instead
 * of via config - but alas, not sure that is possible T-T
 * CornucopiaMovingPlatformsHandler
 */
public class CornucopiaMovingPlatformsHandler {
    public static final int HEIGHT = 43; // TODO: Make this a config option.
    public static final String FALLBACK_BLOCK_ID = "Rock_Ledge_Brick_Smooth"; // TODO: Make this a config option.
    private static final int PLATFORM_DEPTH = 2;

    public static void onMatchStart(MatchStateEvent evt) {
        if (!Objects.equals(evt.getTo(), EZStates.RUNNING.name())) return;

        var gameStore = evt.getGame();
        var gameContext = gameStore.get(ContextComponent.getComponentType()).orElse(null);
        if (gameContext == null) {
            EZLog.warn().withSession(evt.getSessionId()).log("Match started without a game context; platforms will not rise");
            return;
        }

        var world = gameContext.getWorld();
        var config = EZGameConfig.get(gameStore);
        var spawns = gameStore.ensure(EZSpawnComponent.getComponentType(), () -> new EZSpawnComponent(world))
                .getAllFilled();

        // queued rather than run inline because entities cannot be added while a store is mid tick
        world.execute(() -> rise(world, config.getCameraSequenceSeconds(), spawns));
    }

    public static void rise(@Nonnull World world, double durationSeconds, @Nonnull List<Transform> spawns) {
        var seconds = Math.max(0, durationSeconds);
        var entities = new ArrayList<Ref<EntityStore>>();
        for (var spawn : spawns) {
            var base = ArenaBlocks.blockAt(spawn.getPosition());
            for (int depth = 1; depth <= PLATFORM_DEPTH; depth++) {
                var position = new Vector3i(base.x, base.y - depth, base.z);
                var targetY = position.y + 0.5 + HEIGHT;
                var speed = seconds <= 0 ? HEIGHT : HEIGHT / seconds;
                var ref = ArenaBlocks.blockToEntity(world, position, holder -> holder.addComponent(
                        RisingBlockComponent.getComponentType(), new RisingBlockComponent(speed, targetY)));
                if (ref != null) {
                    entities.add(ref);
                }
            }
        }
        EZLog.info().log("Cornucopia rising: " + entities.size() + " blocks for " + spawns.size() + " spawns over "
                + seconds + "s");

        world.scheduleAfter(() -> {
            for (var ref : entities) {
                ArenaBlocks.entityToBlock(world, ref);
            }
        }, (long) (seconds * 1000), TimeUnit.MILLISECONDS);
    }

    /**
     * Puts a player on top of their risen platform, with a solid block under them
     * in case the platform drifted.
     */
    public static void placeOnPlatform(@Nonnull World world, @Nonnull PlayerRef player, @Nonnull Transform spawn) {
        var ref = player.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        var store = ref.getStore();
        var goal = new Transform(spawn);
        var current = store.getComponent(ref, TransformComponent.getComponentType());
        if (current != null) {
            goal.setRotation(current.getRotation());
        }
        goal.getPosition().add(0, HEIGHT + 1, 0);
        var under = ArenaBlocks.blockAt(goal.getPosition());
        ArenaBlocks.placeBlock(world, new Vector3i(under.x, under.y - 3, under.z), FALLBACK_BLOCK_ID,
                RotationTuple.NONE_INDEX);
        store.putComponent(ref, Teleport.getComponentType(), Teleport.createForPlayer(world, goal));
    }
}
