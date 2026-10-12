package com.gaiagauntlet.gauntlet.plugins.auto.components;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.core.session.components.SessionComponent;
import com.gaiagauntlet.gauntlet.core.session.components.SessionComponentType;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import lombok.Getter;
import lombok.Setter;

/**
 * How Auto runs one session: which AutoSession it follows and whether an admin paused it.
 * Changed in place, since the session refuses new components while its game runs
 */
public final class AutoComponent implements SessionComponent {
    public static final String ID = "AutoComponent";

    public static final BuilderCodec<@NotNull AutoComponent> CODEC = BuilderCodec
            .builder(AutoComponent.class, AutoComponent::new)
            .append(new KeyedCodec<>("ConfigId", Codec.STRING),
                    (auto, v) -> auto.configId = v,
                    auto -> auto.configId)
            .add()
            .append(new KeyedCodec<>("Paused", Codec.BOOLEAN),
                    (auto, v) -> auto.paused = v != null && v,
                    auto -> auto.paused)
            .add()
            .append(new KeyedCodec<>("Cursor", Codec.INTEGER),
                    (auto, v) -> auto.cursor = v == null ? 0 : v,
                    auto -> auto.cursor)
            .add()
            .append(new KeyedCodec<>("CurrentPreset", Codec.STRING),
                    (auto, v) -> auto.currentPreset = v,
                    auto -> auto.currentPreset)
            .add()
            .append(new KeyedCodec<>("NextPreset", Codec.STRING),
                    (auto, v) -> auto.nextPreset = v,
                    auto -> auto.nextPreset)
            .add()
            .build();

    @Getter @Setter private static SessionComponentType<@NotNull AutoComponent> componentType;

    /** The AutoSession to follow, or null to follow the one named after the session */
    @Getter @Setter @Nullable private volatile String configId;
    @Getter @Setter private volatile boolean paused;
    /** Failures since the last game reached RUNNING. Not saved, a restart starts the count over */
    @Getter @Setter private volatile int failures;
    /** How far a sequential rotation has gone through its presets */
    @Getter @Setter private volatile int cursor;
    /** The preset applied for the current or last game */
    @Getter @Setter @Nullable private volatile String currentPreset;
    /** A preset an admin picked to play next, taking the place of the rotation once */
    @Getter @Setter @Nullable private volatile String nextPreset;
}
