package com.gaiagauntlet.gauntlet.core.events.events;

import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;

import lombok.Getter;

public class NewSessionEvent extends GauntletEvent {
    @Getter
    private GameSession newSession;

    public NewSessionEvent(GameSession newSession) {
        this.newSession = newSession;
    }

}
