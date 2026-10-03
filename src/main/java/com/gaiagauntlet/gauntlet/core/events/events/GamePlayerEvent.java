package com.gaiagauntlet.gauntlet.core.events.events;

import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import lombok.Getter;

public class GamePlayerEvent extends GauntletEvent {
    @Getter
    private PlayerRef player;
    @Getter
    private String sessionId;

    public GamePlayerEvent(PlayerRef player, String sessionId) {
        this.player = player;
        this.sessionId = sessionId;
    }

    public enum PlayerOp {
        ADD,
        REMOVE
    }
}
