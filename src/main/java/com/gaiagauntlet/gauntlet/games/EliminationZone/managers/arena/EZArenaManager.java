package com.gaiagauntlet.gauntlet.games.EliminationZone.managers.arena;

import java.util.concurrent.CompletableFuture;

import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.interfaces.ArenaManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

public class EZArenaManager implements ArenaManager {

    @Override
    public CompletableFuture<Void> begin(World world) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'begin'");
    }

    @Override
    public void onPlayerJoin(World world, PlayerRef player) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onPlayerJoin'");
    }

    @Override
    public void onPlayerLeave(World world, PlayerRef player) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onPlayerLeave'");
    }

    @Override
    public CompletableFuture<Void> terminate(World world) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'terminate'");
    }
    
}
