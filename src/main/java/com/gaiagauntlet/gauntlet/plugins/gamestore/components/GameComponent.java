package com.gaiagauntlet.gauntlet.plugins.gamestore.components;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.utils.codec.SerializableComponent;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * simple DTO for the session. Not as much churn as an ECS since this is just a
 * mini-ecs without any form of ref system
 */
public interface GameComponent extends SerializableComponent {

    // nothing really to uh... put here... unless we wanted to implement a snapshot system for whatever reason?
    // All reads should be entirely on-thread though
}
