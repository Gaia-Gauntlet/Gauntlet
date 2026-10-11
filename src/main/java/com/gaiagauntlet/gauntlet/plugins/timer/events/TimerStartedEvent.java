package com.gaiagauntlet.gauntlet.plugins.timer.events;

import com.gaiagauntlet.gauntlet.plugins.events.events.MatchEvent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;

import lombok.Getter;

public class TimerStartedEvent extends MatchEvent {

    @Getter
    private final String timerId;
    @Getter
    private final float duration;

    @Getter
    private final GameEcs gameStore;

    public TimerStartedEvent(String sessionId, GameEcs gameStore, String timerId, float duration) {
        super(sessionId);
        this.gameStore = gameStore;
        this.timerId = timerId;
        this.duration = duration;
    }

    public static TimerStartedEvent of(GameEcs game, String timerId, float duration) {
        return new TimerStartedEvent(game.getSessionId(), game, timerId, duration);
    }

}
