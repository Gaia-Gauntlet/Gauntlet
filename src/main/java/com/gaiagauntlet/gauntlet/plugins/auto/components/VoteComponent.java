package com.gaiagauntlet.gauntlet.plugins.auto.components;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.core.session.components.SessionComponent;
import com.gaiagauntlet.gauntlet.core.session.components.SessionComponentType;
import com.gaiagauntlet.gauntlet.core.session.registry.SessionRegistry;

import lombok.Getter;

public final class VoteComponent implements SessionComponent {

    public static final String ID = "VoteComponent";

    @Getter
    private static SessionComponentType<@NotNull VoteComponent> componentType = SessionRegistry
            .register(VoteComponent.ID, VoteComponent.class);

    @Getter
    private final List<String> options;
    private final Map<UUID, String> votes = new ConcurrentHashMap<>();

    /** The game that won, or null while the vote is open or when nobody voted. */
    @Getter
    @Nullable
    private volatile String winner;
    @Getter
    private volatile boolean closed;

    public VoteComponent(@Nonnull List<String> options) {
        this.options = List.copyOf(options);
    }

    public boolean cast(@Nonnull UUID player, @Nonnull String gameId) {
        if (closed || !options.contains(gameId))
            return false;
        votes.put(player, gameId);
        return true;
    }

    @Nullable
    public String voteOf(@Nonnull UUID player) {
        return votes.get(player);
    }

    @Nonnull
    public Map<String, Integer> tally() {
        var tally = new HashMap<String, Integer>();
        options.forEach(option -> tally.put(option, 0));
        votes.values().forEach(vote -> tally.merge(vote, 1, Integer::sum));
        return tally;
    }

    @Nullable
    public String leader(@Nonnull Random random) {
        var tally = tally();
        int best = tally.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        if (best == 0)
            return null;
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
