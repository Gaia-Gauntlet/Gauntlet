package com.gaiagauntlet.gauntlet.games.EliminationZone.managers.lobby;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.gaiagauntlet.gauntlet.utils.MusicUtils;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;
import com.gaiagauntlet.gauntlet.plugins.announcer.utils.Announcer;
import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.msg;
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
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
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
        GaiaLog.atInfo().log("cleaning world " + arenaWorld.getName());
        return GauntletUtils.runAsync(arenaWorld, () -> {
            var arenaPlayers = arenaWorld.getPlayerRefs();
            for (PlayerRef arenaPlayer : arenaPlayers) {
                PlayerUtils.reset(arenaPlayer);
            }
            return arenaWorld.drainPlayersTo(GauntletUtils.withHubWorld(), arenaPlayers);
        }).thenCompose(drained -> drained)
                .whenComplete((_, error) -> GauntletUtils.runAsync(arenaWorld,
                        () -> InstancesPlugin.safeRemoveInstance(arenaWorld)));
    }

    @Override
    public void onJoin(Ref<EntityStore> ref, PlayerRef player, ComponentAccessor<EntityStore> accessor,
            String sessionId) {
        // add components
        accessor.putComponent(ref, EZPlayerComponent.getComponentType(), new EZPlayerComponent(sessionId));
        var world = accessor.getExternalData().getWorld();
        var gameStore = GameStore.withStore(world, sessionId).orElse(null);
        if (gameStore == null)
            return;

        arrive(ref, player, accessor, gameStore);
        // every arrival changes who is online, standing, or on a team for the players
        // already here
        TeamUtils.updateHuds(gameStore, Set.of());
    }

    private static void arrive(Ref<EntityStore> ref, PlayerRef player, ComponentAccessor<EntityStore> accessor,
            GameEcs gameStore) {
        var assigned = TeamUtils.teamOf(gameStore, player.getUuid()).orElse(null);
        if (assigned != null && !assigned.isParticipant()) {
            PlayerUtils.reset(player);
            spectate(ref, accessor);
            return;
        }
        var state = EZState.currentState(gameStore);
        if (state == EZStates.LOBBY) {
            arriveInLobby(ref, player, accessor, gameStore);
            return;
        }
        var team = TeamUtils.teamOf(gameStore, player.getUuid()).filter(TeamComponent::isParticipant).orElse(null);
        if (team == null) {
            bench(ref, player, accessor, gameStore, msg("server.games.ez.lobby.late"));
            return;
        }
        if (TimerUtils.isRunning(gameStore, LobbyEventListeners.INTRO_TIMER)) {
            compete(ref, player, accessor, team);
            return;
        }
        if (state != EZStates.RUNNING && state != EZStates.SUDDEN_DEATH)
            return;

        accessor.putComponent(ref, TeamPlayerComponent.getComponentType(), new TeamPlayerComponent(team.getId()));
        var ledger = gameStore.ensure(EZGameComponent.getComponentType(), EZGameComponent::new);
        var teammate = ledger.isEliminated(player.getUuid()) ? null
                : livingTeammate(team, player, accessor.getExternalData().getWorld());
        if (teammate == null) {
            ledger.eliminate(player.getUuid());
            accessor.putComponent(ref, EliminatedComponent.getComponentType(),
                    new EliminatedComponent(EliminationReason.KILLED));
            spectate(ref, accessor);
            return;
        }
        accessor.putComponent(ref, Teleport.getComponentType(),
                Teleport.createForPlayer(teammate.getPosition(), teammate.getRotation()));
    }

    private static void arriveInLobby(Ref<EntityStore> ref, PlayerRef player, ComponentAccessor<EntityStore> accessor,
            GameEcs gameStore) {
        var config = EZGameConfig.get(gameStore);
        var uuid = player.getUuid();
        var team = TeamUtils.teamOf(gameStore, uuid).filter(TeamComponent::isParticipant).orElse(null);
        if (team == null && TimerUtils.isRunning(gameStore, STARTING_TIMER)) {
            bench(ref, player, accessor, gameStore, msg("server.games.ez.lobby.late"));
            return;
        }
        if (TeamUtils.countAlive(gameStore, Set.of(uuid)) >= config.getMaxPlayers()) {
            bench(ref, player, accessor, gameStore,
                    msg("server.games.ez.lobby.full").param("max", config.getMaxPlayers()));
            return;
        }
        if (team == null) {
            TeamUtils.teamsOf(gameStore).ifPresent(teams -> TeamUtils.distributePlayers(List.of(player), teams));
            team = TeamUtils.teamOf(gameStore, uuid).orElse(null);
        }
        if (team == null) {
            bench(ref, player, accessor, gameStore,
                    msg("server.games.ez.lobby.full").param("max", config.getMaxPlayers()));
            return;
        }
        compete(ref, player, accessor, team);
        announce(gameStore, "server.games.ez.lobby.join", player, Set.of());
        countDown(gameStore);
    }

    /** Marked as competing in this match, there is a valid slot for them */
    private static void compete(Ref<EntityStore> ref, PlayerRef player, ComponentAccessor<EntityStore> accessor,
            TeamComponent team) {
        accessor.tryRemoveComponent(ref, EliminatedComponent.getComponentType());
        accessor.putComponent(ref, TeamPlayerComponent.getComponentType(), new TeamPlayerComponent(team.getId()));
        PlayerUtils.reset(player);
    }

    /**
     * Marks the entity as dead, putting them on the bench. Yall gotta sit out this
     * one mate
     */
    private static void bench(Ref<EntityStore> ref, PlayerRef player, ComponentAccessor<EntityStore> accessor,
            GameEcs gameStore, Message notice) {
        var uuid = player.getUuid();
        TeamUtils.teamsOf(gameStore).ifPresent(teams -> {
            var team = teams.get(uuid);
            if (team != null)
                teams.remove(uuid, team.getId());
        });
        gameStore.get(EZSpawnComponent.getComponentType()).ifPresent(spawns -> spawns.release(uuid));
        accessor.tryRemoveComponent(ref, TeamPlayerComponent.getComponentType());
        PlayerUtils.reset(player);
        if (accessor.getComponent(ref, EliminatedComponent.getComponentType()) == null)
            accessor.addComponent(ref, EliminatedComponent.getComponentType(),
                    new EliminatedComponent(EliminationReason.KILLED));
        spectate(ref, accessor);
        player.sendMessage(notice);
    }

    /** Marks the player as a spectator */
    private static void spectate(Ref<EntityStore> ref, ComponentAccessor<EntityStore> accessor) {
        if (accessor.getComponent(ref, SpectatorComponent.getComponentType()) == null)
            accessor.addComponent(ref, SpectatorComponent.getComponentType(), new SpectatorComponent());
    }

    private static void announce(GameEcs gameStore, String key, PlayerRef player, Set<UUID> out) {
        var config = EZGameConfig.get(gameStore);
        var count = TeamUtils.countAlive(gameStore, out);
        var status = count < config.getMinPlayers()
                ? msg("server.games.ez.lobby.status.waiting").param("count", count).param("needed",
                        config.getMinPlayers())
                : msg("server.games.ez.lobby.status.ready").param("count", count).param("max", config.getMaxPlayers());
        Announcer.chat(GameStore.getContext(gameStore).getWorld(),
                msg(key).param("player", player.getUsername()).param("status", status));
    }

    private static void countDown(GameEcs gameStore) {
        var config = EZGameConfig.get(gameStore);
        var arrived = TeamUtils.countAlive(gameStore, Set.of());
        if (arrived < config.getMinPlayers())
            return;
        var expected = Math.min(GauntletUtils.playersFor(gameStore.getSessionId()).size(), config.getMaxPlayers());
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
        MusicUtils.forcePlayMusicToAllPlayers(LobbyEventListeners.PREGAME_MUSIC_CONTAINER,
                GameStore.getContext(gameStore).getWorld());
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
            Collection<PlayerRef> players, boolean forfeit) {
        var gameStore = GameStore.withStore(arenaWorld, session).orElse(null);
        if (gameStore == null)
            return CompletableFuture.completedFuture(null);
        var leaving = players.stream().map(PlayerRef::getUuid).collect(Collectors.toSet());
        var state = EZState.currentState(gameStore);
        if (state == EZStates.LOBBY || forfeit)
            leaveTeams(gameStore, arenaWorld, players, leaving, state == EZStates.LOBBY);

        var arenaId = arenaWorld.getWorldConfig().getUuid();
        var landings = new ArrayList<CompletableFuture<Void>>();
        for (var player : players) {
            var ref = player.getReference();
            if (ref == null || !ref.isValid() || !arenaId.equals(player.getWorldUuid()))
                continue;
            PlayerUtils.reset(player);
            landings.add(PlayerUtils.sendToHub(player).exceptionally(error -> {
                GaiaLog.atError(error).withSession(session)
                        .log("Could not return " + player.getUsername() + " to the hub");
                return null;
            }));
        }

        EZState.checkEnd(gameStore);
        return CompletableFuture.allOf(landings.toArray(new CompletableFuture[0]));
    }

    private static void leaveTeams(GameEcs gameStore, World arena, Collection<PlayerRef> players, Set<UUID> leaving,
            boolean lobby) {
        var teams = TeamUtils.teamsOf(gameStore).orElse(null);
        if (teams == null)
            return;
        var arenaId = arena.getWorldConfig().getUuid();
        var spawns = gameStore.get(EZSpawnComponent.getComponentType()).orElse(null);
        for (var player : players) {
            var team = teams.get(player.getUuid());
            if (team == null)
                continue;
            teams.remove(player.getUuid(), team.getId());
            if (spawns != null)
                spawns.release(player.getUuid());
            if (lobby && team.isParticipant() && arenaId.equals(player.getWorldUuid()))
                announce(gameStore, "server.games.ez.lobby.leave", player, leaving);
        }
        if (lobby && TeamUtils.countAlive(gameStore, leaving) < EZGameConfig.get(gameStore).getMinPlayers()
                && TimerUtils.isRunning(gameStore, STARTING_TIMER))
            TimerUtils.cancel(gameStore);
        TeamUtils.updateHuds(gameStore, Set.of());
    }

    public CompletableFuture<Transform> locationFor(PlayerRef player, World world, String sessionId) {
        return GauntletUtils.runAsync(world, () -> {

            var ecs = GameStore.ensureStore(world, sessionId);
            var spawnComp = ecs.ensure(EZSpawnComponent.getComponentType(), () -> new EZSpawnComponent(world));

            return spawnComp.getNext(player);
        });
    }
}
