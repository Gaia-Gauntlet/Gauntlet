package com.gaiagauntlet.gauntlet.plugins.timer.events;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.plugins.events.events.MatchEvent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;

import lombok.Getter;

public class TimerCompleteEvent extends MatchEvent {

    @Getter 
    private String timerId;
    
    @Getter 
    private GameEcs gameStore;

    public TimerCompleteEvent(String sessionId, GameEcs gameStore, String timerId) {
        super(sessionId);
        this.gameStore = gameStore;
        this.timerId = timerId;
    }

    @NotNull 
    public static TimerCompleteEvent of(GameEcs game, String timerId) {
        return new TimerCompleteEvent(game.getSessionId(), game, timerId);
    }

}
