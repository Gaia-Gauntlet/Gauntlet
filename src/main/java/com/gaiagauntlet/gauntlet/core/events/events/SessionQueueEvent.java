package com.gaiagauntlet.gauntlet.core.events.events;

import java.util.Collection;

import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;

import lombok.Getter;

public class SessionQueueEvent extends GauntletEvent {
    @Getter
    private final String sessionId;
    @Getter
    private final Collection<String> newQueue;
    @Getter
    private final SessionQueueOp op = SessionQueueOp.SET;

    public SessionQueueEvent(String sessionId, Collection<String> games) {
        newQueue = games;
        this.sessionId = sessionId;
    }

    public enum SessionQueueOp {
        SET,
        REMOVE,
        APPEND
    }
}
