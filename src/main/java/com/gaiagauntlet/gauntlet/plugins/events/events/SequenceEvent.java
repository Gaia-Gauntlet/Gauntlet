package com.gaiagauntlet.gauntlet.plugins.events.events;

import com.gaiagauntlet.gauntlet.plugins.events.assets.GameEventConfig;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.hypixel.hytale.event.IEvent;

import lombok.Getter;

/** The emitted event abstract for a specific event type */
public class SequenceEvent implements IEvent<String> {
    @Getter private final GameEventConfig config;
    @Getter private final String sessionId;
    @Getter private final GameEcs store;

    public SequenceEvent(String sessionId, GameEventConfig config, GameEcs store) {
        this.config = config;
        this.sessionId = sessionId;
        this.store = store;
    }
}
