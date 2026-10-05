package com.gaiagauntlet.gauntlet.core.events.events;

import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;

import lombok.Getter;

public class SessionEvent extends GauntletEvent {
    @Getter
    private SessionOperation op;
    @Getter
    private String sessionId;

    public SessionEvent(SessionOperation op, String id) {
        this.op = op;
        this.sessionId = id;
    }

    public enum SessionOperation {
        /** Starts the session */
        SETUP,
        /** Cancels the current game in the session */
        CLEAN,
        /** Deletes the session */
        DELETE
    }
}
