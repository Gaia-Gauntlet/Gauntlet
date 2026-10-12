package com.gaiagauntlet.gauntlet.plugins.auto.utils;

import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.msg;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.events.GauntletEventRegistry;
import com.gaiagauntlet.gauntlet.core.hud.events.HudEvent;
import com.gaiagauntlet.gauntlet.core.events.events.SessionComponentsEvent;
import com.gaiagauntlet.gauntlet.core.events.events.SessionEvent;
import com.gaiagauntlet.gauntlet.core.events.events.SessionEvent.SessionOperation;
import com.gaiagauntlet.gauntlet.core.events.events.SessionQueueEvent;
import com.gaiagauntlet.gauntlet.core.events.events.SessionQueueEvent.SessionQueueOp;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.session.constants.SessionState;
import com.gaiagauntlet.gauntlet.plugins.auto.AutoPlugin;
import com.gaiagauntlet.gauntlet.plugins.auto.components.AutoComponent;
import com.gaiagauntlet.gauntlet.plugins.auto.components.VoteComponent;
import com.gaiagauntlet.gauntlet.plugins.auto.components.assets.AutoSession;
import com.gaiagauntlet.gauntlet.plugins.auto.components.assets.SessionPreset;
import com.gaiagauntlet.gauntlet.plugins.auto.components.rotation.VoteRotation;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.ContextComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.timer.components.GameTimerComponent;
import com.gaiagauntlet.gauntlet.plugins.timer.ui.TimerHud;
import com.gaiagauntlet.gauntlet.plugins.timer.utils.TimerUtils;
import com.hypixel.hytale.server.core.universe.world.World;

/**
 * Runs every session with Auto installed. Each trigger re-reads the session and arms the one timer
 * it is waiting on, kept in the session's game store on the hub, and only a finished timer acts.
 */
public final class AutoUtils {
    // Timer IDs 
    public static final String GATHER = "AutoGather";
    public static final String INTERMISSION = "AutoIntermission";
    public static final String SETUP_DEADLINE = "AutoSetupDeadline";
    public static final String CLEAN_DEADLINE = "AutoCleanDeadline";
    public static final String IDLE = "AutoIdle";
    public static final String RETRY = "AutoRetry";

    private AutoUtils() {
    }

    public static void schedule(@Nonnull String sessionId) {
        var hub = GauntletUtils.withHubWorld();
        hub.execute(() -> GauntletUtils.sessionFor(sessionId)
                .ifPresentOrElse(AutoUtils::advance, () -> stop(hub, sessionId)));
    }

    /** Re-reads every session, and drops the hub stores of sessions Auto no longer runs */
    public static void scheduleAll() {
        var hub = GauntletUtils.withHubWorld();
        hub.execute(() -> {
            var sessions = GauntletUtils.withResource().getSessions();
            for (var store : new ArrayList<>(GameStore.withResource(hub).getAll())) {
                if (!sessions.containsKey(store.getSessionId()))
                    stop(hub, store.getSessionId());
            }
            sessions.values().forEach(AutoUtils::advance);
        });
    }

    public static boolean runs(@Nonnull GameSession session) {
        return session.getPlugins().contains(AutoPlugin.ID);
    }

    /** The session's Auto settings, created on first use while the session still accepts components */
    @Nullable
    public static AutoComponent auto(@Nonnull GameSession session) {
        if (!runs(session))
            return null;
        return session.get(AutoComponent.getComponentType()).orElseGet(() -> {
            var auto = new AutoComponent();
            if (session.getSessionState() != SessionState.RUNNING)
                session.put(AutoComponent.getComponentType(), auto);
            return auto;
        });
    }

    @Nonnull
    public static String configId(@Nonnull GameSession session, @Nonnull AutoComponent auto) {
        var configId = auto.getConfigId();
        return configId == null ? session.getId() : configId;
    }

    @Nullable
    public static AutoSession config(@Nonnull GameSession session, @Nonnull AutoComponent auto) {
        return AutoSession.getAssetMap().get(configId(session, auto));
    }

    private static void advance(@Nonnull GameSession session) {
        var hub = GauntletUtils.withHubWorld();
        var auto = auto(session);
        if (auto == null || auto.isPaused()) {
            stop(hub, session.getId());
            return;
        }
        var config = config(session, auto);
        if (config == null) {
            GaiaLog.atWarning().withSession(session)
                    .log("Auto has no AutoSession named " + configId(session, auto) + ", so it is not running");
            stop(hub, session.getId());
            return;
        }

        // runs validators and retry hook to verify the state is working properly
        var present = GauntletUtils.playersFor(session).size();
        switch (session.getSessionState()) {
            case SETTING_UP -> arm(hub, session, SETUP_DEADLINE, config.getSetupTimeoutSeconds(), null);
            case ENDING, CLEANING -> arm(hub, session, CLEAN_DEADLINE, config.getCleanTimeoutSeconds(), null);
            case RUNNING -> {
                if (present == 0)
                    arm(hub, session, IDLE, config.getIdleGraceSeconds(), null);
                else
                    stop(hub, session.getId());
            }
            case ERROR -> {
                var delay = retryDelay(session, auto, config);
                if (delay >= 0)
                    arm(hub, session, RETRY, delay, null);
            }
            case IDLE, FINISHED -> {
                var rotation = session.getNext() == null ? config.getRotation() : null;
                if (present < config.getSpinUpPlayers() || (session.getNext() == null && rotation == null)) {
                    stop(hub, session.getId());
                    return;
                }
                var intermission = session.getSessionState() == SessionState.FINISHED
                        || (rotation != null && rotation.holdsIntermission());
                var pause = intermission ? config.getIntermissionSeconds() : config.getGatherSeconds();
                if (auto.getFailures() > 0) {
                    var delay = retryDelay(session, auto, config);
                    if (delay < 0)
                        return;
                    pause = Math.max(pause, delay);
                }
                if (arm(hub, session, intermission ? INTERMISSION : GATHER, pause, TimerUtils.IMG_COUNTDOWN)
                        && rotation != null && auto.getNextPreset() == null)
                    rotation.open(session, auto);
            }
        }
    }

    /** Acts on a finished timer if the session is still in the state the timer was waiting on */
    public static void fire(@Nonnull String sessionId, @Nonnull String timerId) {
        var session = GauntletUtils.sessionFor(sessionId).orElse(null);
        if (session == null)
            return;
        var auto = auto(session);
        var config = auto == null ? null : config(session, auto);
        if (auto != null && !auto.isPaused() && config != null) {
            var state = session.getSessionState();
            var present = GauntletUtils.playersFor(session).size();
            var waitEnded = (timerId.equals(GATHER) || timerId.equals(INTERMISSION))
                    && (state == SessionState.IDLE || state == SessionState.FINISHED)
                    && present >= config.getSpinUpPlayers();
            if (waitEnded && session.getNext() == null && startPreset(session, auto, config))
                return;
            var op = switch (timerId) {
                case GATHER, INTERMISSION -> waitEnded && session.getNext() != null ? SessionOperation.SETUP : null;
                case SETUP_DEADLINE -> state == SessionState.SETTING_UP ? SessionOperation.RECOVER : null;
                case CLEAN_DEADLINE -> state == SessionState.ENDING || state == SessionState.CLEANING
                        ? SessionOperation.RECOVER
                        : null;
                case IDLE -> state == SessionState.RUNNING && present == 0 ? SessionOperation.CLEAN : null;
                case RETRY -> state == SessionState.ERROR ? SessionOperation.CLEAN : null;
                default -> null;
            };
            if (op != null) {
                if (op == SessionOperation.SETUP)
                    session.remove(VoteComponent.getComponentType());
                GaiaLog.atInfo().withSession(session).log("Auto sends " + op + " to " + sessionId + " after " + timerId);
                GauntletEventRegistry.dispatch(new SessionEvent(op, sessionId).onComplete(_ -> schedule(sessionId)));
            }
        }
        advance(session);
    }

    /**
     * Applies the admin's pick or the rotation's, then queues its game and sets it up, each step only
     * once the one before it succeeded. Returns false when there was nothing to start
     */
    private static boolean startPreset(@Nonnull GameSession session, @Nonnull AutoComponent auto,
            @Nonnull AutoSession config) {
        var rotation = config.getRotation();
        var override = auto.getNextPreset();
        var presetId = override != null ? override : rotation == null ? null : rotation.pick(session, auto);
        if (presetId == null)
            return false;
        if (override != null) {
            auto.setNextPreset(null);
            session.remove(VoteComponent.getComponentType());
        }
        var preset = SessionPreset.getAssetMap().get(presetId);
        if (preset == null) {
            GaiaLog.atError().withSession(session).log("Auto cannot start preset " + presetId + ", it is not loaded");
            return false;
        }

        var sessionId = session.getId();
        GaiaLog.atInfo().withSession(session).log("Auto applies preset " + presetId + " to " + sessionId);
        GauntletEventRegistry.dispatch(new SessionComponentsEvent(sessionId, preset.components())
                .onComplete(applied -> then(sessionId, applied, () -> GauntletEventRegistry.dispatch(
                        new SessionQueueEvent(SessionQueueOp.APPEND, sessionId, List.of(preset.getGame()))
                                .onComplete(queued -> then(sessionId, queued, () -> {
                                    GauntletUtils.sessionFor(sessionId).ifPresent(current -> {
                                        var latest = auto(current);
                                        if (latest != null)
                                            latest.setCurrentPreset(presetId);
                                        VoteUtils.tell(current, msg("server.gg.plugins.auto.next")
                                                .param("preset", VoteUtils.title(presetId)));
                                    });
                                    GauntletEventRegistry.dispatch(new SessionEvent(SessionOperation.SETUP, sessionId)
                                            .onComplete(_ -> schedule(sessionId)));
                                }))))));
        return true;
    }

    /** Runs the next step on a fresh hub task, or re-reads the session when the step before failed */
    private static void then(@Nonnull String sessionId, @Nullable GaiaLog result, @Nonnull Runnable next) {
        GauntletUtils.withHubWorld().execute(() -> {
            if (result != null && result.getLevel().intValue() >= Level.WARNING.intValue()) {
                schedule(sessionId);
                return;
            }
            next.run();
        });
    }

    /** The preset the session plays next if nothing changes: the admin's pick, else the rotation's */
    @Nullable
    public static String upNext(@Nonnull GameSession session, @Nonnull AutoComponent auto) {
        if (auto.getNextPreset() != null)
            return auto.getNextPreset();
        var config = config(session, auto);
        var rotation = config == null ? null : config.getRotation();
        return rotation == null ? null : rotation.upNext(session, auto);
    }

    /** Makes the admin's pick the next preset, ending an open vote since its result would be ignored */
    public static void pickNext(@Nonnull GameSession session, @Nonnull AutoComponent auto, @Nonnull String presetId) {
        auto.setNextPreset(presetId);
        if (!VoteUtils.isOpen(session))
            return;
        session.remove(VoteComponent.getComponentType());
        VoteUtils.tell(session, msg("server.gg.plugins.auto.vote.overridden").param("preset", VoteUtils.title(presetId)));
        HudEvent.refresh(session.getId()).dispatch();
    }

    /** Opens a vote now. Only a vote rotation between games can, and not while one is open or an admin picked */
    public static boolean openVote(@Nonnull GameSession session) {
        var auto = auto(session);
        var config = auto == null ? null : config(session, auto);
        if (config == null || !(config.getRotation() instanceof VoteRotation rotation) || !session.available()
                || VoteUtils.isOpen(session) || auto.getNextPreset() != null)
            return false;
        session.remove(VoteComponent.getComponentType());
        rotation.open(session, auto);
        return VoteUtils.isOpen(session);
    }

    /** Seconds left before the next game starts, or -1 when the session is not waiting on one */
    public static int secondsToStart(@Nonnull GameSession session) {
        return GameStore.withStore(GauntletUtils.withHubWorld(), session.getId())
                .flatMap(TimerUtils::get)
                .filter(timer -> timer.isRunning()
                        && (GATHER.equals(timer.getTimerId()) || INTERMISSION.equals(timer.getTimerId())))
                .map(timer -> (int) Math.ceil(timer.getCountdownRemaining()))
                .orElse(-1);
    }

    /** Ends the gather or intermission now, so the next game starts at once */
    public static boolean startNow(@Nonnull GameSession session) {
        var store = GameStore.withStore(GauntletUtils.withHubWorld(), session.getId()).orElse(null);
        if (store == null || !(TimerUtils.isRunning(store, GATHER) || TimerUtils.isRunning(store, INTERMISSION)))
            return false;
        TimerUtils.complete(store);
        return true;
    }

    /** The wait before the next retry, or -1 once the retries ran out and Auto paused the session */
    private static int retryDelay(@Nonnull GameSession session, @Nonnull AutoComponent auto,
            @Nonnull AutoSession config) {
        var retries = config.getRetrySeconds();
        var failures = Math.max(1, auto.getFailures());
        if (retries == null || failures > retries.length) {
            auto.setPaused(true);
            stop(GauntletUtils.withHubWorld(), session.getId());
            GaiaLog.atError().withSession(session).log("Auto paused " + session.getId() + " after " + auto.getFailures()
                    + " failures in a row (" + session.getErrorReason() + "). Resume it from the Auto tab once fixed");
            return -1;
        }
        return retries[failures - 1];
    }

    /**
     * Arms the timer unless it is already counting down, replacing whatever timer was waiting before.
     * Returns whether a new countdown started
     */
    private static boolean arm(@Nonnull World hub, @Nonnull GameSession session, @Nonnull String timerId, int seconds,
            @Nullable String image) {
        var sessionId = session.getId();
        var store = GameStore.ensureStore(hub, sessionId);
        store.ensure(ContextComponent.getComponentType(), () -> new ContextComponent(hub, AutoPlugin.ID, sessionId));
        if (TimerUtils.isRunning(store, timerId))
            return false;
        var timer = image == null ? new GameTimerComponent(timerId) : new GameTimerComponent(timerId, image);
        TimerUtils.start(store, seconds, timer);
        TimerHud.update(store);
        return true;
    }

    /**
     * Drops the session's hub store when it is Auto's, so nothing is left waiting while a game runs or
     * Auto is off. An empty store is a leftover from before a restart, since its components are not saved
     */
    private static void stop(@Nonnull World hub, @Nonnull String sessionId) {
        var stores = GameStore.withResource(hub);
        stores.get(sessionId).ifPresent(store -> {
            var owner = store.get(ContextComponent.getComponentType()).map(ContextComponent::getGameId);
            var owned = owner.map(AutoPlugin.ID::equals).orElse(store.getSessionComponents().isEmpty());
            if (!owned)
                return;
            TimerUtils.cancel(store);
            stores.remove(sessionId);
        });
    }
}
