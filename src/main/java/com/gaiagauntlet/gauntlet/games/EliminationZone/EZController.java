package com.gaiagauntlet.gauntlet.games.EliminationZone;

import java.util.List;

import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.arena.EZArenaManager;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.lobby.EZLobbyManager;
import com.gaiagauntlet.gauntlet.plugins.announcer.AnnouncerPlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestate.GameStatePlugin;
import com.gaiagauntlet.gauntlet.core.gamestore.GameStorePlugin;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.LobbyController;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.LobbyControllerPlugin;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.interfaces.ArenaManager;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.interfaces.LobbyManager;
import com.gaiagauntlet.gauntlet.plugins.proxychat.ProxyChatPlugin;
import com.gaiagauntlet.gauntlet.plugins.scoring.ScoringPlugin;
import com.gaiagauntlet.gauntlet.plugins.teams.TeamsPlugin;
import com.hypixel.hytale.logger.HytaleLogger;

public class EZController extends LobbyController {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "EZGameController";

    @Override
    // note - this will need to be fixed so a new instance isn't made every time we
    // need it
    // I would like to avoid a singleton if possible though. Maybe store it locally
    // here?
    public ArenaManager getArenaManager() {
        return new EZArenaManager();
    }

    @Override
    public LobbyManager getLobbyManager() {
        return new EZLobbyManager();
    }

    @Override
    public AdminTab getAdminTab() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAdminTab'");
    }

    @Override // temp, just messing around here
    public List<String> getPluginIds() {
        return List.of(
                AnnouncerPlugin.ID,
                GameStatePlugin.ID,
                GameStorePlugin.ID,
                LobbyControllerPlugin.ID,
                ProxyChatPlugin.ID,
                TeamsPlugin.ID,
                ScoringPlugin.ID);
    }

}
