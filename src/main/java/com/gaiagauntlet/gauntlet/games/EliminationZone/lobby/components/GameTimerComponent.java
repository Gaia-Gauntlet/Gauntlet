package com.gaiagauntlet.gauntlet.games.EliminationZone.lobby.components;

import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponentType;
import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameComponentRegistry;
import lombok.Getter;
import lombok.Setter;

public class GameTimerComponent implements GameComponent {
    public static String ID = "GameTimer";
    @Getter @Setter public static GameComponentType<GameTimerComponent> componentType =
        GameComponentRegistry.register(ID, GameTimerComponent.class);

    @Getter private double countdownRemaining = 0;
    @Getter @Setter double lastUpdateTime = 0; // last time the timer was updated
    @Getter @Setter private boolean isRunning = false;
    @Getter @Setter Runnable onComplete;
    public GameTimerComponent() {}

    public void startTimer(double time) {
        startTimer(time, null);
    }
    public void startTimer(double time, Runnable onComplete) {
        if (time <= 0) {
            reset();
            return;
        }
        isRunning = true;
        if (onComplete != null) {
            this.onComplete = onComplete;
        }
        countdownRemaining = time;
    }

    public GameTimerComponent clone() {
        var timer = new GameTimerComponent();
        timer.countdownRemaining = countdownRemaining;
        timer.isRunning = isRunning;
        return timer;
    }

    /**
     * 
     * @param dt
     * @return true if it is still running
     */
    public boolean progressTimer(float dt) {
        countdownRemaining -= dt;
        if (countdownRemaining <= 0) {
            reset();
            return false;
        }
        return true;
    }

    public void setTimer(float dt) {
        countdownRemaining = Math.max(dt, 1); // let the ticking system finish it out
    }

    public void pause() {
        isRunning = false;
    }
    public void play() {
        isRunning = true;
    }

    public void reset() {
        countdownRemaining = 0;
        lastUpdateTime = 0;
        isRunning = false;
    }
    public void complete() {
        countdownRemaining = 0;
        lastUpdateTime = 1; // set to a larger number to ensure timer updates
    }

    /** Should only be called when the timer is synced with the state (i.e. every second) */
    public void sync() {
        lastUpdateTime = countdownRemaining;
    }
}
