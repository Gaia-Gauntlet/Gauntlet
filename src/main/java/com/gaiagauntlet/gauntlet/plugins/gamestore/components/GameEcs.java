package com.gaiagauntlet.gauntlet.plugins.gamestore.components;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameComponentRegistry;
import com.gaiagauntlet.gauntlet.utils.codec.StringRegistryCodec;
import com.hypixel.hytale.codec.Codec;
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
            .append(new KeyedCodec<>("SessionId",
                    Codec.STRING),
                    (holder, v) -> holder.sessionId = v,
                    holder -> holder.sessionId)
            .add()
            .build();

    @Getter
    private Map<String, GameComponent> sessionComponents = new ConcurrentHashMap<>();

    @Getter
    private String sessionId;

    private GameEcs() {

    }
    public GameEcs(String sessionId) {
        this.sessionId = sessionId;
    }

    public <T extends GameComponent> void put(GameComponentType<T> type, T component) {
        sessionComponents.put(type.getIndex(), component);
    }

    public <T extends GameComponent> T ensure(GameComponentType<T> type, Supplier<T> supplier) {
        var comp = get(type);
        if (comp.isPresent())
            return comp.get();

        var newComp = supplier.get();
        sessionComponents.put(type.getIndex(), newComp);
        return newComp;
    }

    @Nonnull
    public <T extends GameComponent> Optional<T> get(GameComponentType<T> type) {
        if (type == null) {
            throw new IllegalArgumentException(
                    "Component was not registered properly! Unable to retrieve from GameECS store");
        }
        var sesComp = sessionComponents.get(type.getIndex());
        if (sesComp == null)
            return Optional.empty();

        return Optional.of(type.getTypeClass().cast(sesComp));
    }

    public <T extends GameComponent> void remove(GameComponentType<T> type) {
        sessionComponents.remove(type.getIndex());
    }

    public void clear() {
        sessionComponents.clear();
    }
}
