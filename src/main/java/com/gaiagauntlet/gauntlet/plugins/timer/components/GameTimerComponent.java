package com.gaiagauntlet.gauntlet.plugins.timer.components;

import java.util.ArrayList;
import java.util.List;

import com.gaiagauntlet.gauntlet.games.EliminationZone.components.GGPoi;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponentType;
import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameComponentRegistry;
import lombok.Getter;
import lombok.Setter;

public class GameTimerComponent implements GameComponent {
    public static String ID = "GameTimer";
    @Getter
    @Setter
    public static GameComponentType<GameTimerComponent> componentType = GameComponentRegistry.register(ID,
            GameTimerComponent.class);

    @Getter
    private float countdownRemaining = 0;
    @Getter
    @Setter
    double lastUpdateTime = 0; // last time the timer was updated
    @Getter
    @Setter
    private boolean isRunning = false;
    @Getter
    private final List<GGPoi> pois = new ArrayList<>();
    @Getter
    private final String timerId;
    @Getter
    private String timerImageId = null;

    public GameTimerComponent(String timerId) {
        this(timerId, null);
    }

    public GameTimerComponent(String timerId, String timerImageId) {
        this.timerId = timerId;
        this.timerImageId = timerImageId;
    }

    public GameTimerComponent(GameTimerComponent other) {
        this.timerId = other.timerId;
        this.countdownRemaining = other.countdownRemaining;
        this.isRunning = other.isRunning;
        this.timerImageId = other.timerImageId;
        this.pois.addAll(other.pois);
    }

    public void startTimer(float time) {
        if (time <= 0) {
            reset();
            return;
        }
        isRunning = true;
        countdownRemaining = time;
    }

    public void addPoi(GGPoi poi) {
        pois.add(poi);
    }

    public GameTimerComponent clone() {
        return new GameTimerComponent(this);
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

    /**
     * Should only be called when the timer is synced with the state (i.e. every
     * second)
     */
    public void sync() {
        lastUpdateTime = countdownRemaining;
    }
}
