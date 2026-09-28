package com.gaiagauntlet.gauntlet.games.EliminationZone;

import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.arena.EZArenaManager;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.lobby.EZLobbyManager;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.LobbyController;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.interfaces.ArenaManager;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.interfaces.LobbyManager;
import com.hypixel.hytale.logger.HytaleLogger;

public class EZController extends LobbyController {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "EZGameController";

    @Override
    // note - this will need to be fixed so a new instance isn't made every time we need it
    // I would like to avoid a singleton if possible though. Maybe store it locally here? 
    public ArenaManager getArenaManager() {
        return new EZArenaManager();
    }
    @Override
    public LobbyManager getLobbyManager() {
        return new EZLobbyManager();
    }

}
