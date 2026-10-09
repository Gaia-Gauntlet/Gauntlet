package com.gaiagauntlet.gauntlet.games.EliminationZone.managers.lobby;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfig;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.interfaces.LobbyManager;
import com.gaiagauntlet.gauntlet.utils.WorldUtils;
import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class EZLobbyManager implements LobbyManager {

    @Override
    public CompletableFuture<World> setupWorld(ComponentAccessor<EntityStore> hubAccessor, String sessionId) {
        var hubWorld = hubAccessor.getExternalData().getWorld();

        var cfg = EZGameConfig.get(hubWorld, sessionId);

        return WorldUtils.spawnInstance(cfg.getInstanceTemplateName(), hubWorld, WorldUtils.spawnOf(hubWorld))
                .whenComplete((world, err) -> {
                    // TODO: More validation and proper setup of systems

                    if (err != null) {
                        GaiaLog.atError(err).withSession(sessionId)
                                .log("Failed to spawn instance " + cfg.getInstanceTemplateName());
                        return;
                    }
                    GaiaLog.atInfo().withSession(sessionId)
                            .log("Spawned instance " + world.getName() + " from " + cfg.getInstanceTemplateName());
                });
    }

    @Override
    public CompletableFuture<Void> cleanWorld(World arenaWorld) {
        InstancesPlugin.safeRemoveInstance(arenaWorld);
        GaiaLog.atInfo().log("cleaning world...");
        return GauntletUtils.runAsync(arenaWorld, () -> {
            var arenaPlayers = arenaWorld.getPlayerRefs();
            var hubWorld = GauntletUtils.withHubWorld();
            arenaWorld.drainPlayersTo(hubWorld, arenaPlayers);
        });
    };

    @Override
    public void onJoin(Ref<EntityStore> ref, ComponentAccessor<EntityStore> accessor, String sessionId) {
        // stuffs
        GaiaLog.atError().log("onJoin is not implemented for EZGameController!");
    }

    @Override
    public CompletableFuture<Void> onDisconnect(World arenaWorld, String session, PlayerRef player) {
        GaiaLog.atError().log("onDisconnect is not implemented for EZGameController!");
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> onLeave(World arenaWorld, String session, Collection<PlayerRef> player) {
        GaiaLog.atError().log("onLeave is not implemented for EZGameController!");
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Transform> locationFor(PlayerRef player, World world, String sessionId) {
        return GauntletUtils.runAsync(world, () -> {

            var ecs = GameStore.ensureStore(world, sessionId);
            var spawnComp = ecs.ensure(EZSpawnComponent.getComponentType(), () -> new EZSpawnComponent(world));

            return spawnComp.getNext(player);
        });
    }
}
