package com.gaiagauntlet.gauntlet.plugins.events.events;

import com.gaiagauntlet.gauntlet.plugins.events.assets.GameEventConfig;
import com.hypixel.hytale.event.IEvent;

import lombok.Getter;

/** The emitted event abstract for a specific event type */
public class SequenceEvent implements IEvent<String> {
    @Getter private final GameEventConfig config;
    @Getter private final String sessionId;

    public SequenceEvent(String sessionId, GameEventConfig config) {
        this.config = config;
        this.sessionId = sessionId;
    }
}
