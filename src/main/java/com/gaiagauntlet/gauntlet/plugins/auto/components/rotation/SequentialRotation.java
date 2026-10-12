package com.gaiagauntlet.gauntlet.plugins.auto.components.rotation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.plugins.auto.components.AutoComponent;
import com.gaiagauntlet.gauntlet.plugins.auto.components.assets.SessionPreset;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import lombok.Getter;

public final class SequentialRotation extends PresetRotation {
    public static final String TYPE = "Sequential";

    public static final BuilderCodec<@NotNull SequentialRotation> CODEC = BuilderCodec
            .builder(SequentialRotation.class, SequentialRotation::new, PresetRotation.BASE_CODEC)
            .append(new KeyedCodec<>("Presets", Codec.STRING_ARRAY),
                    (rotation, v) -> rotation.presets = v,
                    rotation -> rotation.presets)
            .documentation("The presets played in order, starting over after the last")
            .addValidatorLate(() -> SessionPreset.VALIDATOR_CACHE.getArrayValidator().late())
            .add()
            .build();

    @Getter
    private String[] presets = new String[0];

    @Nullable
    @Override
    public String pick(@Nonnull GameSession session, @Nonnull AutoComponent auto) {
        var next = upNext(session, auto);
        if (next != null)
            auto.setCursor(Math.floorMod(auto.getCursor() + 1, presets.length));
        return next;
    }

    @Nullable
    @Override
    public String upNext(@Nonnull GameSession session, @Nonnull AutoComponent auto) {
        if (presets.length == 0)
            return null;
        return presets[Math.floorMod(auto.getCursor(), presets.length)];
    }
}
