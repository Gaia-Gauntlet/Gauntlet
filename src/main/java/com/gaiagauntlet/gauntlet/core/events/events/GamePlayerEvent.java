package com.gaiagauntlet.gauntlet.core.events.events;

import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import lombok.Getter;

public class GamePlayerEvent extends GauntletEvent {
    @Getter
    private PlayerRef player;
    @Getter
    private String sessionId;
    @Getter 
    private PlayerOp operation;

    public GamePlayerEvent(PlayerRef player, String sessionId, PlayerOp operation) {
        this.player = player;
        this.sessionId = sessionId;
        this.operation = operation;
    }

    public enum PlayerOp {
        ADD,
        REMOVE
    }
}
