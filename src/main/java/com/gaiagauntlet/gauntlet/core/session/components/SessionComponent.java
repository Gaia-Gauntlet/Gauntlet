package com.gaiagauntlet.gauntlet.core.session.components;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.utils.codec.SerializableComponent;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * simple DTO for the session. Not as much churn as an ECS since this is just a
 * mini-ecs without any form of ref system
 */
public interface SessionComponent extends SerializableComponent {

    // wasn't anything to uh.... put here.... this is mostly just for typesafety lol
}
