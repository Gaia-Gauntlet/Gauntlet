package com.gaiagauntlet.gauntlet.plugins.transfer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.config.GauntletConfig;
import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.msg;
import com.gaiagauntlet.gauntlet.plugins.gamestore.GameStorePlugin;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class TransferPlugin implements GamePlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "TransferPlugin";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<String> getDependencies() {
        return List.of(GameStorePlugin.ID);
    }

    @Override
    public void init(JavaPlugin host) {
        // not sure if this is needed tbh - waiting to hear from melodic as to WHY these are being tracked
        // TransferComponent
        //         .setComponentType(GameComponentRegistry.register(TransferComponent.ID, TransferComponent.class));

    }

    /** Queues a player to join, will be added on the next batch */
    public static void queue(ComponentAccessor<EntityStore> originAccessor, World destination, String sessionId,
            Set<PlayerRef> players, Transform location) {

        // var hubEcs = GameStore.ensureStore(originAccessor, sessionId);

        // var transferComponent = hubEcs.ensure(TransferComponent.getComponentType(),
        //         () -> new TransferComponent(destination));

        var origin = originAccessor.getExternalData().getWorld();

        if (!origin.isInThread()) {
            GaiaLog.atError().log("In the wrong thread to move players! Must be in the Hub Thread");
        }

        // if this were an individual plugin, it would have it's own config.
        // it is not it's own plugin, thus it uses the global config :P
        var cfg = GauntletConfig.get();

        // add all players into a batch
        // var batch = new BatchItem(destination);
        // transferComponent.add(batch);

        // process batch
        int index = 0;
        for (var playerRef : players) {
            // move the players one at a time
            long delay = (index / cfg.getBatchSize()) * cfg.getBatchDelay();
            // run the delay on the destination thread so, if it slows down or dies, we
            // don't dos it
            destination.scheduleAfter(() -> {
                // immediately hop back to the origin thread to add the component (rip lol)
                GauntletUtils.run(origin, () -> {

                    var warpComponent = Teleport.createForPlayer(destination, location);

                    if (!playerRef.isValid()) {
                        // player is no longer valid :/
                        GaiaLog.atInfo().withSession(sessionId)
                                .log(msg("server.gg.plugins.transfer.error.world")
                                        .param("player", playerRef.getUsername())
                                        .param("reason", "player no longer being online or valid"));
                        return;
                    }

                    // literally just logging right now
                    warpComponent.setOnComplete(moveComplete(playerRef.getUsername(), sessionId));
                    
                    var ref = playerRef.getReference();
                    // ref.validate throws if in the wrong store :/
                    if (ref == null || !ref.isValid()
                            || !playerRef.getWorldUuid().equals(origin.getWorldConfig().getUuid())) {
                        // ref is no longer in the origin world or is not valid - invalid state, end.
                        GaiaLog.atInfo().withSession(sessionId)
                                .log(msg("server.gg.plugins.transfer.error.world")
                                        .param("player", playerRef.getUsername())
                                        .param("reason", "player no longer being in hub"));
                        return;
                    }

                    // put the teleport component
                    originAccessor.putComponent(ref, Teleport.getComponentType(), warpComponent);
                });
            }, delay, TimeUnit.SECONDS);
            index++;
        }
    }

    // all this does for now is logging for visibility
    private static CompletableFuture<Void> moveComplete(String playerName, String sessionId) {
        var future = new CompletableFuture<Void>();
        future.whenComplete((_, error) -> {
            if (error != null) {
                GaiaLog.atError(error).withSession(sessionId).log(msg("server.gg.plugins.transfer.failed")
                        .param("player", playerName)
                        .param("cause", error.getLocalizedMessage()));
                return;
            }
            GaiaLog.atInfo().withSession(sessionId).log(msg("server.gg.plugins.transfer.success")
                    .param("player", playerName));
        });
        return future;
    }
}
