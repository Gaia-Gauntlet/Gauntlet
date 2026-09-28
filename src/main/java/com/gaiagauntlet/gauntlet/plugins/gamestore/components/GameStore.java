package com.gaiagauntlet.gauntlet.plugins.gamestore.components;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameComponentRegistry;
import com.gaiagauntlet.gauntlet.utils.codec.StringRegistryCodec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import lombok.Getter;

/**
 * Holds all the components for the current game 
 */
public class GameStore {
    public static BuilderCodec<@NotNull GameStore> CODEC = BuilderCodec
            .builder(GameStore.class, GameStore::new)
            .append(new KeyedCodec<>("Components",
                    new StringRegistryCodec<>(new GameComponentRegistry(), ConcurrentHashMap::new)),
                    (holder, map) -> holder.sessionComponents = map,
                    holder -> holder.sessionComponents)
            .add()
            .build();

    

    @Getter
    private Map<String, GameComponent> sessionComponents = new ConcurrentHashMap<>();

    public <T extends GameComponent> void put(GameComponentType<T> type, T component) {
        sessionComponents.put(type.getIndex(), component);
    }

    public <T extends GameComponent> Optional<T> get(GameComponentType<T> type) {
        var sesComp = sessionComponents.get(type.getIndex());
        if (sesComp == null)
            return Optional.empty();

        return Optional.of(type.getTypeClass().cast(sesComp));
    }
}
