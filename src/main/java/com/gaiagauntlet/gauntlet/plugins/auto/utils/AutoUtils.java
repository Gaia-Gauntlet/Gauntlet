package com.gaiagauntlet.gauntlet.plugins.auto.utils;

import java.util.ArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.events.GauntletEventRegistry;
import com.gaiagauntlet.gauntlet.core.events.events.SessionEvent;
import com.gaiagauntlet.gauntlet.core.events.events.SessionEvent.SessionOperation;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.session.constants.SessionState;
import com.gaiagauntlet.gauntlet.plugins.auto.AutoPlugin;
import com.gaiagauntlet.gauntlet.plugins.auto.components.AutoComponent;
import com.gaiagauntlet.gauntlet.plugins.auto.components.assets.AutoSessionAsset;
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
    public static AutoSessionAsset config(@Nonnull GameSession session, @Nonnull AutoComponent auto) {
        return AutoSessionAsset.getAssetMap().get(configId(session, auto));
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
                    .log("Auto has no AutoSessionAsset named " + configId(session, auto) + ", so it is not running");
            stop(hub, session.getId());
            return;
        }

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
                if (present < config.getSpinUpPlayers() || session.getNext() == null) {
                    stop(hub, session.getId());
                    return;
                }
                var pause = session.getSessionState() == SessionState.FINISHED ? config.getIntermissionSeconds()
                        : config.getGatherSeconds();
                if (auto.getFailures() > 0) {
                    var delay = retryDelay(session, auto, config);
                    if (delay < 0)
                        return;
                    pause = Math.max(pause, delay);
                }
                var timerId = session.getSessionState() == SessionState.FINISHED ? INTERMISSION : GATHER;
                arm(hub, session, timerId, pause, TimerUtils.IMG_COUNTDOWN);
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
            var op = switch (timerId) {
                case GATHER, INTERMISSION -> (state == SessionState.IDLE || state == SessionState.FINISHED)
                        && present >= config.getSpinUpPlayers() && session.getNext() != null
                                ? SessionOperation.SETUP
                                : null;
                case SETUP_DEADLINE -> state == SessionState.SETTING_UP ? SessionOperation.RECOVER : null;
                case CLEAN_DEADLINE -> state == SessionState.ENDING || state == SessionState.CLEANING
                        ? SessionOperation.RECOVER
                        : null;
                case IDLE -> state == SessionState.RUNNING && present == 0 ? SessionOperation.CLEAN : null;
                case RETRY -> state == SessionState.ERROR ? SessionOperation.CLEAN : null;
                default -> null;
            };
            if (op != null) {
                GaiaLog.atInfo().withSession(session).log("Auto sends " + op + " to " + sessionId + " after " + timerId);
                GauntletEventRegistry.dispatch(new SessionEvent(op, sessionId).onComplete(_ -> schedule(sessionId)));
            }
        }
        advance(session);
    }

    /** The wait before the next retry, or -1 once the retries ran out and Auto paused the session */
    private static int retryDelay(@Nonnull GameSession session, @Nonnull AutoComponent auto,
            @Nonnull AutoSessionAsset config) {
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

    /** Arms the timer unless it is already counting down, replacing whatever timer was waiting before */
    private static void arm(@Nonnull World hub, @Nonnull GameSession session, @Nonnull String timerId, int seconds,
            @Nullable String image) {
        var sessionId = session.getId();
        var store = GameStore.ensureStore(hub, sessionId);
        store.ensure(ContextComponent.getComponentType(), () -> new ContextComponent(hub, AutoPlugin.ID, sessionId));
        if (TimerUtils.isRunning(store, timerId))
            return;
        var timer = image == null ? new GameTimerComponent(timerId) : new GameTimerComponent(timerId, image);
        TimerUtils.start(store, seconds, timer);
        TimerHud.update(store);
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
