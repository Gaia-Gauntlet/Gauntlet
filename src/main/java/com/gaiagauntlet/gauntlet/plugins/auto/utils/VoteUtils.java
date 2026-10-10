package com.gaiagauntlet.gauntlet.plugins.auto.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.events.GauntletEventRegistry;
import com.gaiagauntlet.gauntlet.core.events.events.SessionQueueEvent;
import com.gaiagauntlet.gauntlet.core.events.events.SessionQueueEvent.SessionQueueOp;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.orchestrator.GauntletOrchestrator;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.SessionText;
import com.gaiagauntlet.gauntlet.plugins.auto.components.VoteComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * Runs a session's vote on its next game. A vote opens on the end screen, every player in the
 * session gets the vote page, and when it closes the winner moves to the front of the session's game
 * sequence through the orchestrator. Changes are applied on the hub thread.
 */
public final class VoteUtils {

    /** The page every player in the session is shown when a vote opens. */
    public static final String PAGE_ID = "Vote";

    private static final Random RANDOM = new Random();

    private VoteUtils() {
    }

    @Nullable
    public static VoteComponent get(@Nullable GameSession session) {
        var type = VoteComponent.getComponentType();
        if (session == null || type == null) return null;
        return GameStore.withHubStore(session.getId()).flatMap(store -> store.get(type)).orElse(null);
    }

    public static boolean isOpen(@Nullable GameSession session) {
        var vote = get(session);
        return vote != null && !vote.isClosed();
    }

    /** Every registered game, by display name. */
    @Nonnull
    public static List<String> allGames() {
        var games = new ArrayList<>(GameRegistry.getGameIds());
        games.sort(Comparator.comparing(SessionText::game, String.CASE_INSENSITIVE_ORDER));
        return games;
    }

    /** Opens a vote over the given games for the given seconds and shows it to everyone in the session. */
    public static void open(@Nonnull GameSession session, @Nonnull List<String> options, int seconds) {
        onHub(() -> {
            var vote = new VoteComponent(options, System.currentTimeMillis() + seconds * 1000L);
            GameStore.ensureHubStore(session.getId()).put(VoteComponent.getComponentType(), vote);
            for (var player : GauntletUtils.playersFor(session)) {
                showPage(player, session);
            }
        });
    }

    /** Closes the session's open vote now. */
    public static void closeNow(@Nonnull GameSession session) {
        onHub(() -> close(session));
    }

    public static void close(GameSession sessionId) {} // TODO: Fix - though all of this may have to go. I'm just making it compile again
    public static void close(VoteComponent vote, String sessionId) {
        var session = GauntletUtils.sessionFor(sessionId).orElse(null);
        if (session == null) {
            return;
        }
        if (vote == null || vote.isClosed()) return;
        var winner = vote.leader(RANDOM);
        vote.close(winner);

        if (winner == null) {
            tell(session, Message.raw("Nobody voted, so the next game stays as it was.").color("#878e9c"));
            return;
        }
        var votes = vote.tally().get(winner);
        tell(session, Message.raw("Up next: " + SessionText.game(winner) + " with " + votes + (votes == 1 ? " vote" : " votes"))
                .color("#E8A93B").bold(true));

        if (winner.equals(session.getNext())) return;
        var sequence = new ArrayList<>(session.getGameSequence());
        sequence.remove(winner);
        sequence.addFirst(winner);
        GauntletEventRegistry.dispatch(new SessionQueueEvent(SessionQueueOp.SET, session.getId(), sequence));
    }

    private static void tell(@Nonnull GameSession session, @Nonnull Message message) {
        for (var player : GauntletUtils.playersFor(session)) {
            player.sendMessage(message);
        }
    }

    /** Pages open on the player's own world thread. */
    private static void showPage(@Nonnull PlayerRef player, @Nonnull GameSession session) {
        var ref = player.getReference();
        if (ref == null || !ref.isValid()) return;
        var store = ref.getStore();
        GauntletUtils.run(store.getExternalData().getWorld(), () -> {
            if (ref.isValid()) {
                GauntletOrchestrator.openPage(store, ref, player, PAGE_ID, session);
            }
        });
    }

    private static void onHub(@Nonnull Runnable task) {
        GauntletUtils.run(GauntletUtils.withHubWorld(), task);
    }
}
