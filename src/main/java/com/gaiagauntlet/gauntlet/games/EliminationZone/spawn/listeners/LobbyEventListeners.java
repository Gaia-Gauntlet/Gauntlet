package com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.listeners;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.utils.EZLog;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfig;
import com.gaiagauntlet.gauntlet.games.EliminationZone.combat.systems.SpawnDamagePreventionSystem;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.lobby.EZLobbyManager;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state.EZStates;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.components.SpawnProtectionComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.cornucopia.CornucopiaMovingPlatformsHandler;
import com.gaiagauntlet.gauntlet.plugins.announcer.utils.Announcer;
import com.gaiagauntlet.gauntlet.plugins.gamestate.events.MatchStateEvent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.ContextComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.timer.components.GameTimerComponent;
import com.gaiagauntlet.gauntlet.plugins.timer.events.TimerCompleteEvent;
import com.gaiagauntlet.gauntlet.plugins.timer.utils.TimerUtils;
import com.gaiagauntlet.gauntlet.utils.MusicUtils;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class LobbyEventListeners {

    public static final String GAME_MUSIC_CONTAINER = "Elimination_Zone_Start";
    public static final String INTRO_CAMERA_SEQUENCE = "HypeSequence";
    public static final String INTRO_TIMER = "IntroSequence";
    private LobbyEventListeners() {}

    public static void onMatchState(MatchStateEvent event) {
        if (!Objects.equals(event.getTo(), EZStates.RUNNING.name())) return;

        var session = GauntletUtils.sessionFor(event.getSessionId()).orElse(null);
        if (session == null) return;
        if (!EZController.isEz(session)) return;

        var gameEcs = event.getGame();
        var ctx = gameEcs.get(ContextComponent.getComponentType()).orElse(null);
        if (ctx == null) {
            EZLog.warn().withSession(session).log("Match started without a game context; skipping lobby start");
            return;
        }
        var world = ctx.getWorld();

        var config = EZGameConfig.get(gameEcs);
        var gameTimerComp = new GameTimerComponent(INTRO_TIMER, TimerUtils.IMG_STAGING);
        for (var poi : config.getArenaTimerPois()) {
            gameTimerComp.addPoi(poi);
        }
        TimerUtils.start(gameEcs, config.getCornucopiaDurationSeconds() + config.getCameraSequenceSeconds(), gameTimerComp);

        MusicUtils.forcePlayMusicToAllPlayers(GAME_MUSIC_CONTAINER, session);
        world.scheduleAfter(
            () -> Announcer.cinematic(session.getId(), INTRO_CAMERA_SEQUENCE),
            (long) config.getCornucopiaDurationSeconds(), TimeUnit.SECONDS
        );
    }

    public static void onTimerComplete(TimerCompleteEvent evt) {
        var game = evt.getGameStore();
        var sessionId = evt.getSessionId();
        var ctx = GameStore.getContext(game);
        var world = ctx.getWorld();

        switch (evt.getTimerId()) {
            case EZLobbyManager.STARTING_TIMER -> {
                // start the game - the timer has finished
                EZController.get().getArenaManager().start(world, game, sessionId);
            }
            case INTRO_TIMER -> {
                // timers complete inside a system tick, so the teleports wait for the store to finish processing
                world.execute(() -> CornucopiaMovingPlatformsHandler.placeAllOnPlatforms(world, game));
                TimerUtils.start(
                    game,
                    EZGameConfig.get(game).getSpawnProtectionSeconds(),
                    new GameTimerComponent(SpawnProtectionComponent.TIMER_ID, TimerUtils.IMG_GRACE_PERIOD)
                );
            }
            case SpawnProtectionComponent.TIMER_ID -> {
                SpawnDamagePreventionSystem.onEnd(evt);
            }
            default -> {
                
            }
        }
    }
}
