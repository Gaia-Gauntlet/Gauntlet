package com.gaiagauntlet.gauntlet.core.events.events;

import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;
import com.gaiagauntlet.gauntlet.core.session.constants.SessionState;

import lombok.Getter;

/**
 * A session moved to a new state. Dispatched after the change is applied and from inside the
 * transition, so listeners must not change the session in the same task.
 */
public class SessionStateEvent extends GauntletEvent {
    @Getter private final String sessionId;
    @Getter private final SessionState from;
    @Getter private final SessionState to;
    @Getter @Nullable private final String gameId;
    @Getter private final int generation;

    public SessionStateEvent(String sessionId, SessionState from, SessionState to, @Nullable String gameId,
            int generation) {
        this.sessionId = sessionId;
        this.from = from;
        this.to = to;
        this.gameId = gameId;
        this.generation = generation;
    }

    @Override
    public String toString() {
        return "SessionStateEvent[" + sessionId + ": " + from + " -> " + to + " (" + gameId + " #" + generation + ")]";
    }
}
