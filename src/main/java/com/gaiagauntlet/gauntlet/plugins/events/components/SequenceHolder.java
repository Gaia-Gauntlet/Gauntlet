package com.gaiagauntlet.gauntlet.plugins.events.components;

import com.gaiagauntlet.gauntlet.plugins.events.assets.GameEventConfig;

import lombok.Getter;

public class SequenceHolder {

    @Getter private double scheduledTime;
    @Getter private final GameEventConfig gameConfig;

    public SequenceHolder(GameEventConfig gameEvent) {
        this(gameEvent, gameEvent.getAtMatchSeconds());
    }

    public SequenceHolder(GameEventConfig gameEvent, double scheduledTime) {
        this.gameConfig = gameEvent;
        this.scheduledTime = scheduledTime;
    }

    public String getID() {
        return gameConfig.getId();
    }

    public void delay(double seconds) {
        scheduledTime += seconds;
    }
}
