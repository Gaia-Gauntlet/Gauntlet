package com.gaiagauntlet.gauntlet.core.ui;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GameController;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;

/** How sessions and their games read to players. */
public final class SessionText {

    private SessionText() {
    }

    /** The game's display name, its id when it is not registered, or "Nothing" for no game. */
    @Nonnull
    public static String game(@Nullable String gameId) {
        if (gameId == null || gameId.isEmpty()) return "Nothing";
        return GameRegistry.getGame(gameId).map(GameController::getDisplayName).orElse(gameId);
    }

    @Nonnull
    public static String state(@Nonnull GameSession session) {
        return switch (session.getSessionState()) {
            case IDLE -> "Waiting";
            case SETTING_UP -> "Setting up";
            case RUNNING -> "Playing";
            case CLEANING -> "Wrapping up";
            case FINISHED -> "Finished";
            case ERROR -> "Something went wrong";
        };
    }

    /** The display names of the next games in the sequence, at most the given count. */
    @Nonnull
    public static List<String> upNext(@Nonnull GameSession session, int count) {
        return session.getGameSequence().stream().limit(count).map(SessionText::game).toList();
    }
}
