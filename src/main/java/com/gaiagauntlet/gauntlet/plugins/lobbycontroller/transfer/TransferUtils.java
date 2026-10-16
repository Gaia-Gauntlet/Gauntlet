package com.gaiagauntlet.gauntlet.plugins.lobbycontroller.transfer;

import com.gaiagauntlet.gauntlet.core.session.constants.SessionState;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.config.GauntletConfig;
import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.msg;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.components.PlayerMarker;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import it.unimi.dsi.fastutil.Pair;

public class TransferUtils {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Queues a player to join, will be added on the next batch */
    public static void queue(ComponentAccessor<EntityStore> originAccessor, World destination, String sessionId,
            List<Pair<PlayerRef, CompletableFuture<Transform>>> playersSets) {

        var origin = originAccessor.getExternalData().getWorld();

        if (!origin.isInThread()) {
            GaiaLog.atError().log("In the wrong thread to move players! Must be in the Hub Thread");
        }

        // if this were an individual plugin, it would have it's own config.
        // it is not it's own plugin, thus it uses the global config :P
        var cfg = GauntletConfig.get();

        // process batch
        int index = 0;
        for (var playerPair : playersSets) {
            var playerRef = playerPair.first();
            var transformFuture = playerPair.second();
            // move the players one at a time
            long delay = (index / cfg.getBatchSize()) * cfg.getBatchDelay();
            // run the delay on the destination thread so, if it slows down or dies, we
            // don't dos it
            destination.scheduleAfter(() -> {
                transformFuture.whenComplete((transform, err) -> {

                    if (err != null) {
                        GaiaLog.atError(err).withSession(sessionId)
                                .log(msg("server.gg.plugins.transfer.error.world")
                                        .param("player", playerRef.getUsername())
                                        .param("world", destination.getName())
                                        .param("reason", MessageUtils.reason(err)));
                        return;
                    }

                    var ref = playerRef.getReference();
                    if (ref == null) {
                        GaiaLog.atWarning().withSession(sessionId)
                                .log(msg("server.gg.plugins.transfer.error.world")
                                        .param("player", playerRef.getUsername())
                                        .param("world", destination.getName())
                                        .param("reason", "player is between worlds"));
                        return;
                    }
                    var refStore = ref.getStore();
                    var refWorld = refStore.getExternalData().getWorld();
                    // hop onto the player's world thread so their state is read from their thread
                    GauntletUtils.run(refWorld, () -> {

                        if (!playerRef.isValid()) {
                            // player is no longer valid :/
                            GaiaLog.atInfo().withSession(sessionId)
                                    .log(msg("server.gg.plugins.transfer.error.world")
                                            .param("player", playerRef.getUsername())
                                            .param("world", destination.getName())
                                            .param("reason", "player no longer being online or valid"));
                            return;
                        }

                        // ref.validate throws if in the wrong store :/
                        if (ref == null || !ref.isValid()) {
                            // ref is no longer in the origin world or is not valid - invalid state, end.
                            GaiaLog.atInfo().withSession(sessionId)
                                    .log(msg("server.gg.plugins.transfer.error.world")
                                            .param("player", playerRef.getUsername())
                                            .param("world", destination.getName())
                                            .param("reason", "player ref is no longer valid"));
                            return;
                        }

                        var reason = !destination.isAlive() ? "the arena has closed"
                                : !GauntletUtils.sessionFor(playerRef).map(session -> session.getId().equals(sessionId))
                                        .orElse(false) ? "they are no longer in the session"
                                : GauntletUtils.sessionFor(sessionId).map(GameSession::getSessionState)
                                        .orElse(null) != SessionState.RUNNING ? "the game is no longer running" : null;
                        if (reason != null) {
                            GaiaLog.atInfo().withSession(sessionId)
                                    .log(msg("server.gg.plugins.transfer.error.world")
                                            .param("player", playerRef.getUsername())
                                            .param("world", destination.getName())
                                            .param("reason", reason));
                            return;
                        }

                        var marker = new PlayerMarker(sessionId, destination.getWorldConfig().getUuid());
                        PlayerUtils.moveToWorld(playerRef, destination, transform,
                                holder -> holder.putComponent(PlayerMarker.getComponentType(), marker))
                                .whenComplete((_, error) -> logMove(playerRef.getUsername(), sessionId,
                                        destination.getName(), error));
                    });
                });
            }, delay, TimeUnit.SECONDS);
            index++;
        }
    }

    private static void logMove(String playerName, String sessionId, String worldName, Throwable error) {
        if (error != null) {
            GaiaLog.atError(error).withSession(sessionId).log(msg("server.gg.plugins.transfer.failed")
                    .param("player", playerName)
                    .param("world", worldName)
                    .param("cause", MessageUtils.reason(error)));
            return;
        }
        GaiaLog.atInfo().withSession(sessionId).log(msg("server.gg.plugins.transfer.success")
                .param("player", playerName)
                .param("world", worldName));
    }
}
