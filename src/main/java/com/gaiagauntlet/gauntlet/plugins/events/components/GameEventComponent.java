package com.gaiagauntlet.gauntlet.plugins.events.components;

import java.util.ArrayDeque;
import java.util.concurrent.ThreadLocalRandom;

import org.jetbrains.annotations.NotNull;

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

    private ArrayDeque<SequenceHolder> scheduledEvents;
    private SequenceHolder[] randomEvents;
    @Getter
    private double nextEventTime = Double.MAX_VALUE;
    @Getter
    private double nextRandomEventTime = 0f;

    private double randomEventInterval = 200f;

    public void hydrateFrom(GameEvents gameEvents) {
        // add the scheduledEvents
        if (gameEvents == null)
            return;

        var scheduled = gameEvents.getScheduled();
        var sortedKeys = gameEvents.getSortedKeys();
        nextEventTime = sortedKeys[0];
        for (var key : sortedKeys) {
            var evt = scheduled.get(key);
            scheduledEvents.addLast(new SequenceHolder(evt, key));
        }

        randomEventInterval = gameEvents.getIntervalSeconds();
        nextRandomEventTime = randomEventInterval + gameEvents.getFirstDrawAtSeconds();

        var entries = gameEvents.getEntries();
        randomEvents = new SequenceHolder[entries.length];

        for (int i = 0; i < entries.length; i++) {
            randomEvents[i] = new SequenceHolder(entries[i]);
        }
    }

    @Getter
    public float accumulatedGameTimeSeconds;

    public float incrementTimer(float deltaTime) {
        accumulatedGameTimeSeconds += deltaTime;
        return accumulatedGameTimeSeconds;
    }

    public float nextEvent() {
        return nextEvent();
    }

    public SequenceHolder nextRandomEvent() {
        nextRandomEventTime = accumulatedGameTimeSeconds + randomEventInterval;
        if (randomEvents.length == 0)
            return null;

        return randomEvents[ThreadLocalRandom.current().nextInt(randomEvents.length)];
    }

    public SequenceHolder popEvent() {
        if (scheduledEvents.isEmpty()) {
            nextEventTime = Double.MAX_VALUE;
            return null;
        }
        var event = scheduledEvents.pop();
        nextEventTime = !scheduledEvents.isEmpty()
                ? scheduledEvents.getFirst().getScheduledTime()
                : Double.MAX_VALUE;

        return event;
    }

    public GameEventComponent() {
        this(0);
    }

    public GameEventComponent(float initial) {
        this(initial, new ArrayDeque<SequenceHolder>(), new SequenceHolder[0]);
    }

    public GameEventComponent(float initial, ArrayDeque<SequenceHolder> scheduledEvents, SequenceHolder[] randomEvents) {
        this.accumulatedGameTimeSeconds = initial;
        this.scheduledEvents = scheduledEvents;
        this.randomEvents = randomEvents;
    }

}
