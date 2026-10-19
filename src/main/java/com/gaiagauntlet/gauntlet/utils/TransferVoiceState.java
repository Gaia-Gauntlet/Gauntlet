package com.gaiagauntlet.gauntlet.utils;

/** Tracks actual client acknowledgements. Server readiness timeouts do not open this gate. */
final class TransferVoiceState {
    private boolean joinSent;
    private boolean chunksAcknowledged;

    synchronized void joinSent() {
        joinSent = true;
        chunksAcknowledged = false;
    }

    synchronized boolean acknowledge(boolean readyForChunks, boolean readyForGameplay) {
        if (!joinSent) return false;
        if (readyForChunks && !readyForGameplay) chunksAcknowledged = true;
        return readyForGameplay && chunksAcknowledged;
    }

    synchronized boolean canCancelBeforeJoin() {
        return !joinSent;
    }
}
