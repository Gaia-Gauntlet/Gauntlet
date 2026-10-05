package com.gaiagauntlet.gauntlet.core.events.events;

import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import lombok.Getter;

public class UniversePlayerEvent extends GauntletEvent {
    @Getter private PlayerRef player;

    public UniversePlayerEvent(PlayerRef player) {
        this.player = player;
    }

    public enum PlayerOp {
        JOIN,
        LEAVE
    }
}
