package com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.cornucopia;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.utils.EZLog;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfig;
import com.gaiagauntlet.gauntlet.games.EliminationZone.components.EZPlayerComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.components.RisingBlockComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.lobby.EZSpawnComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestate.events.MatchStateEvent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.ContextComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamType;
import com.gaiagauntlet.gauntlet.plugins.teams.utils.TeamUtils;
import com.gaiagauntlet.gauntlet.utils.BlockUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    public static void onMatchStart(MatchStateEvent evt) {

        var gameStore = evt.getGame();
        var gameContext = gameStore.get(ContextComponent.getComponentType()).orElse(null);
        if (gameContext == null)
            return; // kill yourself now

        var world = gameContext.getWorld();

        var config = EZGameConfig.get(gameStore);

        Collection<Ref<EntityStore>> entities = new HashSet<>();

        var spawnsComponent = gameStore.ensure(EZSpawnComponent.getComponentType(), () -> new EZSpawnComponent(world));

        rise(world, config.getCameraSequenceSeconds(), spawnsComponent.getAllFilled());

        world.scheduleAfter(() -> {
            for (Ref<EntityStore> ref : entities) {
                if (!ref.isValid())
                    continue; // don't process invalid entities. Since this happens later, we don't know what
                              // happened to them
                ArenaBlocks.entityToBlock(world, ref);
            }
        }, (long) config.getCameraSequenceSeconds(), TimeUnit.SECONDS);
    }

    public static void rise(@Nonnull World world, double durationSeconds, @Nonnull List<Transform> spawns) {
        var store = world.getEntityStore().getStore();
        var velocity = new Vector3d(0, durationSeconds <= 0 ? HEIGHT : HEIGHT / durationSeconds, 0);
        var components = Map.of(RisingBlockComponent.getComponentType(), new RisingBlockComponent(velocity));
        var entities = new HashSet<Ref<EntityStore>>();
        for (var spawn : spawns) {
            var base = ArenaBlocks.blockAt(spawn.getPosition());
            for (int depth = 1; depth <= 2; depth++) {
                var ref = ArenaBlocks.blockToEntity(world, new Vector3i(base.x, base.y - depth, base.z), store,
                        components);
                if (ref != null) {
                    entities.add(ref);
                }
            }
        }
        EZLog.info().log("Cornucopia rising: " + entities.size() + " blocks for " + spawns.size() + " spawns over " + durationSeconds);
        world.scheduleAfter(() -> {
            for (var ref : entities) {
                if (ref.isValid()) {
                    ArenaBlocks.entityToBlock(world, ref);
                }
            }
        }, (long) Math.max(0, durationSeconds), TimeUnit.SECONDS);
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
        world.setBlock(under.x, under.y - 3, under.z, FALLBACK_BLOCK_ID);
        store.putComponent(ref, Teleport.getComponentType(), Teleport.createForPlayer(world, goal));
    }
}
