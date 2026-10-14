package com.gaiagauntlet.gauntlet.core.events.events;

import java.util.Collection;
import java.util.List;

import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import lombok.Getter;

public class PlayerReviveEvent extends GauntletEvent {
    @Getter private final String sessionId;
    @Getter private final List<PlayerRef> players;

    public PlayerReviveEvent(String sessionId, Collection<PlayerRef> players) {
        this.sessionId = sessionId;
        this.players = List.copyOf(players);
    }

    @Override
    public String toString() {
        return "PlayerReviveEvent[" + sessionId + ": " + players.stream().map(PlayerRef::getUsername).toList() + "]";
    }
}
