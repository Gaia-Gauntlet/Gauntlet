package com.gaiagauntlet.gauntlet.plugins.gamestate.components;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class VoteComponentTest {

    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();
    private static final UUID C = UUID.randomUUID();

    @Test
    void nobodyVotingHasNoLeader() {
        var vote = new VoteComponent(List.of("EZ", "Race"), 0);
        assertNull(vote.leader(new Random(1)));
        assertEquals(0, vote.tally().get("EZ"));
    }

    @Test
    void mostVotesLeadsAndChangingAVoteMovesIt() {
        var vote = new VoteComponent(List.of("EZ", "Race"), 0);
        vote.cast(A, "EZ");
        vote.cast(B, "Race");
        vote.cast(C, "Race");
        assertEquals("Race", vote.leader(new Random(1)));

        vote.cast(C, "EZ");
        assertEquals(2, vote.tally().get("EZ"));
        assertEquals(3, vote.totalVotes());
        assertEquals("EZ", vote.leader(new Random(1)));
    }

    @Test
    void tiesPickAmongTheTiedOptionsOnly() {
        var vote = new VoteComponent(List.of("EZ", "Race", "Parkour"), 0);
        vote.cast(A, "EZ");
        vote.cast(B, "Race");
        var seen = new HashSet<String>();
        var random = new Random(7);
        for (int i = 0; i < 50; i++) seen.add(vote.leader(random));
        assertEquals(Set.of("EZ", "Race"), seen);
    }

    @Test
    void closedVotesAndUnknownGamesRejectVotes() {
        var vote = new VoteComponent(List.of("EZ"), 0);
        assertFalse(vote.cast(A, "Race"));
        vote.close("EZ");
        assertFalse(vote.cast(A, "EZ"));
        assertTrue(vote.isClosed());
        assertEquals("EZ", vote.getWinner());
        assertEquals(0, vote.remainingSeconds(-10_000));
    }
}
