package com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;

/** The pacing of one closing round. The last phase repeats when there are more zones than phases. */
public final class ZonePhase {

    public static final BuilderCodec<ZonePhase> CODEC = BuilderCodec.builder(ZonePhase.class, ZonePhase::new)
            .append(new KeyedCodec<>("Name", Codec.STRING), (p, v) -> p.name = v == null ? "Phase" : v, p -> p.name).add()
            .append(new KeyedCodec<>("DurationSeconds", Codec.DOUBLE), (p, v) -> p.durationSeconds = nonNegative(v, 60), p -> p.durationSeconds)
            .documentation("Seconds the edge takes to sweep from the outer radius to the inner radius.")
            .add()
            .append(new KeyedCodec<>("HoldSeconds", Codec.DOUBLE), (p, v) -> p.holdSeconds = nonNegative(v, 0), p -> p.holdSeconds)
            .documentation("Seconds to wait after this zone seals before the next one starts closing.")
            .add()
            .append(new KeyedCodec<>("WarningSeconds", Codec.DOUBLE), (p, v) -> p.warningSeconds = nonNegative(v, 10), p -> p.warningSeconds)
            .documentation("Seconds before the seal at which the warning is announced.")
            .add()
            .build();

    private String name = "Phase";
    private double durationSeconds = 60;
    private double holdSeconds;
    private double warningSeconds = 10;

    public ZonePhase() {
    }

    public ZonePhase(@Nonnull String name, double durationSeconds, double holdSeconds, double warningSeconds) {
        this.name = name;
        this.durationSeconds = durationSeconds;
        this.holdSeconds = holdSeconds;
        this.warningSeconds = warningSeconds;
    }

    @Nonnull
    public String name() {
        return name;
    }

    public double durationSeconds() {
        return durationSeconds;
    }

    public double holdSeconds() {
        return holdSeconds;
    }

    public double warningSeconds() {
        return warningSeconds;
    }

    private static double nonNegative(Double value, double fallback) {
        return value == null ? fallback : Math.max(0.0, value);
    }
}
