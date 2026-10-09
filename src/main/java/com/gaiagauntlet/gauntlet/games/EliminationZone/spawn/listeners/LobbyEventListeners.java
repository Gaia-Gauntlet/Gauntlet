package com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.listeners;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfig;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state.EZStates;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.components.GameTimerComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestate.events.MatchStateEvent;
import java.util.Objects;

public final class LobbyEventListeners {
    private LobbyEventListeners() {}

    public static void onMatchState(MatchStateEvent event) {
        if (!Objects.equals(event.getTo(), EZStates.RUNNING.name())) return;

        var session = GauntletUtils.sessionFor(event.getSessionId()).orElse(null);
        if (session == null) return;
        if (!EZController.isEz(session)) return;

        var gameEcs = event.getGame();

        var gameTimerComp = new GameTimerComponent();
        var config = EZGameConfig.get(gameEcs);
        gameTimerComp.startTimer(config.getCornucopiaDurationSeconds() + config.getCameraSequenceSeconds());
        gameEcs.put(GameTimerComponent.getComponentType(), gameTimerComp);
        event.getGame().put(GameTimerComponent.getComponentType(), new GameTimerComponent());
    }
}
