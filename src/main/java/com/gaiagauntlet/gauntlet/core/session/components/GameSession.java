package com.gaiagauntlet.gauntlet.core.session.components;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.core.session.constants.SessionState;
import com.gaiagauntlet.gauntlet.core.session.registry.SessionRegistry;
import com.gaiagauntlet.gauntlet.utils.codec.StringRegistryCodec;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;

import lombok.Getter;
import lombok.Setter;

/**
 * Holds the relevant information regarding an active session.
 * 
 * Does NOT hold specific information like players active, rather, only the
 * information necessary to identify a specific game
 */
public class GameSession {
    public static BuilderCodec<@NotNull GameSession> CODEC = BuilderCodec
            .builder(GameSession.class, GameSession::new)
            .append(new KeyedCodec<>("Components",
                    new StringRegistryCodec<>(new SessionRegistry(), ConcurrentHashMap::new)),
                    (holder, map) -> holder.sessionComponents = map,
                    holder -> holder.sessionComponents)
            .add()
            .append(new KeyedCodec<>("Sequence", Codec.STRING_ARRAY),
                    (holder, v) -> holder.gameSequence = v,
                    holder -> holder.gameSequence)
            .add()
            .append(new KeyedCodec<>("CurrentGame", Codec.STRING),
                    (holder, v) -> holder.currentGame = v,
                    holder -> holder.currentGame)
            .add()
            .append(new KeyedCodec<>("State", new EnumCodec<>(SessionState.class)),
                    (holder, v) -> holder.sessionState = v,
                    holder -> holder.sessionState)
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

    @Getter
    private String[] gameSequence;

    @Getter
    // design here may change. My head canon is that the currentGame will pop from the array and the array of the sequence will shrink.
    // Alternatively we could store the index of the current game inside the sequence and keep the sequence as-is
    // I'm good with either
    private String currentGame;

    @Getter 
    @NotNull 
    private SessionState sessionState = SessionState.SETTING_UP;

}
