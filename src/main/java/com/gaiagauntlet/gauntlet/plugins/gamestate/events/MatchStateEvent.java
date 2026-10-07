package com.gaiagauntlet.gauntlet.plugins.gamestate.events;

import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;
import com.gaiagauntlet.gauntlet.plugins.gamestate.constants.MatchState;

import lombok.Getter;

/**
 * A session's match moved to a new phase. Dispatched after the change is applied, so listeners read
 * the new state. Games react here to open the portal, move players, and so on.
 */
public class MatchStateEvent extends GauntletEvent {
    @Getter
    private final String sessionId;
    @Getter
    private final MatchState from;
    @Getter
    private final MatchState to;

    public MatchStateEvent(String sessionId, MatchState from, MatchState to) {
        this.sessionId = sessionId;
        this.from = from;
        this.to = to;
    }

    @Override
    public String toString() {
        return "MatchStateEvent[" + sessionId + ": " + from + " -> " + to + "]";
    }
}
