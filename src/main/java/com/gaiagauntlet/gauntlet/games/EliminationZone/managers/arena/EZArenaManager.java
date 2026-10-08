package com.gaiagauntlet.gauntlet.games.EliminationZone.managers.arena;

import java.util.concurrent.CompletableFuture;

import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.interfaces.ArenaManager;
import com.hypixel.hytale.server.core.universe.world.World;

public class EZArenaManager implements ArenaManager {
    @Override
    public CompletableFuture<Void> begin(World world) {
        GaiaLog.atError().log("begin is not implemented for EZGameController!");
        return CompletableFuture.completedFuture(null);
    }
    
}
