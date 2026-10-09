package com.gaiagauntlet.gauntlet.games.EliminationZone.managers.lobby;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.party.utils.PartyUtils;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfig;
import com.gaiagauntlet.gauntlet.games.EliminationZone.components.EZGameComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.components.EZPlayerComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state.EZState;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state.EZStates;
import com.gaiagauntlet.gauntlet.plugins.gamestate.utils.MatchUtils;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.interfaces.LobbyManager;
import com.gaiagauntlet.gauntlet.plugins.teams.utils.TeamUtils;
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
    public void onJoin(Ref<EntityStore> ref, PlayerRef player, ComponentAccessor<EntityStore> accessor, String sessionId) {
        // add components
        accessor.putComponent(ref, EZPlayerComponent.getComponentType(), new EZPlayerComponent(sessionId));
        var world = accessor.getExternalData().getWorld();
        var gameStore = GameStore.withStore(world, sessionId).orElse(null);
        if (gameStore == null) return;

        // Increment participant counter
        var gameComp = gameStore.ensure(EZGameComponent.getComponentType(), EZGameComponent::new);
        gameComp.incrementParticipants();

        if (EZState.currentState(gameStore).equals(EZStates.RUNNING)) {
            if (gameComp.getParticipants() > EZGameConfig.get(world, sessionId).getMinPlayers()) {
                EZController.get().getArenaManager().start(world, gameStore, sessionId);
                return;
            }
        }

        var team = TeamUtils.withTeamFor(world, sessionId, player.getUuid());
        if (team == null) return;
        var onlinePlayers = TeamUtils.getOnlinePlayers(team);
        for (var teamPlayer : onlinePlayers) {
            var comp = teamPlayer.getComponentConcurrent(EZPlayerComponent.getComponentType());
            if (comp != null && comp.isAlive()) {
                // warp to player
                
            }
        }
        // if party is alive
    }



    @Override
    public CompletableFuture<Void> onLeave(World arenaWorld, String session, Collection<PlayerRef> players) {
        // Increment participant counter
        var gameStore = GameStore.withStore(arenaWorld, session).orElse(null);
        if (gameStore == null) return CompletableFuture.completedFuture(null);
        var gameComp = gameStore.ensure(EZGameComponent.getComponentType(), EZGameComponent::new);

        for (var player : players) {
            gameComp.decrementParticipants();
            var ref = player.getReference();
            if (!ref.isValid()) {
                GaiaLog.atWarning().withSession(session).log("Player " + player.getUsername() + " has an invalid ref, unable to clean");
                continue;
            }
            var store = ref.getStore();
            var playerWorld = store.getExternalData().getWorld();
            GauntletUtils.run(playerWorld, () -> {
                // remove the component off the player
                try {
                    store.tryRemoveComponent(ref, EZPlayerComponent.getComponentType());
                } catch (Exception e) {
                    GaiaLog.atError(e).withSession(session).log("Failed to clean " + player.getUsername() + " from EZGame with cause " + e.getLocalizedMessage());
                }
            });
        }

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
