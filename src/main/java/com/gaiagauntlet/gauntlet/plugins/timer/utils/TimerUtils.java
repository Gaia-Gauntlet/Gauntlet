package com.gaiagauntlet.gauntlet.plugins.timer.utils;

import java.util.Optional;

import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.timer.components.GameTimerComponent;

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
}
