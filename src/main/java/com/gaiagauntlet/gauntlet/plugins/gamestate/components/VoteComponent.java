package com.gaiagauntlet.gauntlet.plugins.gamestate.components;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.session.components.SessionComponent;
import com.gaiagauntlet.gauntlet.core.session.components.SessionComponentType;

import lombok.Getter;
import lombok.Setter;

/**
 * A session's vote on its next game: the games on offer, one vote per player, and when it closes.
 * After it closes it keeps the winner so the end screen can show it. Not saved, since a vote only
 * lives for one end screen.
 */
public final class VoteComponent implements SessionComponent {

    public static final String ID = "VoteComponent";

    @Getter @Setter private static SessionComponentType<VoteComponent> sessionComponentType;

    @Getter private final List<String> options;
    private final Map<UUID, String> votes = new ConcurrentHashMap<>();
    @Getter private final long endsAt;

    /** The game that won, or null while the vote is open or when nobody voted. */
    @Getter @Nullable private volatile String winner;
    @Getter private volatile boolean closed;

    public VoteComponent(@Nonnull List<String> options, long endsAt) {
        this.options = List.copyOf(options);
        this.endsAt = endsAt;
    }

    public int remainingSeconds(long now) {
        return closed ? 0 : (int) Math.max(0, (endsAt - now + 999) / 1000);
    }

    /** Records or changes a player's vote. Returns false when the vote is closed or the game is not on offer. */
    public boolean cast(@Nonnull UUID player, @Nonnull String gameId) {
        if (closed || !options.contains(gameId)) return false;
        votes.put(player, gameId);
        return true;
    }

    @Nullable
    public String voteOf(@Nonnull UUID player) {
        return votes.get(player);
    }

    /** Votes per option, in option order, with zero for options nobody picked. */
    @Nonnull
    public Map<String, Integer> tally() {
        var tally = new HashMap<String, Integer>();
        options.forEach(option -> tally.put(option, 0));
        votes.values().forEach(vote -> tally.merge(vote, 1, Integer::sum));
        return tally;
    }

    /** The option with the most votes, a random one of them on a tie, or null when nobody voted. */
    @Nullable
    public String leader(@Nonnull Random random) {
        var tally = tally();
        int best = tally.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        if (best == 0) return null;
        var tied = options.stream().filter(option -> tally.get(option) == best).toList();
        return tied.get(random.nextInt(tied.size()));
    }

    public int totalVotes() {
        return votes.size();
    }

    public void close(@Nullable String winner) {
        this.winner = winner;
        this.closed = true;
    }
}
