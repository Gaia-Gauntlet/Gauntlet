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
    /** Registers a listener for a specific timer id */
    public static void registerOnComplete(String gameId, Consumer<TimerCompleteEvent> onComplete) {
        MatchEventRegistry.register(TimerCompleteEvent.class, gameId, onComplete::accept);
    }

    /** Sets up and starts a timer with a given component */
    public static void start(GameEcs gameStore, float duration, GameTimerComponent timerComponent) {
        gameStore.put(GameTimerComponent.getComponentType(), timerComponent);
        timerComponent.startTimer(duration);
    }

    /** A thin timer that doesn't render anywhere but emits an event on end. */
    public static void start(String timerId, GameEcs gameStore, float duration) {
        var timerComp = new GameTimerComponent(timerId);
        start(gameStore, duration, timerComp);
    }

    public static void stop(GameEcs gameStore) {
        gameStore.get(GameTimerComponent.getComponentType()).ifPresent(timerComp -> {
            timerComp.pause();
        });
    }

    public static void play(GameEcs gameStore) {
        gameStore.get(GameTimerComponent.getComponentType()).ifPresent(timerComp -> {
            timerComp.play();
        });
    }
}
