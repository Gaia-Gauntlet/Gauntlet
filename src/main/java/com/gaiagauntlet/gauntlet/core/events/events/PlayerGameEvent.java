package com.gaiagauntlet.gauntlet.core.events.events;

import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import lombok.Getter;

public class PlayerGameEvent extends GauntletEvent {
    @Getter
    private PlayerRef player;
    @Getter
    private String sessionId;
    @Getter 
    private PlayerOp operation;

    public PlayerGameEvent(PlayerRef player, String sessionId, PlayerOp operation) {
        this.player = player;
        this.sessionId = sessionId;
        this.operation = operation;
    }

    public static PlayerGameEvent Add(PlayerRef player, String sessionId) {
        return new PlayerGameEvent(player, sessionId, PlayerOp.ADD);
    }
    public static PlayerGameEvent Remove(PlayerRef player, String sessionId) {
        return new PlayerGameEvent(player, sessionId, PlayerOp.REMOVE);
    }

    public enum PlayerOp {
        ADD,
        REMOVE
    }
}
