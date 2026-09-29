package com.gaiagauntlet.gauntlet.core.ui.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.ui.events.AdminPageEvent;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
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
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * The in-game admin page, built on the server's own UI pipeline. It opens on the game the admin
 * stands in, a picker switches games, the active tab renders every two seconds, and every action
 * calls the same orchestrator and components the commands do. Dangerous actions ask first.
 */
public final class AdminPage extends InteractiveCustomUIPage<AdminPageEvent> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String PAGE = "GG/Admin/Dashboard.ui";
    private static final long REFRESH_MILLIS = 2000;
    private static final Set<String> NEEDS_CONFIRM = Set.of("match.stop", "match.end", "games.close", "games.remove");

    // Legacy hardcoded tabs - will need to be tweaked to be only the top-level mngmnt tabs and then game-specific tabs. One widget/tab per component ?
    private final List<AdminTab> tabs = List.of(new LogTab());//new MatchTab(), new GamesTab(), new ZonesTab(), new EventsTab(),
            // new BossesTab(), new TeamsTab(), new SettingsTab(), new LogTab());

    private final AtomicReference<ScheduledFuture<?>> refresh = new AtomicReference<>();

    /** Renders and actions touch the tabs' row caches, so the refresh timer and the click thread take turns. */
    private final Object lock = new Object();
    // private volatile Game game;
    private volatile AdminTab activeTab = tabs.get(0);
    @Nullable private volatile AdminPageEvent pendingConfirm;

    public AdminPage(@Nonnull PlayerRef playerRef/*,  @Nonnull Game game */) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminPageEvent.CODEC);
        // this.game = game;
    }

    // @Nonnull
    // public Game game() {
    //     return game;
    // }

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
        Widgets.bindChange(evt, "#GamePicker", "page.selectGame");
        for (var tab : tabs) {
            Widgets.bindArg(evt, "#Tab" + tab.id(), "page.selectTab", tab.id());
            tab.bind(evt);
        }
        fillGamePicker(cmd);
        for (var tab : tabs) {
            // tab.buildOnce(cmd, evt, game);
        }
        applyTab(cmd);
        // activeTab.render(cmd, evt, game);
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
            case "page.selectGame" -> selectGame(data.pick());
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
            if (tab.id().equalsIgnoreCase(tabId)) {
                try {
                    // return tab.handle(action, event, game, this);
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
    void pushStatus(@Nonnull Message status) {
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
                // activeTab.render(cmd, evt, game);
            } catch (RuntimeException e) {
                LOGGER.atWarning().withCause(e).log("Admin tab %s failed to render; nothing sent", activeTab.id());
                return;
            }
            sendUpdate(cmd, evt, false);
        }
    }

    private void applyTab(@Nonnull UICommandBuilder cmd) {
        for (var tab : tabs) {
            cmd.set("#Panel" + tab.id() + ".Visible", tab == activeTab);
            cmd.set("#Tab" + tab.id() + ".Disabled", tab == activeTab);
        }
    }

    private void selectTab(@Nullable String id) {
        for (var tab : tabs) {
            if (tab.id().equalsIgnoreCase(id == null ? "" : id) && tab != activeTab) {
                activeTab = tab;
                var cmd = new UICommandBuilder();
                var evt = new UIEventBuilder();
                applyTab(cmd);
                // tab.render(cmd, evt, game);
                sendUpdate(cmd, evt, false);
                return;
            }
        }
    }

    private void selectGame(@Nullable String id) {
        // var store = GlobalStore.find();
        // var chosen = store == null || id == null ? null : store.game(id).orElse(null);
        // if (chosen == null || chosen == game) {
        //     return;
        // }
        // game = chosen;
        // var cmd = new UICommandBuilder();
        // var evt = new UIEventBuilder();
        // for (var tab : tabs) {
        //     tab.buildOnce(cmd, evt, game);
        // }
        // activeTab.render(cmd, evt, game);
        // cmd.set("#ActionStatus.TextSpans", Widgets.ok("Now showing " + game.id()));
        // sendUpdate(cmd, evt, false);
    }

    /** Refills the game picker; called on open and after games are created or removed. */
    void fillGamePicker(@Nonnull UICommandBuilder cmd) {
        // var options = new ArrayList<Widgets.Option>();
        // for (var g : GlobalStore.get().games()) {
        //     options.add(new Widgets.Option(g.id() + ", " + g.type().name() + (g.isOpen() ? ", " + Orchestrator.state(g).name().toLowerCase() : ", closed"), g.id()));
        // }
        // Widgets.fillPicker(cmd, "#GamePicker", options);
        // cmd.set("#GamePicker.Value", game.id());
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

