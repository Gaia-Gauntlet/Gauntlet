package com.gaiagauntlet.gauntlet.core.session.registry;


import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.gaiagauntlet.gauntlet.core.codec.CodecRegistry;
import com.gaiagauntlet.gauntlet.core.session.components.SessionComponent;
import com.gaiagauntlet.gauntlet.core.session.components.SessionComponentType;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Should only be owned by one object at a time. If a game is running
 * 
 * I wasn't entirely sure how far to put this. THe SessionComponentType is just
 * for typesafety. There may be a better way, but this is about as barebones
 * anon MiniECS you can get
 */
public class SessionRegistry implements CodecRegistry<SessionComponent> {

    private static final Map<String, SessionComponentType<?>> componentRegistry = new ConcurrentHashMap<>();

    public static <T extends SessionComponent> SessionComponentType<T> register(String id, Class<T> cClass,
            BuilderCodec<T> codec) {
        if (componentRegistry.containsKey(id)) {
            throw new IllegalArgumentException("Component with ID " + id + " is already registered!");
        }
        var componentType = new SessionComponentType<T>(id, cClass, codec);
        componentRegistry.put(id, componentType);
        return componentType;
    }

    public <T extends SessionComponent> BuilderCodec<T> getCodec(String id) {
        if (!componentRegistry.containsKey(id)) return null;

        var component = (SessionComponentType<T>) componentRegistry.get(id);
        var codec = component.getCodec();

        if (codec == null) return null;

        return codec;
    }

}
