package com.gaiagauntlet.gauntlet.core.events.events;

import java.util.Collection;

import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;
import com.gaiagauntlet.gauntlet.core.events.events.SessionQueueEvent.SessionQueueOp;

import lombok.Getter;

public class SessionQueueEvent extends GauntletEvent {
    @Getter
    private final String sessionId;
    @Getter
    private final Collection<String> newQueue;
    @Getter
    private final SessionQueueOp op;

    public SessionQueueEvent(SessionQueueOp op, String sessionId, Collection<String> games) {
        newQueue = games;
        this.sessionId = sessionId;
        this.op = op;
    }

    @Override
    public String toString() {
        return "SessionQueueEvent[" + sessionId + ": " + op + " " + newQueue + "]";
    }

    public enum SessionQueueOp {
        SET,
        REMOVE,
        APPEND
    }
}
