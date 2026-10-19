package com.gaiagauntlet.gauntlet.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TransferVoiceStateTest {
    @Test
    void doesNotRestoreBeforeJoinOrFromGameplayAlone() {
        var state = new TransferVoiceState();
        assertFalse(state.acknowledge(true, true));
        state.joinSent();
        assertFalse(state.acknowledge(true, true));
    }

    @Test
    void staysSilentUntilBothLoadingPhasesComplete() {
        var state = new TransferVoiceState();
        state.joinSent();
        assertFalse(state.acknowledge(true, false));
        assertFalse(state.acknowledge(false, false));
        assertTrue(state.acknowledge(false, true));
    }

    @Test
    void rescueJoinRequiresANewChunkAcknowledgement() {
        var state = new TransferVoiceState();
        state.joinSent();
        state.acknowledge(true, false);
        state.joinSent();
        assertFalse(state.acknowledge(true, true));
        assertFalse(state.acknowledge(true, false));
        assertTrue(state.acknowledge(true, true));
    }

    @Test
    void onlyCancelsBeforeTheClientWasAskedToMove() {
        var state = new TransferVoiceState();
        assertTrue(state.canCancelBeforeJoin());
        state.joinSent();
        assertFalse(state.canCancelBeforeJoin());
    }
}
