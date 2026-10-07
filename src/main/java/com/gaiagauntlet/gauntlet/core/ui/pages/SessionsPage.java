package com.gaiagauntlet.gauntlet.core.ui.pages;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.events.GauntletEventRegistry;
import com.gaiagauntlet.gauntlet.core.events.events.PlayerGameEvent;
import com.gaiagauntlet.gauntlet.core.party.components.PartyComponent;
import com.gaiagauntlet.gauntlet.core.party.utils.PartyUtils;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.SessionText;
import com.gaiagauntlet.gauntlet.core.ui.events.AdminPageEvent;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * The session browser: every session with its status, game, queue and head count, and a button to
 * move the viewer's party into it or out of it. Joining goes through the same orchestrator event as
 * the join command, so its rules (only a party leader moves the party) apply here too.
 */
public final class SessionsPage extends GauntletPage {

    public static final String ID = "Sessions";

    private static final String PAGE = "Gauntlet/Sessions/SessionsPage.ui";
    private static final String ROW = "Gauntlet/Sessions/SessionRow.ui";
    private static final int UP_NEXT_SHOWN = 4;

    /** What the rows were built for: the session ids in order, then the viewer's session and leadership. */
    @Nullable private List<String> built;

    public SessionsPage(@Nonnull PlayerRef playerRef, @Nullable GameSession session) {
        super(playerRef, 2000);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        cmd.append(PAGE);
        Widgets.bind(evt, "#CloseButton", "page.close");
        built = null;
    }

    @Override
    protected void render(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        var sessions = new ArrayList<>(GauntletUtils.withResource().getSessions().values());
        sessions.sort(Comparator.comparing(GameSession::getId, String.CASE_INSENSITIVE_ORDER));
        var party = PartyUtils.getPartyNullable(playerRef).orElse(null);
        var mine = party == null ? null : PartyUtils.sessionFor(party).orElse(null);
        var leader = party == null || party.getOwner().equals(playerRef.getUuid());

        cmd.set("#YouText.Text", describeYou(party, mine, leader));

        var key = new ArrayList<String>();
        sessions.forEach(s -> key.add(s.getId()));
        key.add(mine == null ? "" : mine.getId());
        key.add(Boolean.toString(leader));
        if (!key.equals(built)) {
            built = key;
            buildRows(cmd, evt, sessions, mine, leader);
        }

        for (int i = 0; i < sessions.size(); i++) {
            var session = sessions.get(i);
            var row = "#SessionList[" + i + "]";
            cmd.set(row + " #State.Text", SessionText.state(session));
            cmd.set(row + " #Now.Text", "Playing " + SessionText.game(session.getCurrentGame()));
            var next = SessionText.upNext(session, UP_NEXT_SHOWN);
            cmd.set(row + " #Next.Text", next.isEmpty() ? "Nothing queued" : "Up next: " + String.join(", ", next));
            var players = GauntletUtils.playersFor(session).size();
            cmd.set(row + " #Count.Text", players + (players == 1 ? " player" : " players"));
        }
    }

    private void buildRows(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nonnull List<GameSession> sessions,
            @Nullable GameSession mine, boolean leader) {
        cmd.clear("#SessionList");
        if (sessions.isEmpty()) {
            cmd.append("#SessionList", Widgets.ROW);
            cmd.set("#SessionList[0].Text", "There are no sessions right now. An admin creates them.");
            return;
        }
        for (int i = 0; i < sessions.size(); i++) {
            var session = sessions.get(i);
            var row = "#SessionList[" + i + "]";
            var joined = mine != null && mine.getId().equals(session.getId());
            cmd.append("#SessionList", ROW);
            cmd.set(row + " #Name.Text", joined ? session.getId() + " (yours)" : session.getId());
            cmd.set(row + " #Action.Text", joined ? "Leave" : "Join");
            cmd.set(row + " #Action.Disabled", !leader);
            Widgets.bindArg(evt, row + " #Action", joined ? "session.leave" : "session.join", session.getId());
        }
    }

    @Nonnull
    private String describeYou(@Nullable PartyComponent party, @Nullable GameSession mine, boolean leader) {
        var where = mine == null ? "not in a session" : "in " + mine.getId();
        if (party == null || party.size() <= 1) {
            return "Playing solo, " + where + ".";
        }
        return "Your party " + party.getLabel() + " (" + party.size() + " players) is " + where + "."
                + (leader ? " You lead it." : " Your party leader picks the session.");
    }

    @Nullable
    @Override
    protected Message handle(@Nonnull AdminPageEvent event) {
        var sessionId = event.arg();
        var request = switch (event.action()) {
            case "session.join" -> PlayerGameEvent.Add(playerRef, sessionId);
            case "session.leave" -> PlayerGameEvent.Remove(playerRef, sessionId);
            default -> null;
        };
        if (request == null) return null;
        GauntletEventRegistry.dispatch(request
                .onMessage(log -> pushStatus(log.toMessage()))
                .onComplete(log -> pushStatus(log.toMessage())));
        return Widgets.ok(event.action().equals("session.join") ? "Joining " + sessionId + "..." : "Leaving " + sessionId + "...");
    }
}
