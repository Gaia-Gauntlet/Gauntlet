package com.gaiagauntlet.gauntlet.plugins.gamestore.components;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameComponentRegistry;
import com.gaiagauntlet.gauntlet.utils.codec.StringRegistryCodec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import lombok.Getter;

/**
 * Holds all the components for the current game 
 */
public class GameEcs {
    public static BuilderCodec<@NotNull GameEcs> CODEC = BuilderCodec
            .builder(GameEcs.class, GameEcs::new)
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
    public <T extends GameComponent> T ensure(GameComponentType<T> type, Supplier<T> supplier) {
        var comp = get(type);
        if (comp.isPresent()) return comp.get();

        var newComp = supplier.get();
        sessionComponents.put(type.getIndex(), newComp);
        return newComp;
    }

    public <T extends GameComponent> Optional<T> get(GameComponentType<T> type) {
        var sesComp = sessionComponents.get(type.getIndex());
        if (sesComp == null)
            return Optional.empty();

        return Optional.of(type.getTypeClass().cast(sesComp));
    }

    public void clear() {
        sessionComponents.clear();
    }
}
