package com.gaiagauntlet.gauntlet.core.ui.huds;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.orchestrator.GauntletOrchestrator;
import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.HudElement;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * The player HUD, one layer per player, built on the server's custom HUD pipeline. It holds every HUD
 * element the orchestrator collected, shows the ones that apply to the player and their session, and
 * renders the visible ones every second.
 */
public final class GauntletHud extends CustomUIHud {

    public static final String KEY = "Gauntlet";

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String HUD = "Gauntlet/Hud/Hud.ui";
    private static final long REFRESH_MILLIS = 1000;

    private final List<HudElement> elements = GauntletOrchestrator.getHudElements();

    /** Visibility last sent for each element, so only changes are pushed. */
    private final boolean[] shown = new boolean[elements.size()];

    private final AtomicReference<ScheduledFuture<?>> refresh = new AtomicReference<>();

    /** Builds and renders touch the elements' caches, so the refresh timer and the world thread take turns. */
    private final Object lock = new Object();

    @Nullable private String sessionId;

    public GauntletHud(@Nonnull PlayerRef playerRef) {
        super(playerRef, KEY);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder cmd) {
        synchronized (lock) {
            var player = getPlayerRef();
            var session = sessionOf(player);
            sessionId = session == null ? null : session.getId();

            cmd.append(HUD);
            for (var element : elements) {
                cmd.append("#Hud", element.getMarkup());
            }
            for (var element : elements) {
                element.buildOnce(cmd, player, session);
            }
            for (int i = 0; i < elements.size(); i++) {
                var element = elements.get(i);
                shown[i] = element.isVisible(player, session);
                cmd.set("#Hud[" + i + "].Visible", shown[i]);
                if (shown[i]) {
                    element.render(cmd, player, session);
                }
            }
        }
        startRefresh();
    }

    private void push() {
        synchronized (lock) {
            var player = getPlayerRef();
            var session = sessionOf(player);
            var cmd = new UICommandBuilder();
            try {
                var id = session == null ? null : session.getId();
                if (!Objects.equals(id, sessionId)) {
                    sessionId = id;
                    for (var element : elements) {
                        element.buildOnce(cmd, player, session);
                    }
                }
                for (int i = 0; i < elements.size(); i++) {
                    var element = elements.get(i);
                    var visible = element.isVisible(player, session);
                    if (visible != shown[i]) {
                        shown[i] = visible;
                        cmd.set("#Hud[" + i + "].Visible", visible);
                    }
                    if (visible) {
                        element.render(cmd, player, session);
                    }
                }
            } catch (RuntimeException e) {
                LOGGER.atWarning().withCause(e).log("HUD for %s failed to render; nothing sent", player.getUsername());
                return;
            }
            if (cmd.getCommands().length > 0) {
                update(false, cmd);
            }
        }
    }

    @Nullable
    private static GameSession sessionOf(@Nonnull PlayerRef player) {
        return GauntletUtils.sessionFor(player).orElse(null);
    }

    // Refresh timer

    private void startRefresh() {
        var task = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(this::refreshTick, REFRESH_MILLIS, REFRESH_MILLIS, TimeUnit.MILLISECONDS);
        if (!refresh.compareAndSet(null, task)) {
            task.cancel(false);
        }
    }

    private void refreshTick() {
        try {
            var ref = getPlayerRef().getReference();
            if (ref == null || !ref.isValid()) {
                stop();
                return;
            }
            push();
        } catch (Throwable t) {
            LOGGER.atWarning().withCause(t).log("HUD refresh failed; stopping it");
            stop();
        }
    }

    /** Stops the refresh timer. The layer stays on the client until it is replaced or removed. */
    public void stop() {
        var task = refresh.getAndSet(null);
        if (task != null) {
            task.cancel(false);
        }
    }

    @Override
    protected void onRemove() {
        stop();
    }
}
