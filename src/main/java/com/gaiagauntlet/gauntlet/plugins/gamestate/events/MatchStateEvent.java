package com.gaiagauntlet.gauntlet.plugins.gamestate.events;
import com.gaiagauntlet.gauntlet.plugins.events.events.MatchEvent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;

import lombok.Getter;

/**
 * A session's match moved to a new phase. Dispatched after the change is applied, so listeners read
 * the new state. Games react here to open the portal, move players, and so on.
 */
public class MatchStateEvent extends MatchEvent {
    @Getter private final String from;
    @Getter private final String to;
    @Getter private final GameEcs game;

    public MatchStateEvent(String sessionId, GameEcs game, String from, String to) {
        super(sessionId);
        this.from = from;
        this.to = to;
        this.game = game;
    }

    @Override
    public String toString() {
        return "MatchStateEvent[" + sessionId + ": " + from + " -> " + to + "]";
    }
}
