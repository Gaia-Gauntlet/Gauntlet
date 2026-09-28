package com.gaiagauntlet.gauntlet.core.session.components;

import javax.annotation.Nullable;

import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * simple DTO for the session. Not as much churn as an ECS since this is just a
 * mini-ecs without any form of ref system
 */
public class SessionComponentType<T extends SessionComponent> {

    /**
     * The type class that this component type is associated with.
     */
    private Class<T> tClass;

    @Nullable 
    private final BuilderCodec<T> codec;

    private String id;

    public SessionComponentType(String id, Class<T> tClass) {
        this(id, tClass, null);
    }
    public SessionComponentType(String id, Class<T> tClass, BuilderCodec<T> codec) {
        this.tClass = tClass;
        this.id = id;
        this.codec = codec;
    }

    public Class<T> getTypeClass() {
        return tClass;
    }

    public BuilderCodec<T> getCodec() {
        return codec;
    }

    /**
     * Get the index string for this registered component type.
     */
    public String getIndex() {
        return id;
    }

}
