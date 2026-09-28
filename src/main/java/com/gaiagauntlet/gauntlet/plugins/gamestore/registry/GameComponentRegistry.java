package com.gaiagauntlet.gauntlet.plugins.gamestore.registry;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponentType;
import com.gaiagauntlet.gauntlet.utils.codec.CodecRegistry;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Should only be owned by one object at a time. If a game is running
 * 
 * I wasn't entirely sure how far to put this. THe SessionComponentType is just
 * for typesafety. There may be a better way, but this is about as barebones
 * anon MiniECS you can get
 */
public class GameComponentRegistry implements CodecRegistry<GameComponent> {

    private static final Map<String, GameComponentType<?>> componentRegistry = new ConcurrentHashMap<>();

    public static <T extends GameComponent> GameComponentType<T> register(String id, Class<T> cClass,
            BuilderCodec<T> codec) {
        if (componentRegistry.containsKey(id)) {
            throw new IllegalArgumentException("Component with ID " + id + " is already registered!");
        }
        var componentType = new GameComponentType<T>(id, cClass, codec);
        componentRegistry.put(id, componentType);
        return componentType;
    }

    public <T extends GameComponent> BuilderCodec<T> getCodec(String id) {
        if (!componentRegistry.containsKey(id)) return null;

        var component = (GameComponentType<T>) componentRegistry.get(id);
        var codec = component.getCodec();

        if (codec == null) return null;

        return codec;
    }

}
