package com.gaiagauntlet.gauntlet.plugins.events.components;

import com.gaiagauntlet.gauntlet.plugins.events.assets.GameEventConfig;

import lombok.Getter;

public class SequenceHolder {
    
    @Getter private double scheduledTime = 0f;
    @Getter private String ID = "";
    @Getter private GameEventConfig gameConfig;

    public void hydrateFrom(GameEventConfig gameEvent) {
        scheduledTime = gameEvent.getAtMatchSeconds();
        ID = gameEvent.getId();
        gameConfig = gameEvent;
    }

    public void hydrateFrom(GameEventConfig gameEvent, double scheduledTime) {
        hydrateFrom(gameEvent);
        this.scheduledTime = scheduledTime;
    }

    public SequenceHolder() {}
    public SequenceHolder(GameEventConfig gameEvent) {
        hydrateFrom(gameEvent);
    }

    public SequenceHolder(GameEventConfig gameEvent, double scheduledTime) {
        hydrateFrom(gameEvent, scheduledTime);
    }

    public SequenceHolder clone() {
        return new SequenceHolder();
    }
}
