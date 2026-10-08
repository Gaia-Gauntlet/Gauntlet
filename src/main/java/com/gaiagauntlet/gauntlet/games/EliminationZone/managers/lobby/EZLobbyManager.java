package com.gaiagauntlet.gauntlet.games.EliminationZone.managers.lobby;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfigAsset;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.interfaces.LobbyManager;
import com.gaiagauntlet.gauntlet.utils.WorldUtils;
import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class EZLobbyManager implements LobbyManager {

    @Override
    public CompletableFuture<World> setupWorld(ComponentAccessor<EntityStore> hubAccessor, String sessionId) {
        var hubWorld = hubAccessor.getExternalData().getWorld();

        // this has gotta be fixed once lordi finalizes the config branch. Fully on me
        // for merging early tbh
        var cfg = new EZGameConfigAsset(); // = GauntletUtils.sessionFor(sessionId)
        // .flatMap(session ->
        // session.get(SessionGameConfigComponent.getComponentType()))
        // .filter(config -> config != null && config.getConfig(EZController.ID) !=
        // null)
        // .map(config -> (EZGameConfigAsset)
        // GameConfigAsset.getAssetMap().get(config.getConfig(EZController.ID))).orElse(new
        // EZGameConfigAsset());

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
}
