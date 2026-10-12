package com.gaiagauntlet.gauntlet.core.session.components;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.events.GauntletEventRegistry;
import com.gaiagauntlet.gauntlet.core.events.events.SessionStateEvent;
import com.gaiagauntlet.gauntlet.core.session.constants.SessionState;
import com.gaiagauntlet.gauntlet.core.session.registry.SessionRegistry;
import com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils;
import com.gaiagauntlet.gauntlet.utils.codec.StringRegistryCodec;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.logger.HytaleLogger;

import lombok.Getter;

/**
 * Holds the relevant information regarding an active session.
 * 
 * Does NOT hold specific information like players active, rather, only the
 * information necessary to identify a specific game
 */
public class GameSession {
    private final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static BuilderCodec<@NotNull GameSession> CODEC = BuilderCodec
            .builder(GameSession.class, GameSession::new)
            .append(new KeyedCodec<>("Id", Codec.STRING),
                    (holder, v) -> holder.id = v,
                    holder -> holder.id)
            .add()
            .append(new KeyedCodec<>("Components",
                    new StringRegistryCodec<>(new SessionRegistry(), ConcurrentHashMap::new)),
                    (holder, components) -> holder.sessionComponents = components,
                    holder -> holder.sessionComponents)
            .add()
            .append(new KeyedCodec<>("Sequence", Codec.STRING_ARRAY),
                    (holder, v) -> holder.setGames(Arrays.asList(v)),
                    holder -> holder.gameSequence.toArray(new String[0]))
            .add()
            .append(new KeyedCodec<>("CurrentGame", Codec.STRING),
                    (holder, v) -> holder.currentGame = v,
                    holder -> holder.currentGame)
            .add()
            .append(new KeyedCodec<>("Parties", new SetCodec<>(Codec.STRING, HashSet::new, false)),
                    (holder, v) -> {
                        holder.parties.clear();
                        holder.parties.addAll(v);
                    },
                    holder -> holder.parties)
            .add()
            .append(new KeyedCodec<>("Plugins", new SetCodec<>(Codec.STRING, HashSet::new, false)),
                    (holder, v) -> {
                        holder.plugins.clear();
                        holder.plugins.addAll(v);
                    },
                    holder -> holder.plugins)
            .add()
            .build();

    @Getter
    private Map<String, SessionComponent> sessionComponents;
    @Getter
    private String id;
    @Getter
    private Set<String> parties = ConcurrentHashMap.newKeySet();
    /**
     * Ids of the plugins installed in this session. Only these are set up for its
     * games
     */
    @Getter
    private Set<String> plugins = ConcurrentHashMap.newKeySet();

    private volatile boolean locked = false;

    public <T extends SessionComponent> void put(SessionComponentType<T> type, T component) {
        if (locked) {
            GaiaLog.atWarning().log("Attempted to write " + type.getIndex() + " to " + getId()
                    + " while game is locked (state is " + sessionState.toString() + ") - addition was blocked!");
            return;
        }
        sessionComponents.put(type.getIndex(), component);
    }

    /** Puts components keyed by their registered id, replacing any the session already has */
    public boolean putAll(Map<String, SessionComponent> components) {
        if (locked) {
            GaiaLog.atWarning().log("Attempted to write " + components.keySet() + " to " + getId()
                    + " while game is locked (state is " + sessionState.toString() + ") - addition was blocked!");
            return false;
        }
        sessionComponents.putAll(components);
        return true;
    }

    public <T extends SessionComponent> Optional<T> get(SessionComponentType<T> type) {
        var sesComp = sessionComponents.get(type.getIndex());
        if (sesComp == null)
            return Optional.empty();

        return Optional.of(type.getTypeClass().cast(sesComp));
    }

    public <T extends SessionComponent> T ensure(SessionComponentType<T> type, T component) {
        var sesComp = get(type);
        if (sesComp.isEmpty()) {
            put(type, component);
            return component;
        } else {
            return sesComp.get();
        }
    }

    public <T extends SessionComponent> Optional<T> remove(SessionComponentType<T> type) {
        var existing = sessionComponents.remove(type.getIndex());
        return Optional.ofNullable(type.getTypeClass().cast(existing));
    }

    @Getter
    private final ArrayDeque<String> gameSequence = new ArrayDeque<>();

    // design here may change. My head canon is that the currentGame will pop from
    // the array and the array of the sequence will shrink.
    // Alternatively we could store the index of the current game inside the
    // sequence and keep the sequence as-is
    // I'm good with either
    @Getter
    private String currentGame;

    @Getter
    @NotNull
    private SessionState sessionState = SessionState.IDLE;
    @Getter
    @Nullable
    private String errorReason;
    @Getter
    private int generation;

    public GameSession() {
        sessionComponents = new ConcurrentHashMap<>();
    }

    public GameSession(String id) {
        this.id = id;
        this();
    }

    public boolean available() {
        return !sessionState.active();
    }

    /** gets the next available game */
    @Nullable
    public String getNext() {
        return gameSequence.peekFirst();
    }

    private boolean transitionBlocked(SessionState state, @Nullable String gameIdCheck) {
        if (gameIdCheck != null && !gameIdCheck.equals(currentGame)) {
            error().log(
                    "Game " + gameIdCheck + " in " + getId() + " failed to switch to " + state.toString() + "! Game "
                            + currentGame
                            + " was somehow registered instead");
            return true;
        }

        if (sessionState.to(state)) {
            return false; // transition allowed, not blocked
        }

        error().log("Game failed to switch to " + state.toString() + "! State is " + sessionState.toString()
                + " instead!");

        return true;
    }

    private boolean transition(SessionState state, @Nullable String gameIdCheck, Runnable apply) {
        if (transitionBlocked(state, gameIdCheck))
            return false;
        var from = sessionState;
        apply.run();
        sessionState = state;
        locked = state == SessionState.RUNNING;
        logger().log(MessageUtils.msg("server.gg.gauntlet.session.transition.success")
                .param("sessionId", this.id)
                .param("newState", state.toString())
                .param("oldState", from.toString()));
        GauntletEventRegistry.dispatch(new SessionStateEvent(id, from, state, currentGame, generation));
        return true;
    }

    /** Whether the attempt started under this generation is still the session's current one */
    public boolean isCurrent(int attempt) {
        return attempt == generation;
    }

    private GaiaLog logger() {
        return GaiaLog.atInfo().withSession(this);
    }

    private GaiaLog error() {
        return GaiaLog.atError().withSession(this);
    }

    /**
     * transitions to the next game, popping it from the list and setting it as
     * current
     */
    @Nullable
    public String startNext() {
        if (gameSequence.isEmpty())
            return null;
        var started = transition(SessionState.SETTING_UP, null, () -> {
            currentGame = gameSequence.pollFirst();
            generation++;
            errorReason = null;
        });
        return started ? currentGame : null;
    }

    /**
     * Sets the game started under this attempt as running, refusing once the
     * attempt has been abandoned
     * <br />
     * <br />
     * <b>MANAGED BY THE ORCHESTRATOR</b>
     */
    public boolean setRunning(int attempt) {
        if (!isCurrent(attempt))
            return false;
        return transition(SessionState.RUNNING, null, () -> errorReason = null);
    }

    /**
     * RULE: Can switch to Complete if currently in Running state. Warns if already
     * finished/idle but returns true
     * <br />
     * <br />
     * Recovers from an errored state - but keeps the error reason
     */
    public boolean setComplete(String gameIdCheck) {
        return transition(SessionState.FINISHED, gameIdCheck, () -> errorReason = null);
    }

    /**
     * Returns TRUE if the transition happened. Returns FALSE if already in cleaning
     * or game is not running
     */
    public boolean setCleaning(String gameIdCheck) {
        return transition(SessionState.CLEANING, gameIdCheck, () -> {
        });
    }

    /**
     * Returns TRUE if the transition happened. Returns FALSE if already in cleaning
     * or game is not running
     */
    public boolean setEnding(String gameIdCheck) {
        return transition(SessionState.ENDING, gameIdCheck, () -> {
        });
    }

    /** Set if the session resulted in an error of some kind */
    public void setErrored(String errorReason) {
        transition(SessionState.ERROR, null, () -> this.errorReason = errorReason);
    }

    /**
     * Gives up on the attempt in progress, so a setup or clean still running for it
     * can no longer change the session. Leaves the session in ERROR, from which a
     * CLEAN tears the game down
     */
    public void abandon(String errorReason) {
        transition(SessionState.ERROR, null, () -> {
            generation++;
            this.errorReason = errorReason;
        });
    }

    /** adds a game to the sequence */
    public void addGame(@Nonnull String gameId) {
        gameSequence.addLast(gameId);
    }

    public void addGames(@Nonnull Collection<String> games) {
        gameSequence.addAll(games);
    }

    public void setGames(Collection<String> games) {
        gameSequence.clear();
        if (games != null) {
            gameSequence.addAll(games);
        }
    }

    /** removes a game if it is present */
    public boolean removeGame(@Nonnull String gameId) {
        return gameSequence.removeFirstOccurrence(gameId);
    }
}
