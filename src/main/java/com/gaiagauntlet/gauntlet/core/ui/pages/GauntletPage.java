package com.gaiagauntlet.gauntlet.core.ui.pages;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.ui.events.AdminPageEvent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * A player page that re-renders on a timer and answers each click with a line in its #Status label.
 * Subclasses build the markup once, render the live parts, and handle their actions; "page.close"
 * closes the page. Renders and actions take turns, so subclasses can keep row caches unguarded.
 */
public abstract class GauntletPage extends InteractiveCustomUIPage<AdminPageEvent> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final long refreshMillis;
    private final AtomicReference<ScheduledFuture<?>> refresh = new AtomicReference<>();
    private final Object lock = new Object();

    protected GauntletPage(@Nonnull PlayerRef playerRef, long refreshMillis) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminPageEvent.CODEC);
        this.refreshMillis = refreshMillis;
    }

    /** Appends the markup and binds the controls that are always there. */
    protected abstract void build(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt);

    /** Rewrites the live parts. Runs after the build, on the timer, and after every action. */
    protected abstract void render(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt);

    /** Performs one action. Returns the line to show in the status label, or null. */
    @Nullable
    protected abstract Message handle(@Nonnull AdminPageEvent event);

    @Override
    public final void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt,
            @Nonnull Store<EntityStore> store) {
        synchronized (lock) {
            build(cmd, evt);
            render(cmd, evt);
        }
        startRefresh();
    }

    @Override
    public final void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull AdminPageEvent data) {
        switch (data.action()) {
            case "" -> sendUpdate(null, false);
            case "page.close" -> close();
            default -> {
                Message status;
                synchronized (lock) {
                    try {
                        status = handle(data);
                    } catch (IllegalStateException | IllegalArgumentException e) {
                        status = Widgets.fail(e.getMessage() == null ? "That did not work" : e.getMessage());
                    } catch (RuntimeException e) {
                        LOGGER.atWarning().withCause(e).log("Page action %s failed", data.action());
                        status = Widgets.fail("Failed: " + e.getMessage());
                    }
                }
                update(status);
            }
        }
    }

    /** Shows a status line from any thread, rendering the page with it. */
    public void pushStatus(@Nonnull Message status) {
        update(status);
    }

    private void update(@Nullable Message status) {
        synchronized (lock) {
            var cmd = new UICommandBuilder();
            var evt = new UIEventBuilder();
            if (status != null) {
                cmd.set("#Status.TextSpans", status);
            }
            try {
                render(cmd, evt);
            } catch (RuntimeException e) {
                LOGGER.atWarning().withCause(e).log("%s failed to render, so nothing was sent", getClass().getSimpleName());
                return;
            }
            sendUpdate(cmd, evt, false);
        }
    }

    private void startRefresh() {
        var task = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(this::refreshTick, refreshMillis, refreshMillis, TimeUnit.MILLISECONDS);
        if (!refresh.compareAndSet(null, task)) {
            task.cancel(false);
        }
    }

    private void refreshTick() {
        try {
            var ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) {
                stopRefresh();
                return;
            }
            update(null);
        } catch (Throwable t) {
            LOGGER.atWarning().withCause(t).log("%s refresh failed, so it stopped", getClass().getSimpleName());
            stopRefresh();
        }
    }

    private void stopRefresh() {
        var task = refresh.getAndSet(null);
        if (task != null) {
            task.cancel(false);
        }
    }

    @Override
    protected void close() {
        stopRefresh();
        super.close();
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        stopRefresh();
    }
}
