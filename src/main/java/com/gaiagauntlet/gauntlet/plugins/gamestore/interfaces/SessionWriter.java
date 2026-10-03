package com.gaiagauntlet.gauntlet.plugins.gamestore.interfaces;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;

/**
 * Runs on the Hub thread, applies the changes to the GameSession
 */
public interface SessionWriter {
    void apply(GameSession session);
}
