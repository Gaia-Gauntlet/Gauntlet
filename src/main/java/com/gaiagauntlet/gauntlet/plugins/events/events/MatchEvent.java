package com.gaiagauntlet.gauntlet.plugins.events.events;

import com.hypixel.hytale.event.IEvent;

import lombok.Getter;

public abstract class MatchEvent implements IEvent<String> {
    @Getter
    protected String sessionId;

    public MatchEvent(String sessionId) {
        this.sessionId = sessionId;
    }
    
}
