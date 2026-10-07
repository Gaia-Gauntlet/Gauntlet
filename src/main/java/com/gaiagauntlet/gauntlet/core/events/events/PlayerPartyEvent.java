package com.gaiagauntlet.gauntlet.core.events.events;

import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import lombok.Getter;

public class PlayerPartyEvent extends GauntletEvent {
    @Getter
    private PlayerRef player;
    @Getter
    private String partyId;
    @Getter 
    private PlayerOp operation;

    public PlayerPartyEvent(PlayerRef player, String partyId, PlayerOp operation) {
        this.player = player;
        this.partyId = partyId;
        this.operation = operation;
    }

    public static PlayerPartyEvent Leave(PlayerRef playerRef) {
        return new PlayerPartyEvent(playerRef, null, PlayerOp.REMOVE);
    }
    public static PlayerPartyEvent Leave(PlayerRef playerRef, String partyId) {
        return new PlayerPartyEvent(playerRef, partyId, PlayerOp.REMOVE);
    }
    public static PlayerPartyEvent Join(PlayerRef playerRef, String partyId) {
        return new PlayerPartyEvent(playerRef, partyId, PlayerOp.ADD);
    }

    public enum PlayerOp {
        ADD,
        REMOVE
    }
}
