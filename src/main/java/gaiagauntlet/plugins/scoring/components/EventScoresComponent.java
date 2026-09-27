package gaiagauntlet.plugins.scoring.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import gaiagauntlet.plugins.gamestore.components.GameComponent;
import gaiagauntlet.plugins.gamestore.components.GameComponentType;
import gaiagauntlet.plugins.gamestore.components.GameComponents;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Each team's points summed over every match of the event. Saved with the game and kept until an admin resets it. */
public final class EventScoresComponent implements GameComponent {

    public static final BuilderCodec<EventScoresComponent> CODEC = BuilderCodec.builder(EventScoresComponent.class, EventScoresComponent::new)
            .append(new KeyedCodec<>("Points", new MapCodec<>(Codec.INTEGER, LinkedHashMap::new)),
                    (c, v) -> { c.points.clear(); if (v != null) { c.points.putAll(v); } },
                    c -> new LinkedHashMap<>(c.points))
            .documentation("Team id to the points it has earned this event.")
            .add()
            .build();

    public static final GameComponentType<EventScoresComponent> TYPE = GameComponents.register(
            "EventScores", EventScoresComponent.class, EventScoresComponent::new, CODEC);

    private final Map<String, Integer> points = new ConcurrentHashMap<>();

    public EventScoresComponent() {
    }

    public int pointsOf(@Nonnull String teamId) {
        return points.getOrDefault(teamId, 0);
    }

    /** Adds one match's points to each team's total. */
    public void add(@Nonnull List<StandingsComponent.Standing> standings) {
        for (var standing : standings) {
            points.merge(standing.teamId(), standing.points(), Integer::sum);
        }
    }

    public void reset() {
        points.clear();
    }
}
