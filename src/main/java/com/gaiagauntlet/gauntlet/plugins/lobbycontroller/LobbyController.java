package com.gaiagauntlet.gauntlet.plugins.lobbycontroller;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GameController;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.interfaces.ArenaManager;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.interfaces.LobbyManager;

/** Should enforce the implementation of a Lobby-Arena system */
public abstract class LobbyController extends GameController {


    public abstract LobbyManager getLobbyManager();
    public abstract ArenaManager getArenaManager();

    @Override
    public final void setupGame() {
        // setup the game
    };

    @Override
    public final void cleanGame() {
        // remove the game
    };

    @Override
    public final void playerJoin() {
        // add a player to the game
    };

    @Override
    public final void playerLeave() {
        // remove a player from the game
    };
}
