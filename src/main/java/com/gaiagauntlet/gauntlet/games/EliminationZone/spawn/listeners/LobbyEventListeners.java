package com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.listeners;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfig;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.components.GameTimerComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state.EZStates;
import com.gaiagauntlet.gauntlet.plugins.announcer.utils.Announcer;
import com.gaiagauntlet.gauntlet.plugins.gamestate.events.MatchStateEvent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.ContextComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.components.LobbyComponent;
import com.gaiagauntlet.gauntlet.utils.MusicUtils;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class LobbyEventListeners {

    public static final String GAME_MUSIC_CONTAINER = "Elimination_Zone_Start";
    public static final String INTRO_CAMERA_SEQUENCE = "ZoneShowcase";
    private LobbyEventListeners() {}

    public static void onMatchState(MatchStateEvent event) {
        if (!Objects.equals(event.getTo(), EZStates.RUNNING.name())) return;

        var session = GauntletUtils.sessionFor(event.getSessionId()).orElse(null);
        if (session == null) return;
        if (!EZController.isEz(session)) return;

        var gameEcs = event.getGame();
        var ctx = gameEcs.get(ContextComponent.getComponentType()).orElse(null);
        if (ctx == null) return; // kys now
        var world = ctx.getWorld();

        if (gameEcs == null) return;
        var gameTimerComp = new GameTimerComponent();
        var config = EZGameConfig.get(gameEcs);
        gameTimerComp.startTimer(config.getCornucopiaDurationSeconds() + config.getCameraSequenceSeconds());
        gameEcs.put(GameTimerComponent.getComponentType(), gameTimerComp);
        event.getGame().put(GameTimerComponent.getComponentType(), new GameTimerComponent());

        MusicUtils.forcePlayMusicToAllPlayers(GAME_MUSIC_CONTAINER, session);
        world.scheduleAfter(
            () -> Announcer.cinematic(session.getId(), INTRO_CAMERA_SEQUENCE),
            config.getCornucopiaDurationSeconds(), TimeUnit.SECONDS
        );
    }
}
