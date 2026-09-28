package com.gaiagauntlet.gauntlet.plugins.gamestore.components;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.utils.codec.SerializableComponent;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * simple DTO for the session. Not as much churn as an ECS since this is just a
 * mini-ecs without any form of ref system
 */
public interface GameComponent extends SerializableComponent {

    // opt-in persistence if wanted. Enforces safety on game crash or restart. My thought is scores / teams will implement this as a fallback for safety
    public static BuilderCodec<@NotNull GameComponent> ABSTRACT_CODEC = BuilderCodec
            .abstractBuilder(GameComponent.class)
            .build();
}
