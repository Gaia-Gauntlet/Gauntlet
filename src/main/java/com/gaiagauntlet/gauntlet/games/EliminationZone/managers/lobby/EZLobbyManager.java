package com.gaiagauntlet.gauntlet.games.EliminationZone.managers.lobby;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;
import com.gaiagauntlet.gauntlet.plugins.teams.components.entity.EliminatedComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.entity.TeamPlayerComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.entity.EliminatedComponent.EliminationReason;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamComponent;
import com.gaiagauntlet.gauntlet.plugins.spectator.components.SpectatorComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.listeners.LobbyEventListeners;
import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfig;
import com.gaiagauntlet.gauntlet.games.EliminationZone.components.EZGameComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.components.EZPlayerComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state.EZState;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state.EZStates;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.interfaces.LobbyManager;
import com.gaiagauntlet.gauntlet.plugins.teams.utils.TeamUtils;
import com.gaiagauntlet.gauntlet.plugins.timer.components.GameTimerComponent;
import com.gaiagauntlet.gauntlet.plugins.timer.utils.TimerUtils;
import com.gaiagauntlet.gauntlet.utils.WorldUtils;
import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SpawnUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class EZLobbyManager implements LobbyManager {
    public static final String STARTING_TIMER = "GameStarting";

    @Override
    public CompletableFuture<World> setupWorld(ComponentAccessor<EntityStore> hubAccessor, String sessionId) {
        var hubWorld = hubAccessor.getExternalData().getWorld();

        var cfg = EZGameConfig.get(GauntletUtils.sessionFor(sessionId).orElse(null));

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
            for (PlayerRef arenaPlayer : arenaPlayers) {
                PlayerUtils.reset(arenaPlayer);
            }
            return arenaWorld.drainPlayersTo(hubWorld, arenaPlayers);
        }).thenCompose(drained -> drained);
    };

    @Override
    public void onJoin(Ref<EntityStore> ref, PlayerRef player, ComponentAccessor<EntityStore> accessor,
            String sessionId) {
        // add components
        accessor.putComponent(ref, EZPlayerComponent.getComponentType(), new EZPlayerComponent(sessionId));
        var world = accessor.getExternalData().getWorld();
        var gameStore = GameStore.withStore(world, sessionId).orElse(null);
        if (gameStore == null)
            return;

        arrive(ref, player, accessor, sessionId, gameStore);
        // every arrival changes who is online, standing, or on a team for the players
        // already here
        TeamUtils.updateHuds(gameStore, Set.of());
    }

    private static void arrive(Ref<EntityStore> ref, PlayerRef player, ComponentAccessor<EntityStore> accessor,
            String sessionId, GameEcs gameStore) {
        // Increment participant counter
        var gameComp = gameStore.ensure(EZGameComponent.getComponentType(), EZGameComponent::new);
        gameComp.incrementParticipants();

        var state = EZState.currentState(gameStore);
        // allow late joins during intro sequence
        if (state == EZStates.LOBBY || TimerUtils.isRunning(gameStore, LobbyEventListeners.INTRO_TIMER)) {
            joinLobby(ref, player, accessor, gameStore);
            PlayerUtils.reset(player);
            countDown(gameStore, gameComp.getParticipants(), GauntletUtils.playersFor(sessionId).size());
            return;
        }
        if (accessor.getComponent(ref, TeamPlayerComponent.getComponentType()) == null) {
            var teamId = TeamUtils.teamOf(gameStore, player.getUuid()).map(TeamComponent::getId).orElse(null);
            if (teamId != null) {
                // only add the teamPlayerComp if the player is on an existing team
                accessor.putComponent(ref, TeamPlayerComponent.getComponentType(), new TeamPlayerComponent(teamId));
            }
        }
        if ((state != EZStates.RUNNING && state != EZStates.SUDDEN_DEATH)
                || TimerUtils.isRunning(gameStore, LobbyEventListeners.INTRO_TIMER))
            return;

        var team = TeamUtils.teamOf(gameStore, player.getUuid()).orElse(null);
        if (team == null || !team.isParticipant()
                || accessor.getComponent(ref, EliminatedComponent.getComponentType()) != null) {
            var ezComp = accessor.getComponent(ref, EliminatedComponent.getComponentType());
            if (ezComp != null && ezComp.getReason() == EliminationReason.KILLED) { // if previously killed, spectator
                                                                                    // for thou
                spectate(ref, accessor);
                return;
            }
        }
        var teammate = livingTeammate(team, player, accessor.getExternalData().getWorld());
        if (teammate == null) {
            accessor.addComponent(ref, EliminatedComponent.getComponentType(),
                    new EliminatedComponent(EliminationReason.KILLED));
            spectate(ref, accessor);
            return;
        }
        accessor.putComponent(ref, Teleport.getComponentType(),
                Teleport.createForPlayer(teammate.getPosition(), teammate.getRotation()));
    }

    private static void countDown(GameEcs gameStore, int arrived, int expected) {
        var config = EZGameConfig.get(gameStore);
        if (arrived < config.getMinPlayers())
            return;
        var everyone = arrived >= expected;
        if (TimerUtils.isRunning(gameStore, STARTING_TIMER)) {
            var timer = TimerUtils.get(gameStore).orElseThrow();
            if (everyone && timer.getCountdownRemaining() > config.getStartBufferSeconds())
                timer.setTimer(config.getStartBufferSeconds());
            return;
        }
        var gameTimerComp = new GameTimerComponent(STARTING_TIMER, TimerUtils.IMG_COUNTDOWN);
        for (var poi : config.getArenaTimerPois()) {
            gameTimerComp.addPoi(poi);
        }
        TimerUtils.start(gameStore, everyone ? config.getStartBufferSeconds() : config.getArrivalTimeoutSeconds(),
                gameTimerComp);
    }

    // a match that has not started gives every arrival a clean slate and a team,
    // since state from an earlier match is saved with the player
    private static void joinLobby(Ref<EntityStore> ref, PlayerRef player, ComponentAccessor<EntityStore> accessor,
            GameEcs gameStore) {
        accessor.tryRemoveComponent(ref, EliminatedComponent.getComponentType());
        TeamUtils.teamsOf(gameStore).ifPresent(teams -> TeamUtils.distributePlayers(List.of(player), teams));
        var teamId = TeamUtils.teamOf(gameStore, player.getUuid()).map(TeamComponent::getId).orElse(null);
        accessor.putComponent(ref, TeamPlayerComponent.getComponentType(), new TeamPlayerComponent(teamId));
    }

    private static void spectate(Ref<EntityStore> ref, ComponentAccessor<EntityStore> accessor) {
        if (accessor.getComponent(ref, SpectatorComponent.getComponentType()) == null)
            accessor.addComponent(ref, SpectatorComponent.getComponentType(), new SpectatorComponent());
    }

    @Nullable
    private static Transform livingTeammate(TeamComponent team, PlayerRef self, World arena) {
        for (var uuid : team.getPlayers()) {
            if (uuid.equals(self.getUuid()))
                continue;
            var teammate = PlayerUtils.get(uuid);
            var ref = teammate == null ? null : teammate.getReference();
            if (ref == null || !ref.isValid() || ref.getStore() != arena.getEntityStore().getStore())
                continue;
            var store = ref.getStore();
            if (store.getComponent(ref, EliminatedComponent.getComponentType()) != null
                    || store.getComponent(ref, SpectatorComponent.getComponentType()) != null)
                continue;
            var transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform != null)
                return transform.getTransform();
        }
        return null;
    }

    @Override
    public CompletableFuture<Void> onLeave(World hubWorld, World arenaWorld, String session,
            Collection<PlayerRef> players) {
        // Increment participant counter
        var gameStore = GameStore.withStore(arenaWorld, session).orElse(null);
        if (gameStore == null)
            return CompletableFuture.completedFuture(null);
        var gameComp = gameStore.ensure(EZGameComponent.getComponentType(), EZGameComponent::new);
        var cleanupFutures = new ArrayList<CompletableFuture<Void>>();

        for (var player : players) {
            gameComp.decrementParticipants();
            PlayerUtils.reset(player);
            var ref = player.getReference();
            if (ref == null || !ref.isValid()) {
                GaiaLog.atWarning().withSession(session)
                        .log("Player " + player.getUsername() + " has an invalid ref, unable to clean");
                continue;
            }
            var store = ref.getStore();
            var playerWorld = store.getExternalData().getWorld();
            var cleanupFuture = GauntletUtils.runAsync(playerWorld, () -> {
                // remove the component off the player
                try {
                    store.addComponent(ref, EliminatedComponent.getComponentType(),
                            new EliminatedComponent(EliminationReason.KILLED));
                    store.tryRemoveComponent(ref, EZPlayerComponent.getComponentType());
                } catch (Exception e) {
                    GaiaLog.atError(e).withSession(session).log("Failed to clean " + player.getUsername()
                            + " from EZGame with cause " + e.getLocalizedMessage());
                }
            });
            cleanupFutures.add(cleanupFuture);

        }

        if (EZState.currentState(gameStore) == EZStates.LOBBY
                && gameComp.getParticipants() < EZGameConfig.get(gameStore).getMinPlayers()
                && TimerUtils.isRunning(gameStore, STARTING_TIMER)) {
            TimerUtils.cancel(gameStore);
        }

        return CompletableFuture.allOf(cleanupFutures.toArray(new CompletableFuture[0]));
    }

    public CompletableFuture<Transform> locationFor(PlayerRef player, World world, String sessionId) {
        return GauntletUtils.runAsync(world, () -> {

            var ecs = GameStore.ensureStore(world, sessionId);
            var spawnComp = ecs.ensure(EZSpawnComponent.getComponentType(), () -> new EZSpawnComponent(world));

            return spawnComp.getNext(player);
        });
    }
}
