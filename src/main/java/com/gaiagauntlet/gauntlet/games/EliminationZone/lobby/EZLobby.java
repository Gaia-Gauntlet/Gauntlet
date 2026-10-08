package com.gaiagauntlet.gauntlet.games.EliminationZone.lobby;

import com.gaiagauntlet.gauntlet.core.events.GauntletEventRegistry;
import com.gaiagauntlet.gauntlet.games.EliminationZone.lobby.listeners.LobbyEventListeners;
import com.gaiagauntlet.gauntlet.plugins.gamestate.events.MatchStateEvent;

public class EZLobby {
    private EZLobby() {
    }

    public static void setup() {
        GauntletEventRegistry.on(MatchStateEvent.class, LobbyEventListeners::onMatchState);
    }
}
