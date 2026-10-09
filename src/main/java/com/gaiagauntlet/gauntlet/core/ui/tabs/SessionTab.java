package com.gaiagauntlet.gauntlet.core.ui.tabs;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.events.GauntletEventRegistry;
import com.gaiagauntlet.gauntlet.core.events.events.NewSessionEvent;
import com.gaiagauntlet.gauntlet.core.events.events.SessionEvent;
import com.gaiagauntlet.gauntlet.core.events.events.SessionPluginEvent;
import com.gaiagauntlet.gauntlet.core.events.events.SessionQueueEvent;
import com.gaiagauntlet.gauntlet.core.events.events.SessionEvent.SessionOperation;
import com.gaiagauntlet.gauntlet.core.events.events.SessionPluginEvent.SessionPluginOp;
import com.gaiagauntlet.gauntlet.core.events.events.SessionQueueEvent.SessionQueueOp;
import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.party.utils.PartyUtils;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.SessionText;
import com.gaiagauntlet.gauntlet.core.ui.events.AdminPageEvent;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.core.ui.pages.AdminPage;
import com.gaiagauntlet.gauntlet.core.ui.pages.Widgets;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.error;
import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.msg;

public final class SessionTab implements AdminTab {
    public SessionTab() {
    }

    @Override
    public @NonNull String getId() {
        return "Session";
    }

    @Override
    public @NonNull String getPanel() {
        return "Gauntlet/Admin/Panels/PanelSession.ui";
    }

    @Override
    public void bind(@NonNull UIEventBuilder evt) {
        Widgets.bindValues(evt, "#CreateSession", "session.create", "", Map.of("@Text", "#NewSessionId.Value"));
        Widgets.bind(evt, "#DestroySession", "session.destroy");

        Widgets.bind(evt, "#SetupGame", "session.game.setup");
        Widgets.bind(evt, "#StopGame", "session.game.stop");

        Widgets.bindValues(evt, "#AddGame", "session.queue.add", "", Map.of("@Pick", "#GamePicker.Value"));
        Widgets.bindValues(evt, "#PlayNext", "session.queue.next", "", Map.of("@Pick", "#GamePicker.Value"));
        Widgets.bindValues(evt, "#RemoveGame", "session.queue.remove", "", Map.of("@Pick", "#GamePicker.Value"));

        Widgets.bindValues(evt, "#AddPlugin", "session.plugin.install", "", Map.of("@Pick", "#PluginPicker.Value"));
        Widgets.bindValues(evt, "#RemovePlugin", "session.plugin.uninstall", "", Map.of("@Pick", "#PluginPicker.Value"));
    }

    @Override
    public void buildOnce(@NonNull UICommandBuilder cmd, @NonNull UIEventBuilder evt, @Nullable GameSession session) {
        var games = new ArrayList<>(GameRegistry.getGameIds());
        games.sort(Comparator.comparing(SessionText::game, String.CASE_INSENSITIVE_ORDER));
        Widgets.fillPicker(cmd, "#GamePicker",
                games.stream().map(id -> new Widgets.Option(SessionText.game(id), id)).toList());

        var plugins = new ArrayList<>(GameRegistry.getPlugins(GamePlugin.class));
        plugins.sort(Comparator.comparing(GamePlugin::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        Widgets.fillPicker(cmd, "#PluginPicker",
                plugins.stream().map(plugin -> new Widgets.Option(plugin.getDisplayName(), plugin.getId())).toList());
    }

    @Override
    public void render(@NonNull UICommandBuilder cmd, @NonNull UIEventBuilder evt, GameSession session) {
        Widgets.field(cmd, "SessionField", Objects.isNull(session) ? "N/A" : session.getId());
        Widgets.field(cmd, "GameField", Objects.isNull(session) ? "N/A" : SessionText.game(session.getCurrentGame()));
        Widgets.field(cmd, "StateField", Objects.isNull(session) ? "N/A" : session.getSessionState().name());
        Widgets.field(cmd, "ErrorField", Objects.isNull(session) || session.getErrorReason() == null ? "none" : session.getErrorReason());
        Widgets.field(cmd, "PlayersField", Objects.isNull(session) ? "N/A" : Integer.toString(GauntletUtils.playersFor(session).size()));

        Widgets.fillList(cmd, "GameList",
                Objects.isNull(session) ? List.of() : queue(session),
                "No games added yet...");
        Widgets.fillList(cmd, "PluginList",
                Objects.isNull(session) ? List.of() : plugins(session),
                "No plugins installed");
        Widgets.fillList(cmd, "PartyList",
                Objects.isNull(session) ? List.of() : parties(session),
                "No parties in this session");
    }

    @Override
    public @Nullable Message handle(@NonNull String action, @NonNull AdminPageEvent event,
            @Nullable GameSession session, @NonNull AdminPage page) {
        if (action.equals("session.create")) {
            return sessionCreate(event.text(), page);
        }
        if (Objects.isNull(session)) {
            return error("Pick a session first!");
        }
        return switch (action) {
            case "session.destroy" -> sessionDestroy(session, page);
            case "session.game.setup" -> gameSetup(session, page);
            case "session.game.stop" -> gameStop(session, page);
            case "session.queue.add" -> queue(session, SessionQueueOp.APPEND, List.of(event.pick()), page);
            case "session.queue.remove" -> queue(session, SessionQueueOp.REMOVE, List.of(event.pick()), page);
            case "session.plugin.install" -> plugin(session, SessionPluginOp.INSTALL, event.pick(), page);
            case "session.plugin.uninstall" -> plugin(session, SessionPluginOp.UNINSTALL, event.pick(), page);
            case "session.queue.next" -> {
                var sequence = new ArrayList<>(session.getGameSequence());
                sequence.remove(event.pick());
                sequence.addFirst(event.pick());
                yield queue(session, SessionQueueOp.SET, sequence, page);
            }
            default -> Message.raw("Session tab received unknown action " + action).color(Color.RED);
        };
    }

    private Message sessionCreate(@NonNull String sessionId, AdminPage page) {
        if (sessionId.isEmpty()) {
            return error("Type an id for the new session!");
        }
        if (GauntletUtils.sessionFor(sessionId).isPresent()) {
            return error("A session called " + sessionId + " already exists!");
        }
        GauntletEventRegistry.dispatch(
                new NewSessionEvent(new GameSession(sessionId))
                        .onMessage(msg -> page.pushStatus(msg.toMessage()))
                        .onComplete(message -> {
                            page.showSession(sessionId);
                            page.pushStatus(message.toMessage());
                        }));
        return msg("server.gg.commands.session.create.pending").param("sessionId", sessionId);
    }

    private Message sessionDestroy(@NonNull GameSession session, AdminPage page) {
        var sessionId = session.getId();
        GauntletEventRegistry.dispatch(
                new SessionEvent(SessionOperation.DELETE, sessionId)
                        .onMessage(msg -> page.pushStatus(msg.toMessage()))
                        .onComplete(message -> {
                            page.showSession(null);
                            page.pushStatus(message.toMessage());
                        }));
        return msg("server.gg.commands.session.destroy.pending").param("sessionId", sessionId);
    }

    private Message gameSetup(@NonNull GameSession session, AdminPage page) {
        var sessionId = session.getId();
        if (session.getNext() == null) {
            return error("The queue is empty, so add a game first!");
        }

        GauntletEventRegistry.dispatch(
                new SessionEvent(SessionOperation.SETUP, sessionId)
                        .onMessage(msg -> page.pushStatus(msg.toMessage()))
                        .onComplete(message -> page.pushStatus(message.toMessage())));

        return msg("server.gg.commands.session.setup.pending").param("sessionId", sessionId);
    }

    private Message gameStop(@NonNull GameSession session, AdminPage page) {
        var sessionId = session.getId();
        GauntletEventRegistry.dispatch(
                new SessionEvent(SessionOperation.CLEAN, sessionId)
                        .onMessage(msg -> page.pushStatus(msg.toMessage()))
                        .onComplete(message -> page.pushStatus(message.toMessage())));
        return msg("server.gg.commands.session.cleanup.pending").param("sessionId", sessionId);
    }

    private Message queue(@NonNull GameSession session, SessionQueueOp op, List<String> games, AdminPage page) {
        if (games.isEmpty() || games.getFirst().isEmpty()) {
            return error("Pick a game first!");
        }
        GauntletEventRegistry.dispatch(
                new SessionQueueEvent(op, session.getId(), games)
                        .onMessage(msg -> page.pushStatus(msg.toMessage()))
                        .onComplete(message -> page.pushStatus(message.toMessage())));
        return Widgets.ok("Updating the queue...");
    }

    /** Installs or uninstalls a plugin in the session through the orchestrator. */
    public static Message plugin(@NonNull GameSession session, SessionPluginOp op, String pluginId, AdminPage page) {
        if (pluginId.isEmpty()) {
            return error("Pick a plugin first!");
        }
        GauntletEventRegistry.dispatch(
                new SessionPluginEvent(op, session.getId(), pluginId)
                        .onMessage(msg -> page.pushStatus(msg.toMessage()))
                        .onComplete(message -> page.pushStatus(message.toMessage())));
        return Widgets.ok("Updating the plugins...");
    }

    /** The queued games in order, numbered. */
    private static List<String> queue(@NonNull GameSession session) {
        var rows = new ArrayList<String>();
        for (var game : session.getGameSequence()) {
            rows.add((rows.size() + 1) + ". " + SessionText.game(game));
        }
        return rows;
    }

    /** The installed plugins by name, marking the ones the session's game needs. */
    private static List<String> plugins(@NonNull GameSession session) {
        var game = session.getCurrentGame() != null ? session.getCurrentGame() : session.getNext();
        var required = GameRegistry.getGame(game)
                .map(controller -> GameRegistry.withDependencies(controller.getRequiredPlugins()))
                .orElse(Set.of());
        var rows = new ArrayList<String>();
        for (var pluginId : session.getPlugins()) {
            rows.add(SessionText.plugin(pluginId) + (required.contains(pluginId) ? " (needed by " + SessionText.game(game) + ")" : ""));
        }
        rows.sort(String.CASE_INSENSITIVE_ORDER);
        return rows;
    }

    /** Each party in the session with its leader and how many of its players are online. */
    private static List<String> parties(@NonNull GameSession session) {
        var rows = new ArrayList<String>();
        for (var partyId : session.getParties()) {
            var party = PartyUtils.getParty(partyId).orElse(null);
            if (party == null) {
                rows.add(partyId + " (missing)");
                continue;
            }
            var leader = PlayerUtils.resolveOnline(party.getOwner());
            rows.add(party.getLabel() + ": " + party.getAllOnlinePlayers().size() + " of " + party.size()
                    + " online, led by " + (leader == null ? "an offline player" : leader));
        }
        rows.sort(String.CASE_INSENSITIVE_ORDER);
        return rows;
    }
}
