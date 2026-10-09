package com.gaiagauntlet.gauntlet.games.EliminationZone.lobby;

import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;
import com.gaiagauntlet.gauntlet.games.EliminationZone.lobby.listeners.LobbyEventListeners;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.services.ZoneTickSystem;
import com.gaiagauntlet.gauntlet.plugins.events.events.MatchEventRegistry;
import com.gaiagauntlet.gauntlet.plugins.gamestate.events.MatchStateEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

public class EZLobby {
    private EZLobby() {
    }

    public static void setup(JavaPlugin plugin) {
        MatchEventRegistry.register(MatchStateEvent.class, EZController.ID, LobbyEventListeners::onMatchState);
    }
}
