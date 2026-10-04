package com.gaiagauntlet.gauntlet.plugins.gamestore.components;

import javax.annotation.Nullable;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import lombok.Getter;

/**
 * simple DTO for the session. Not as much churn as an ECS since this is just a
 * mini-ecs without any form of ref system
 */
public class GameComponentType<T extends GameComponent> {

    /**
     * The type class that this component type is associated with.
     */
    private Class<T> tClass;

    @Getter
    @Nullable
    private final BuilderCodec<T> codec;

    private String id;

    public GameComponentType(String id, Class<T> tClass) {
        this(id, tClass, null);
    }
    public GameComponentType(String id, Class<T> tClass, BuilderCodec<T> codec) {
        this.tClass = tClass;
        this.id = id;
        this.codec = codec;
    }

    public Class<T> getTypeClass() {
        return tClass;
    }

    /**
     * Get the index string for this registered component type.
     */
    public String getIndex() {
        return id;
    }

}
