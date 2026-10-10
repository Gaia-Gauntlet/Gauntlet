package com.gaiagauntlet.gauntlet.core.ui.pages;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.orchestrator.GauntletOrchestrator;
import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.events.events.SessionPluginEvent.SessionPluginOp;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.AdminSection;
import com.gaiagauntlet.gauntlet.core.ui.SessionText;
import com.gaiagauntlet.gauntlet.core.ui.events.AdminPageEvent;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.core.ui.tabs.SessionTab;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.Getter;

/**
 * The in-game admin page, built on the server's own UI pipeline. The top strip opens the session
 * section, the controller of the session's game, or a plugin's section, which offers to install the
 * plugin when the session does not have it. The active tab renders every two seconds, and every
 * action calls the same orchestrator and components the commands do. Dangerous actions ask first.
 */
public final class AdminPage extends InteractiveCustomUIPage<AdminPageEvent> {

    public static final String ID = "Admin";

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String PAGE = "Gauntlet/Admin/Dashboard.ui";
    private static final String TAB_BUTTON = "Gauntlet/Admin/TabButton.ui";
    private static final String STYLES = "Gauntlet/Admin/Common.ui";
    private static final Value<String> TAB_STYLE = Value.ref(STYLES, "TabStyle");
    private static final Value<String> TAB_ACTIVE_STYLE = Value.ref(STYLES, "TabActiveStyle");
    private static final Value<String> TAB_OFF_STYLE = Value.ref(STYLES, "TabOffStyle");
    private static final String SESSION = "Session";
    private static final String CONTROLLER = "Controller";
    private static final long REFRESH_MILLIS = 2000;
    private static final Set<String> NEEDS_CONFIRM = Set.of("match.stop", "match.end", "games.close", "games.remove", "session.destroy", "session.game.stop",
            "session.plugin.uninstall", "plugin.uninstall");

    private List<AdminSection> sections = List.of();
    /** Every section's tabs, in the order their panels and sub strip buttons were appended. */
    private final List<AdminTab> tabs = new ArrayList<>();
    /** What each top strip button opens: the session section, the controller, or a plugin id. */
    private final List<String> strip = new ArrayList<>();

    private final AtomicReference<ScheduledFuture<?>> refresh = new AtomicReference<>();

    /** Renders and actions touch the tabs' row caches, so the refresh timer and the click thread take turns. */
    private final Object lock = new Object();

    @Nullable @Getter private GameSession session;
    private volatile String activeSection = SESSION;
    @Nullable private volatile AdminTab activeTab;
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
        Widgets.bind(evt, "#InstallPluginButton", "plugin.install");
        Widgets.bind(evt, "#UninstallPlugin", "plugin.uninstall");

        sections = GauntletOrchestrator.getAdminSections();
        strip.add(SESSION);
        strip.add(CONTROLLER);
        for (var section : sections) {
            if (section.pluginId() != null) strip.add(section.pluginId());
        }
        for (int i = 0; i < strip.size(); i++) {
            cmd.append("#SectionStrip", TAB_BUTTON);
            cmd.set("#SectionStrip[" + i + "].Text", title(strip.get(i)));
            Widgets.bindArg(evt, "#SectionStrip[" + i + "]", "page.selectSection", strip.get(i));
        }

        for (var section : sections) {
            for (var tab : section.tabs()) {
                var i = tabs.size();
                tabs.add(tab);
                cmd.append("#SubStrip", TAB_BUTTON);
                cmd.set("#SubStrip[" + i + "].Text", tab.getTitle());
                cmd.append("#TabBody", tab.getPanel());

                Widgets.bindArg(evt, "#SubStrip[" + i + "]", "page.selectTab", tab.getId());
                tab.bind(evt);
            }
        }
        fillSessionPicker(cmd);
        for (var tab : tabs) {
             tab.buildOnce(cmd, evt, session);
        }
        applyView(cmd);
        renderActive(cmd, evt);
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
            case "page.selectSection" -> selectSection(data.arg());
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
        if (action.startsWith("plugin.")) {
            return pluginAction(action);
        }
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

    /** Installs or uninstalls the plugin of the open section in the selected session. */
    @Nullable
    private Message pluginAction(@Nonnull String action) {
        var section = section(activeSection);
        if (section == null || section.pluginId() == null) return null;
        if (session == null) return Widgets.fail("Pick a session first!");
        var op = action.equals("plugin.install") ? SessionPluginOp.INSTALL : SessionPluginOp.UNINSTALL;
        return SessionTab.plugin(session, op, section.pluginId(), this);
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
                applyView(cmd);
                renderActive(cmd, evt);
            } catch (RuntimeException e) {
                LOGGER.atWarning().withCause(e).log("Admin section %s failed to render; nothing sent", activeSection);
                return;
            }
            sendUpdate(cmd, evt, false);
        }
    }

    /**
     * Shows the open section: its strip button, its tab buttons and the active panel, or a notice when
     * it has nothing to show. Plugin buttons are dimmed while the session does not have the plugin.
     */
    private void applyView(@Nonnull UICommandBuilder cmd) {
        var section = section(activeSection);
        var installed = installed(section);
        if (section == null || !section.tabs().contains(activeTab)) {
            activeTab = section == null || section.tabs().isEmpty() ? null : section.tabs().getFirst();
        }
        var shown = installed && activeTab != null;
        var subTabs = shown && section.tabs().size() > 1;

        for (int i = 0; i < strip.size(); i++) {
            var key = strip.get(i);
            cmd.set("#SectionStrip[" + i + "].Style",
                    key.equals(activeSection) ? TAB_ACTIVE_STYLE : installed(section(key)) ? TAB_STYLE : TAB_OFF_STYLE);
        }
        for (int i = 0; i < tabs.size(); i++) {
            var tab = tabs.get(i);
            cmd.set("#TabBody[" + i + "].Visible", shown && tab == activeTab);
            cmd.set("#SubStrip[" + i + "].Visible", subTabs && section.tabs().contains(tab));
            cmd.set("#SubStrip[" + i + "].Style", tab == activeTab ? TAB_ACTIVE_STYLE : TAB_STYLE);
        }

        var sessionRow = SESSION.equals(activeSection);
        cmd.set("#SessionRow.Visible", sessionRow);
        cmd.set("#SubRow.Visible", sessionRow || subTabs);
        cmd.set("#PluginBar.Visible", shown && section.pluginId() != null);
        cmd.set("#Notice.Visible", !shown);
        cmd.set("#InstallPlugin.Visible", !installed && session != null);
        if (!shown) {
            cmd.set("#NoticeText.Text", notice(section, installed));
        }
    }

    private void renderActive(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        var tab = activeTab;
        if (tab != null && installed(section(activeSection))) {
            tab.render(cmd, evt, session);
        }
    }

    /** The section a strip button opens. The controller is the section of the session's game. */
    @Nullable
    private AdminSection section(@Nonnull String key) {
        var game = gameOf(session);
        for (var section : sections) {
            var match = switch (key) {
                case SESSION -> section.pluginId() == null && section.gameId() == null;
                case CONTROLLER -> section.gameId() != null && section.gameId().equals(game);
                default -> key.equals(section.pluginId());
            };
            if (match) return section;
        }
        return null;
    }

    /** The game being played, or the next one up when nothing is. */
    @Nullable
    private static String gameOf(@Nullable GameSession session) {
        if (session == null) return null;
        return session.getCurrentGame() != null ? session.getCurrentGame() : session.getNext();
    }

    private boolean installed(@Nullable AdminSection section) {
        if (section == null || section.pluginId() == null) return true;
        return session != null && session.getPlugins().contains(section.pluginId());
    }

    @Nonnull
    private String title(@Nonnull String key) {
        if (key.equals(SESSION) || key.equals(CONTROLLER)) return key;
        return SessionText.plugin(key);
    }

    @Nonnull
    private String notice(@Nullable AdminSection section, boolean installed) {
        if (session == null) return "Create or pick a session first";
        if (!installed) return section.title() + " is not installed in " + session.getId();
        if (section == null) return "No game is queued in " + session.getId();
        return section.title() + " has no admin tabs";
    }

    private void selectSection(@Nonnull String key) {
        if (key.equals(activeSection) || !strip.contains(key)) return;
        activeSection = key;
        activeTab = null;
        var cmd = new UICommandBuilder();
        var evt = new UIEventBuilder();
        applyView(cmd);
        renderActive(cmd, evt);
        sendUpdate(cmd, evt, false);
    }

    private void selectTab(@Nonnull String id) {
        var section = section(activeSection);
        if (section == null) return;
        for (var tab : section.tabs()) {
            if (tab.getId().equalsIgnoreCase(id) && tab != activeTab) {
                activeTab = tab;
                var cmd = new UICommandBuilder();
                var evt = new UIEventBuilder();
                applyView(cmd);
                renderActive(cmd, evt);
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
        applyView(cmd);
        renderActive(cmd, evt);
        cmd.set("#ActionStatus.TextSpans", Widgets.ok("Now showing " + session.getId()));
        sendUpdate(cmd, evt, false);
    }

    /**
     * Refills the session picker and shows the given session, or the first one when it is gone. Tabs
     * call this from any thread after creating or destroying a session.
     */
    public void showSession(@Nullable String id) {
        synchronized (lock) {
            var sessions = GauntletUtils.withResource().getSessions();
            var chosen = id == null ? null : sessions.get(id);
            session = chosen != null ? chosen : sessions.values().stream().findFirst().orElse(null);
            var cmd = new UICommandBuilder();
            var evt = new UIEventBuilder();
            fillSessionPicker(cmd);
            for (var tab : tabs) {
                tab.buildOnce(cmd, evt, session);
            }
            applyView(cmd);
            renderActive(cmd, evt);
            sendUpdate(cmd, evt, false);
        }
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
        pendingConfirm = event;
        var target = session == null ? "this session" : session.getId();
        var cmd = new UICommandBuilder();
        cmd.set("#MainPage.Visible", false);
        cmd.set("#ConfirmPage.Visible", true);
        cmd.set("#ConfirmMessage.Text", switch (event.action()) {
            case "match.stop" -> "Stop the match in " + target + " and send everyone back to the lobby?";
            case "match.end" -> "End the live match in " + target + " now and show the standings?";
            case "session.destroy" -> "Destroy " + target + "? Its game is stopped and its parties are let go.";
            case "session.game.stop" -> "Stop the game running in " + target + "?";
            case "session.plugin.uninstall" -> "Uninstall " + SessionText.plugin(event.pick()) + " from " + target + "?";
            case "plugin.uninstall" -> "Uninstall " + title(activeSection) + " from " + target + "?";
            default -> "Go ahead with " + event.action() + " on " + target + "?";
        });
        sendUpdate(cmd, null, false);
    }

    private void closeConfirm(@Nullable Message status) {
        var cmd = new UICommandBuilder();
        var evt = new UIEventBuilder();
        cmd.set("#ConfirmPage.Visible", false);
        cmd.set("#MainPage.Visible", true);
        if (status != null) {
            cmd.set("#ActionStatus.TextSpans", status);
        }
        applyView(cmd);
        renderActive(cmd, evt);
        sendUpdate(cmd, evt, false);
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

