package com.gaiagauntlet.gauntlet.plugins.timer.utils;

import java.util.Optional;
import java.util.function.Consumer;

import com.gaiagauntlet.gauntlet.plugins.events.events.MatchEventRegistry;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.timer.components.GameTimerComponent;
import com.gaiagauntlet.gauntlet.plugins.timer.events.TimerCompleteEvent;

public class TimerUtils {
    public static final String IMG_COUNTDOWN = "TimerHeaderUntilPortalOpens"; 
    public static final String IMG_PORTA_OPEN = "TimerHeaderGetToThePortal"; 
    public static final String IMG_STAGING = "TimerHeaderMatchStartsIn";

    public static Optional<GameTimerComponent> get(GameEcs gameStore) {
        return gameStore.get(GameTimerComponent.getComponentType());
    }

    public static String clock(float seconds) {
        int totalSeconds = Math.max(0, (int) seconds);
        return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    /** Registers a listener for a specific timer id */
    public static void registerOnComplete(String timerId, String gameId, Consumer<TimerCompleteEvent> onComplete) {
        MatchEventRegistry.register(TimerCompleteEvent.class, gameId, evt -> {
            if (evt.getTimerId() == timerId) {
                onComplete.accept(evt);
            }
        });
    }
}
