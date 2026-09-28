package com.gaiagauntlet.gauntlet.core.session.components;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.core.session.registry.SessionRegistry;
import com.gaiagauntlet.gauntlet.utils.codec.StringRegistryCodec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import lombok.Getter;
import lombok.Setter;

/**
 * Holds the relevant information regarding an active session.
 * 
 * Does NOT hold specific information like players active, rather, only the information necessary to identify a specific game
 */
public class GameSession {
    public static BuilderCodec<@NotNull GameSession> CODEC = BuilderCodec
            .builder(GameSession.class, GameSession::new)
            .append(new KeyedCodec<>("Components",
                    new StringRegistryCodec<>(new SessionRegistry(), ConcurrentHashMap::new)),
                    (holder, map) -> holder.sessionComponents = map,
                    holder -> holder.sessionComponents)
            .add()
            .build();

    

    @Getter
    private Map<String, SessionComponent> sessionComponents = new ConcurrentHashMap<>();

    public <T extends SessionComponent> void put(SessionComponentType<T> type, T component) {
        sessionComponents.put(type.getIndex(), component);
    }

    public <T extends SessionComponent> Optional<T> get(SessionComponentType<T> type) {
        var sesComp = sessionComponents.get(type.getIndex());
        if (sesComp == null)
            return Optional.empty();

        return Optional.of(type.getTypeClass().cast(sesComp));
    }

}
