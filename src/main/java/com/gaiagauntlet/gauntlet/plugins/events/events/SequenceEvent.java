package com.gaiagauntlet.gauntlet.plugins.events.events;

import com.gaiagauntlet.gauntlet.plugins.events.assets.GameEventConfig;

import lombok.Getter;

/** The emitted event abstract for a specific event type */
public abstract class SequenceEvent<T extends GameEventConfig> extends MatchEvent {
    @Getter private final T config;

    public SequenceEvent(String sessionId, T config) {
        super(sessionId);
        this.config = config;
    }
}
