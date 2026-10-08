package com.gaiagauntlet.gauntlet.core.events.events;

import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;

import lombok.Getter;

public class GameEvent extends GauntletEvent {
    @Getter
    private GameOperation op;

    @Override
    public String toString() {
        return "GameEvent[" + op + "]";
    }

    public enum GameOperation {
        START,
        STOP
    }
}
