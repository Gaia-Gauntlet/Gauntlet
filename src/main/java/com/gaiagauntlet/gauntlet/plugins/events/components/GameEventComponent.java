package com.gaiagauntlet.gauntlet.plugins.events.components;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.plugins.events.assets.GameEventConfig;
import com.gaiagauntlet.gauntlet.plugins.events.assets.GameEvents;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponentType;
import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameComponentRegistry;

import lombok.Getter;

public class GameEventComponent implements GameComponent {
    public static final String ID = "GameEventComponent";
    @Getter
    private static final GameComponentType<@NotNull GameEventComponent> componentType = GameComponentRegistry.register(
            ID,
            GameEventComponent.class);

    private final List<SequenceHolder> scheduledEvents = new ArrayList<>();
    private SequenceHolder[] randomEvents = new SequenceHolder[0];
    @Getter
    private double nextRandomEventTime = Double.MAX_VALUE;
    private double randomEventInterval;
    @Getter
    private float accumulatedGameTimeSeconds;
    @Getter
    private boolean paused;

    public void hydrateFrom(@Nullable GameEvents gameEvents) {
        scheduledEvents.clear();
        randomEvents = new SequenceHolder[0];
        accumulatedGameTimeSeconds = 0;
        paused = false;
        nextRandomEventTime = Double.MAX_VALUE;
        if (gameEvents == null) {
            return;
        }

        var scheduled = gameEvents.getScheduled();
        if (scheduled != null) {
            for (var key : gameEvents.getSortedKeys()) {
                scheduledEvents.add(new SequenceHolder(scheduled.get(key), key));
            }
        }

        var entries = gameEvents.getEntries();
        if (entries != null && entries.length > 0) {
            randomEvents = new SequenceHolder[entries.length];
            for (int i = 0; i < entries.length; i++) {
                randomEvents[i] = new SequenceHolder(entries[i]);
            }
            randomEventInterval = gameEvents.getIntervalSeconds();
            nextRandomEventTime = randomEventInterval + gameEvents.getFirstDrawAtSeconds();
        }
    }

    public float incrementTimer(float deltaTime) {
        if (!paused) {
            accumulatedGameTimeSeconds += deltaTime;
        }
        return accumulatedGameTimeSeconds;
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    @Nullable
    public SequenceHolder popDue() {
        if (scheduledEvents.isEmpty() || scheduledEvents.getFirst().getScheduledTime() > accumulatedGameTimeSeconds) {
            return null;
        }
        return scheduledEvents.removeFirst();
    }

    @Nullable
    public SequenceHolder popNext() {
        return scheduledEvents.isEmpty() ? null : scheduledEvents.removeFirst();
    }

    @Nullable
    public SequenceHolder drawRandomIfDue() {
        if (accumulatedGameTimeSeconds < nextRandomEventTime || randomEvents.length == 0) {
            return null;
        }
        nextRandomEventTime = accumulatedGameTimeSeconds + randomEventInterval;
        return randomEvents[ThreadLocalRandom.current().nextInt(randomEvents.length)];
    }

    public void queue(@Nonnull GameEventConfig config, double inSeconds) {
        scheduledEvents.add(new SequenceHolder(config, accumulatedGameTimeSeconds + Math.max(0.0, inSeconds)));
        scheduledEvents.sort(Comparator.comparingDouble(SequenceHolder::getScheduledTime));
    }

    public void delay(double seconds) {
        scheduledEvents.forEach(event -> event.delay(seconds));
        if (nextRandomEventTime != Double.MAX_VALUE) {
            nextRandomEventTime += seconds;
        }
    }

    public boolean cancel(int index, double scheduledTime) {
        if (index < 0 || index >= scheduledEvents.size()
                || Double.compare(scheduledEvents.get(index).getScheduledTime(), scheduledTime) != 0) {
            return false;
        }
        scheduledEvents.remove(index);
        return true;
    }

    @Nonnull
    public List<SequenceHolder> scheduled() {
        return List.copyOf(scheduledEvents);
    }
}
