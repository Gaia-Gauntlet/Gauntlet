package com.gaiagauntlet.gauntlet.plugins.gamestate.components;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MatchComponentTest {

    @Test
    void countdownRoundsUpAndExpiresAtZero() {
        var match = new MatchComponent();
        assertFalse(match.hasCountdown());

        match.startCountdown(10, 1_000);
        assertTrue(match.hasCountdown());
        assertEquals(10, match.remainingSeconds(1_000));
        assertEquals(10, match.remainingSeconds(1_001));
        assertEquals(1, match.remainingSeconds(10_999));
        assertFalse(match.isExpired(10_999));
        assertEquals(0, match.remainingSeconds(11_000));
        assertTrue(match.isExpired(11_000));
    }

    @Test
    void pauseHoldsTheRemainingTimeUntilResumed() {
        var match = new MatchComponent();
        match.startCountdown(10, 0);
        match.pause(4_000);

        assertTrue(match.isPaused());
        assertEquals(6, match.remainingSeconds(60_000));
        assertFalse(match.isExpired(60_000));

        match.resume(60_000);
        assertFalse(match.isPaused());
        assertEquals(6, match.remainingSeconds(60_000));
        assertTrue(match.isExpired(66_000));
    }

    @Test
    void clearingRemovesBothRunningAndPausedCountdowns() {
        var match = new MatchComponent();
        match.startCountdown(10, 0);
        match.pause(1_000);
        match.clearCountdown();

        assertFalse(match.hasCountdown());
        assertFalse(match.isPaused());
        assertFalse(match.isExpired(100_000));
    }
}
