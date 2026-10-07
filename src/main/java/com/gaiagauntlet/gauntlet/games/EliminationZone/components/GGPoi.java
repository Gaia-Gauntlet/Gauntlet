package com.gaiagauntlet.gauntlet.games.EliminationZone.components;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Transform;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
public class GGPoi {
    public static final BuilderCodec<@NotNull GGPoi> CODEC = BuilderCodec.builder(GGPoi.class, GGPoi::new)
            .append(
                    new KeyedCodec<>("Coordinates", Transform.CODEC),
                    (obj, val) -> obj.transform = val,
                    GGPoi::getTransform)
            .documentation("Where the POI is and what its orientation should be.")
            .add()
            .build();

    @Getter
    private Transform transform;

    protected GGPoi() {
    }

    public GGPoi(Transform transform) {
        this.transform = transform;
    }

}