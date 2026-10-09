package com.gaiagauntlet.gauntlet.plugins.events.events;

import com.gaiagauntlet.gauntlet.plugins.events.components.GameEventConfig;
import lombok.Getter;

public abstract class ConfiguredGameEvent<T extends GameEventConfig> extends MatchEvent {
    @Getter private final T config;

    public ConfiguredGameEvent(String sessionId, T config) {
        super(sessionId);
        this.config = config;
    }
}
