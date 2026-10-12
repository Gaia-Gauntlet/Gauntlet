package com.gaiagauntlet.gauntlet.plugins.auto.components.rotation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.plugins.auto.components.AutoComponent;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;

/**
 * How an auto session chooses the SessionPreset it plays next once its queue is empty. Both
 * hooks run on the hub thread while the session is between games
 */
public abstract class PresetRotation {
    public static final CodecMapCodec<@NotNull PresetRotation> CODEC = new CodecMapCodec<>("Type");
    public static final BuilderCodec<@NotNull PresetRotation> BASE_CODEC = BuilderCodec
            .abstractBuilder(PresetRotation.class)
            .build();

    /** The intermission before the next game started */
    public void open(@Nonnull GameSession session, @Nonnull AutoComponent auto) {
    }

    /** The intermission ended. Returns the preset to apply, or null to start nothing */
    @Nullable
    public abstract String pick(@Nonnull GameSession session, @Nonnull AutoComponent auto);

    /** The preset the rotation would pick now, for display */
    @Nullable
    public abstract String upNext(@Nonnull GameSession session, @Nonnull AutoComponent auto);

    /** Whether a session starting from idle waits a full intermission instead of the short gather */
    public boolean holdsIntermission() {
        return false;
    }
}
