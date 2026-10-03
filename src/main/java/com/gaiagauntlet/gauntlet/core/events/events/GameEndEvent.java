package com.gaiagauntlet.gauntlet.core.events.events;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;
import com.hypixel.hytale.server.core.universe.world.World;

import lombok.Getter;

/**
 * Natural game-end event. Emitted by games when they complete
 * This triggers the cleanup code for the controller
 */
public class GameEndEvent extends GauntletEvent {
    @Getter
    private final String gameId;
    @Getter
    private final String sessionId;
    /** Accessor for where the game world is from */
    @Getter
    private final World gameWorld;

    private final List<CompletableFuture<?>> deferrals = new ArrayList<>();

    public GameEndEvent(World gameWorld, String sessionId, String gameId) {
        this.gameWorld = gameWorld;
        this.gameId = gameId;
        this.sessionId = sessionId;
    }

    public void defer(CompletableFuture<?> future) {
        deferrals.add(future);
    }

    /** Should be on a timeout just in case a thread hangs for too long */
    public CompletableFuture<Void> settled() {
        return CompletableFuture.allOf(deferrals.toArray(CompletableFuture[]::new));
    }
}
