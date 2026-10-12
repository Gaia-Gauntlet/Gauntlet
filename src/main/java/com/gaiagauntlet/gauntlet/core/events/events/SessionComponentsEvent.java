package com.gaiagauntlet.gauntlet.core.events.events;

import java.util.Map;

import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;
import com.gaiagauntlet.gauntlet.core.session.components.SessionComponent;

import lombok.Getter;

/** Puts components onto a session between games, replacing the ones it has under the same ids */
public class SessionComponentsEvent extends GauntletEvent {
    @Getter private final String sessionId;
    @Getter private final Map<String, SessionComponent> components;

    public SessionComponentsEvent(String sessionId, Map<String, SessionComponent> components) {
        this.sessionId = sessionId;
        this.components = components;
    }

    @Override
    public String toString() {
        return "SessionComponentsEvent[" + sessionId + ": " + components.keySet() + "]";
    }
}
