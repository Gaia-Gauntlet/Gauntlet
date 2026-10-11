package com.gaiagauntlet.gauntlet.plugins.timer.utils;

import java.util.Optional;
import java.util.function.Consumer;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.TimerDisplay;
import com.gaiagauntlet.gauntlet.plugins.events.events.MatchEventRegistry;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.timer.components.GameTimerComponent;
import com.gaiagauntlet.gauntlet.plugins.timer.events.TimerCompleteEvent;
import com.gaiagauntlet.gauntlet.plugins.timer.ui.TimerHud;

public class TimerUtils {
    public static final String IMG_COUNTDOWN = "TimerHeaderMatchStartingSoon";
    public static final String IMG_PORTAL_OPEN = "TimerHeaderGetToThePortal";
    public static final String IMG_STAGING = "TimerHeaderUntilMatchStart";
    public static final String IMG_GRACE_PERIOD = "TimerHeaderGracePeriod";

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
            if (timerId.equals(evt.getTimerId())) {
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

    public static boolean isRunning(GameEcs gameStore, String timerId) {
        return get(gameStore).filter(timer -> timer.isRunning() && timerId.equals(timer.getTimerId())).isPresent();
    }

    /** Removes the timer without completing it, clearing its displays and HUD. */
    public static void cancel(GameEcs gameStore) {
        var timer = get(gameStore).orElse(null);
        if (timer == null)
            return;
        gameStore.remove(GameTimerComponent.getComponentType());
        var world = GameStore.getContext(gameStore).getWorld();
        GauntletUtils.run(world, () -> {
            for (var poi : timer.getPois()) {
                TimerDisplay.clear(world, poi.getTransform());
            }
        });
        TimerHud.update(gameStore);
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
