package com.gaiagauntlet.gauntlet.games.EliminationZone.managers.arena;

import java.util.concurrent.CompletableFuture;

import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state.EZState;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state.EZStates;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.components.SpawnProtectionComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.interfaces.ArenaManager;
import com.hypixel.hytale.server.core.universe.world.World;

public class EZArenaManager implements ArenaManager {
    @Override
    public CompletableFuture<Void> start(World world, GameEcs game, String sessionID) {
        var success = EZState.setState(game, EZStates.RUNNING);

        if (!success) {
            throw new IllegalStateException("EZArena unable to transition to RUNNING because state is in " + EZState.currentState(game).toString());
        }

        game.put(SpawnProtectionComponent.getComponentType(), new SpawnProtectionComponent());

        return CompletableFuture.completedFuture(null);
    }
    
}
