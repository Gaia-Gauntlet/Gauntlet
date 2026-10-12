package com.gaiagauntlet.gauntlet.plugins.auto.events;

import com.gaiagauntlet.gauntlet.core.events.events.GameEndEvent;
import com.gaiagauntlet.gauntlet.core.events.events.GameEvent;
import com.gaiagauntlet.gauntlet.core.events.events.PlayerGameEvent;
import com.gaiagauntlet.gauntlet.core.events.events.SessionEvent;

public class AutoSessionEvents {
    public static void onGameEnd(GameEndEvent evt) {

    }

    /**
     * Listens to player events to know when to start the next round
     */
    public static void onPlayerEvent(PlayerGameEvent evt) {
        switch (evt.getOperation()) {
            case ADD -> {
                
            }
            case REMOVE -> {

            }
        }
    }

    public static void onSessionEvent(SessionEvent evt) {
        switch (evt.getOp()) {
            case DELETE -> {

            }
            case CLEAN -> {

            }
            case SETUP -> {

            }
        }
    }

    public static void onGameEvent(GameEvent evt) {
        switch (evt.getOp()) {
            case START -> {

            }
            case STOP -> {
           
            }
        }
    }

}
