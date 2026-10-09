package com.gaiagauntlet.gauntlet.plugins.gamestore.events;

import java.util.ArrayList;
import java.util.List;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.events.events.GameEndEvent;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.plugins.gamestore.GameStorePlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestore.interfaces.PersistentGamePlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestore.interfaces.SessionWriter;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.hypixel.hytale.logger.HytaleLogger;

public class GameEventHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void GameEndHandler(GameEndEvent evt) {
        var gameId = evt.getGameId();
        var sessionId = evt.getSessionId();

        if (!(GauntletUtils.sessionFor(sessionId).orElse(null) instanceof GameSession session))
            return; // session does not exist - will be handled later

        var installedPlugins = List.copyOf(session.getPlugins());
        if (!installedPlugins.contains(GameStorePlugin.ID))
            return; // not installed for this session, do nothing

        var persistentPlugins = GameRegistry.getPlugins(installedPlugins, PersistentGamePlugin.class);

        var gameWorld = evt.getGameWorld();

        evt.defer(GauntletUtils.runAsync(gameWorld, () -> {
            var writes = new ArrayList<SessionWriter>();
            var gameEcs = GameStore.ensureStore(gameWorld, sessionId);
            for (var plugin : persistentPlugins) {
                try {
                    var writer = plugin.capture(gameWorld, gameEcs, sessionId);
                    if (writer != null) {
                        writes.add(writer);
                    }
                } catch (Exception e) {
                    evt.log(GaiaLog.atWarning().withSession(session).withCause(e)
                            .log(plugin.getId() + " plugin failed to read state from " + gameId + " with error "
                                    + e.getLocalizedMessage()));
                }
            }
            return writes;
        }).thenCompose(sessionWrites -> GauntletUtils.runAsync(GauntletUtils.withHubWorld(), () -> {
            for (var writer : sessionWrites) {
                try {
                    writer.apply(session);
                } catch (Exception e) {
                    evt.log(GaiaLog.atWarning().withSession(session).withCause(e)
                            .log("Failed to write session from " + gameId + " with error "
                                    + e.getLocalizedMessage()));
                }
            }
        })));
        evt.complete(GaiaLog.atInfo().withSession(session).log("Finished writing games to session"));
    }
}
