package com.gaiagauntlet.gauntlet.core.session.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.core.session.constants.SessionState;
import com.gaiagauntlet.gauntlet.core.session.registry.SessionRegistry;
import com.gaiagauntlet.gauntlet.utils.codec.StringRegistryCodec;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

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
                    (holder, components) -> holder.sessionComponents = components,
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
    private Map<String, SessionComponent> sessionComponents;

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
    // design here may change. My head canon is that the currentGame will pop from
    // the array and the array of the sequence will shrink.
    // Alternatively we could store the index of the current game inside the
    // sequence and keep the sequence as-is
    // I'm good with either
    private String currentGame;

    @Getter
    @NotNull
    private SessionState sessionState = SessionState.SETTING_UP;

    public GameSession() {
        sessionComponents = new ConcurrentHashMap<>();
    }

    public boolean available() {
        return sessionState == SessionState.FINISHED || sessionState == SessionState.IDLE;
    }

    /** gets the next available game */
    @Nullable 
    public String getNext() {
        return gameSequence == null || gameSequence.length == 0 ? null : gameSequence[0];
    }
    /** transitions to the next game, popping it from the list and setting it as current */
    @Nullable 
    public String startNext() {
        String nextGame = getNext();
        if (nextGame == null) {
            currentGame = null;
            return null;
        }

        currentGame = nextGame;
        sessionState = SessionState.RUNNING;
        int remainingGames = gameSequence.length - 1;
        if (remainingGames == 0) {
            gameSequence = new String[0];
        } else {
            System.arraycopy(gameSequence, 1, gameSequence, 0, remainingGames);
            gameSequence = Arrays.copyOf(gameSequence, remainingGames);
        }

        return currentGame;
    }
    /** adds a game to the sequence */
    public void addGame(@Nonnull String gameId) {

        if (gameSequence == null || gameSequence.length == 0) {
            gameSequence = new String[] { gameId };
            return;
        }

        String[] updatedSequence = Arrays.copyOf(gameSequence, gameSequence.length + 1);
        updatedSequence[gameSequence.length] = gameId;
        gameSequence = updatedSequence;
    }
    /** removes a game from the sequence */
    public void removeGame(int index) {
        if (gameSequence == null || index < 0 || index >= gameSequence.length) {
            return;
        }

        int remainingGames = gameSequence.length - index - 1;
        if (remainingGames > 0) {
            System.arraycopy(gameSequence, index + 1, gameSequence, index, remainingGames);
        }
        gameSequence = Arrays.copyOf(gameSequence, gameSequence.length - 1);
    }
    /** removes a game if it is present */
    public void removeGameIfPresent(@Nonnull String gameId) {
        if (gameSequence == null) {
            return;
        }

        for (int index = 0; index < gameSequence.length; index++) {
            if (gameId.equals(gameSequence[index])) {
                removeGame(index);
                return;
            }
        }
    }
}
