package com.gaiagauntlet.gauntlet.core.ui.pages;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.orchestrator.GauntletOrchestrator;
import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.events.AdminPageEvent;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
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
import lombok.Getter;

/**
 * The in-game admin page, built on the server's own UI pipeline. It opens on the game the admin
 * stands in, a picker switches games, the active tab renders every two seconds, and every action
 * calls the same orchestrator and components the commands do. Dangerous actions ask first.
 */
public final class AdminPage extends InteractiveCustomUIPage<AdminPageEvent> {

    public static final String ID = "Admin";

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String PAGE = "Gauntlet/Admin/Dashboard.ui";
    private static final String TAB_BUTTON = "Gauntlet/Admin/TabButton.ui";
    private static final long REFRESH_MILLIS = 2000;
    private static final Set<String> NEEDS_CONFIRM = Set.of("match.stop", "match.end", "games.close", "games.remove");

    private List<AdminTab> tabs = List.of(new NoTab());//new LogTab(), new MatchTab(), new GamesTab(), new ZonesTab(), new EventsTab(),
            // new BossesTab(), new TeamsTab(), new SettingsTab(), new LogTab());

    private final AtomicReference<ScheduledFuture<?>> refresh = new AtomicReference<>();

    /** Renders and actions touch the tabs' row caches, so the refresh timer and the click thread take turns. */
    private final Object lock = new Object();

    @Nullable @Getter private GameSession session;
    private volatile AdminTab activeTab = tabs.getFirst();
    @Nullable private volatile AdminPageEvent pendingConfirm;
    @Getter private final PlayerRef player;

    public AdminPage(@Nonnull PlayerRef playerRef, @Nullable GameSession session) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminPageEvent.CODEC);
        this.player = playerRef;
        if (Objects.nonNull(session)) {
            this.session = session;
        } else {
            Optional<GameSession> firstSession = GauntletUtils.withResource().getSessions().values().stream().findFirst();
            firstSession.ifPresent(gameSession -> this.session = gameSession);
        }
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt,
            @Nonnull Store<EntityStore> store) {
        synchronized (lock) {
            build0(cmd, evt);
        }
        startRefresh();
    }

    private void build0(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        cmd.append(PAGE);
        Widgets.bind(evt, "#CloseButton", "page.close");
        Widgets.bind(evt, "#ConfirmYes", "page.confirmYes");
        Widgets.bind(evt, "#ConfirmNo", "page.confirmNo");
        Widgets.bindChange(evt, "#SessionPicker", "page.selectSession");

        var registered = GauntletOrchestrator.getAdminTabs();
        if (!registered.isEmpty()) tabs = registered;
        activeTab = tabs.getFirst();

        for (int i = 0; i < tabs.size(); i++) {
            var tab = tabs.get(i);
            cmd.append("#TabStrip", TAB_BUTTON);
            cmd.set("#TabStrip[" + i + "].Text", tab.getTitle());
            cmd.append("#TabBody", tab.getPanel());

            Widgets.bindArg(evt, "#TabStrip[" + i + "]", "page.selectTab", tab.getId());
            tab.bind(evt);
        }
        fillSessionPicker(cmd);
        for (var tab : tabs) {
             tab.buildOnce(cmd, evt, session);
        }
        applyTab(cmd);
        activeTab.render(cmd, evt, session);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull AdminPageEvent data) {
        synchronized (lock) {
            handle0(data);
        }
    }

    private void handle0(@Nonnull AdminPageEvent data) {
        var action = data.action();
        switch (action) {
            case "" -> sendUpdate(null, false);
            case "page.close" -> close();
            case "page.selectTab" -> selectTab(data.arg());
            case "page.selectSession" -> selectSession(data.pick());
            case "page.confirmNo" -> {
                pendingConfirm = null;
                closeConfirm(null);
            }
            case "page.confirmYes" -> {
                var armed = pendingConfirm;
                pendingConfirm = null;
                closeConfirm(armed == null ? null : perform(armed));
            }
            default -> {
                if (NEEDS_CONFIRM.contains(action)) {
                    openConfirm(data);
                } else {
                    update(perform(data));
                }
            }
        }
    }

    @Nullable
    private Message perform(@Nonnull AdminPageEvent event) {
        var action = event.action();
        var dot = action.indexOf('.');
        var tabId = dot < 0 ? "" : action.substring(0, dot);
        for (var tab : tabs) {
            if (tab.getId().equalsIgnoreCase(tabId)) {
                try {
                     return tab.handle(action, event, session, this);
                } catch (IllegalStateException | IllegalArgumentException e) {
                    return Widgets.fail(e.getMessage() == null ? "That did not work" : e.getMessage());
                } catch (RuntimeException e) {
                    LOGGER.atWarning().withCause(e).log("Admin action %s failed", action);
                    return Widgets.fail("Failed: " + e.getMessage());
                }
            }
        }
        LOGGER.atWarning().log("Admin page received unknown action %s", action);
        return null;
    }

    /** Runs the task on the game's arena thread and reports what it says afterwards. */
    // void onArena(@Nonnull Game target, @Nonnull BiConsumer<World, AdminPage> task, @Nonnull String pendingText) {
        // var arena = ArenaComponent.TYPE.of(target).world();
        // if (arena == null) {
        //     throw new IllegalStateException(target.id() + " has no arena loaded");
        // }
        // Worlds.run(arena, () -> {
        //     try {
        //         task.accept(arena, this);
        //     } catch (IllegalStateException | IllegalArgumentException e) {
        //         pushStatus(Widgets.fail(e.getMessage() == null ? "That did not work" : e.getMessage()));
        //     }
        // });
    // }

    /** Sets the status line from any thread, rendering the active tab with it. */
    public void pushStatus(@Nonnull Message status) {
        update(status);
    }

    // Rendering

    private void update(@Nullable Message status) {
        synchronized (lock) {
            var cmd = new UICommandBuilder();
            var evt = new UIEventBuilder();
            if (status != null) {
                cmd.set("#ActionStatus.TextSpans", status);
            }
            try {
                 activeTab.render(cmd, evt, session);
            } catch (RuntimeException e) {
                LOGGER.atWarning().withCause(e).log("Admin tab %s failed to render; nothing sent", activeTab.getId());
                return;
            }
            sendUpdate(cmd, evt, false);
        }
    }

    private void applyTab(@Nonnull UICommandBuilder cmd) {
        for (int i = 0; i < tabs.size(); i++) {
            var tab = tabs.get(i);
            cmd.set("#TabBody[" + i + "].Visible", tab == activeTab);
            cmd.set("#TabStrip[" + i + "].Disabled", tab == activeTab);
        }
    }

    private void selectTab(@Nullable String id) {
        for (var tab : tabs) {
            if (tab.getId().equalsIgnoreCase(id == null ? "" : id) && tab != activeTab) {
                activeTab = tab;
                var cmd = new UICommandBuilder();
                var evt = new UIEventBuilder();
                applyTab(cmd);
                tab.render(cmd, evt, session);
                sendUpdate(cmd, evt, false);
                return;
            }
        }
    }

    private void selectSession(@Nullable String id) {
        var sessions = GauntletUtils.withResource().getSessions();
        var chosen = sessions.get(id);
        if (chosen == null) return;
        session = chosen;
        var cmd = new UICommandBuilder();
        var evt = new UIEventBuilder();
        for (var tab : tabs) {
            tab.buildOnce(cmd, evt, session);
        }
        activeTab.render(cmd, evt, session);
        cmd.set("#ActionStatus.TextSpans", Widgets.ok("Now showing " + session.getId()));
        sendUpdate(cmd, evt, false);
    }

    /** Refills the session picker; called on open and after sessions are created or removed. */
    void fillSessionPicker(@Nonnull UICommandBuilder cmd) {
        var sessions = GauntletUtils.withResource().getSessions();

        var options = new ArrayList<Widgets.Option>();
        for (var s : sessions.values()) {
            options.add(
                new Widgets.Option(
                    s.getId() + " - " + s.getCurrentGame(),
                    s.getId()
                )
            );
        }
        Widgets.fillPicker(cmd, "#SessionPicker", options);

        cmd.set("#SessionPicker.Value", Objects.isNull(session) ? "" : session.getId());
    }

    // Confirm prompt

    private void openConfirm(@Nonnull AdminPageEvent event) {
        // pendingConfirm = event;
        // var cmd = new UICommandBuilder();
        // cmd.set("#MainPage.Visible", false);
        // cmd.set("#ConfirmPage.Visible", true);
        // cmd.set("#ConfirmMessage.Text", switch (event.action()) {
        //     case "match.stop" -> "Stop the match in " + game.id() + " and send everyone back to their lobby?";
        //     case "match.end" -> "End the live match in " + game.id() + " now and show the standings?";
        //     case "games.close" -> "Close " + game.id() + ", sending everyone in its lobbies to the hub and removing the lobbies?";
        //     default -> "Remove " + game.id() + " for good?";
        // });
        // sendUpdate(cmd, null, false);
    }

    private void closeConfirm(@Nullable Message status) {
        // var cmd = new UICommandBuilder();
        // var evt = new UIEventBuilder();
        // cmd.set("#ConfirmPage.Visible", false);
        // cmd.set("#MainPage.Visible", true);
        // if (status != null) {
        //     cmd.set("#ActionStatus.TextSpans", status);
        // }
        // activeTab.render(cmd, evt, game);
        // sendUpdate(cmd, evt, false);
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
            var ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) {
                stopRefresh();
                return;
            }
            if (pendingConfirm == null) {
                update(null);
            }
        } catch (Throwable t) {
            LOGGER.atWarning().withCause(t).log("Admin page refresh failed; stopping it");
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

