package com.gaiagauntlet.gauntlet.plugins.gamestore.events;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;
import com.gaiagauntlet.gauntlet.core.games.interfaces.PersistentGamePlugin;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.plugins.gamestore.GameStorePlugin;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;

public class GameEventHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void GameEndHandler(GauntletEvent.GameEnd gameEndEvt) {
        var gameId = gameEndEvt.getGameId();
        var gameController = GameRegistry.getGame(gameId).get();
        if (gameController == null)
            return;
        var requiredPlugins = gameController.getRequiredPlugins();
        var requred = requiredPlugins.contains(GameStorePlugin.ID);
        if (!requred)
            return; // not required for this game, do nothing
        var session = GauntletUtils.withResource().getSession(gameEndEvt.getSessionId()).get();
        if (session == null)
            return; // somehow the session is not present? The actual end event handler will deal
                    // with erroring this

        var persistentPlugins = GameRegistry.getPlugins(requiredPlugins, PersistentGamePlugin.class);
        for (var plugin : persistentPlugins) {
            try {
                plugin.writeSession(gameEndEvt.getAccessor(), session, gameEndEvt.getGameId());
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("Error writing %s to session %s from game %s",
                        plugin.getId(), session.getId(), gameId);
                gameEndEvt.Error("Failed to write " + plugin.getId() + "'s session from " + gameId + " with error "
                        + e.getLocalizedMessage());
            }
        }
        gameEndEvt.Message(Message.raw("Finished writing games to session"));
    }
}
