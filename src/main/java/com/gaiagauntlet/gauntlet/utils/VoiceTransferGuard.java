package com.gaiagauntlet.gauntlet.utils;

import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.player.JoinWorld;
import com.hypixel.hytale.protocol.packets.player.ClientReady;
import com.hypixel.hytale.protocol.packets.voice.VoiceCodec;
import com.hypixel.hytale.protocol.packets.voice.VoiceConfig;
import com.hypixel.hytale.registry.Registration;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher;
import com.hypixel.hytale.server.core.modules.voice.VoiceModule;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/** Disables both client voice directions until the new world is ready for gameplay. */
public final class VoiceTransferGuard {
    private static final long QUIET_MILLIS = 500;
    private static final Object TRANSFERS_LOCK = new Object();
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final ConcurrentHashMap<UUID, Transfer> PAUSED = new ConcurrentHashMap<>();
    private static PacketFilter inbound;
    private static PacketFilter outbound;
    private static Registration voiceInterceptor;

    private VoiceTransferGuard() { }

    public static void setup(JavaPlugin plugin) {
        inbound = PacketAdapters.registerInbound((PlayerPacketWatcher) (player, packet) -> {
            if (!(packet instanceof ClientReady ready)) return;
            synchronized (TRANSFERS_LOCK) {
                var transfer = current(player);
                if (transfer != null
                        && transfer.state.acknowledge(ready.readyForChunks, ready.readyForGameplay)
                        && PAUSED.remove(player.getUuid(), transfer)) {
                    VoiceModule.get().getVoiceRouter().sendVoiceConfig(player);
                    LOGGER.atInfo().log("Restored voice for %s after client gameplay readiness", player.getUsername());
                }
            }
        });
        outbound = PacketAdapters.registerOutbound((PlayerPacketFilter) (player, packet) -> {
            if (!(packet instanceof JoinWorld) && !(packet instanceof VoiceConfig)) return false;
            synchronized (TRANSFERS_LOCK) {
                if (packet instanceof JoinWorld) {
                    var transfer = pause(player);
                    transfer.state.joinSent();
                    sendDisabledConfig(player);
                } else if (packet instanceof VoiceConfig config && config.voiceEnabled && current(player) != null) {
                    // A settings broadcast must not restore voice while this client is loading.
                    sendDisabledConfig(player);
                    return true;
                }
                return false;
            }
        });
        voiceInterceptor = VoiceModule.get().addPlayerVoiceInterceptor(EventPriority.LAST, frame -> {
            if (current(frame.speaker()) != null) {
                frame.drop();
                return;
            }
            for (var id : PAUSED.keySet()) frame.excludeListener(id);
        });
        plugin.getEventRegistry().register(PlayerDisconnectEvent.class, event -> {
            var player = event.getPlayerRef();
            var transfer = current(player);
            if (transfer != null) PAUSED.remove(player.getUuid(), transfer);
        });
    }

    /** Stops voice before the world move and allows old speaking indicators to expire. */
    public static CompletableFuture<Void> quietBeforeTransfer(PlayerRef player) {
        synchronized (TRANSFERS_LOCK) {
            pause(player);
            sendDisabledConfig(player);
        }
        return CompletableFuture.runAsync(() -> { }, CompletableFuture.delayedExecutor(QUIET_MILLIS, TimeUnit.MILLISECONDS));
    }

    /** Restores voice only when a move failed before sending any JoinWorld packet. */
    public static void cancelBeforeJoin(PlayerRef player) {
        synchronized (TRANSFERS_LOCK) {
            var transfer = current(player);
            if (transfer != null && transfer.state.canCancelBeforeJoin() && PAUSED.remove(player.getUuid(), transfer)) {
                VoiceModule.get().getVoiceRouter().sendVoiceConfig(player);
            }
        }
    }

    public static void shutdown() {
        if (inbound != null) PacketAdapters.deregisterInbound(inbound);
        if (outbound != null) PacketAdapters.deregisterOutbound(outbound);
        if (voiceInterceptor != null) voiceInterceptor.unregister();
        // Keep incomplete transfers disabled. A reconnect receives the normal server configuration.
        PAUSED.clear();
    }

    private static Transfer pause(PlayerRef player) {
        return PAUSED.compute(player.getUuid(), (_, previous) ->
                previous != null && previous.player == player ? previous : new Transfer(player, new TransferVoiceState()));
    }

    private static Transfer current(PlayerRef player) {
        var transfer = PAUSED.get(player.getUuid());
        return transfer != null && transfer.player == player ? transfer : null;
    }

    private static void sendDisabledConfig(PlayerRef player) {
        var voice = VoiceModule.get();
        var config = new VoiceConfig();
        config.voiceEnabled = false;
        config.codec = VoiceCodec.Opus;
        config.sampleRate = 48000;
        config.channels = 1;
        config.maxHearingDistance = voice.getMaxHearingDistance();
        config.referenceDistance = voice.getReferenceDistance();
        config.supportsVoiceStream = true;
        config.maxPacketsPerSecond = (byte) voice.getMaxPacketsPerSecond();
        player.getPacketHandler().writeNoCache(config);
    }

    private record Transfer(PlayerRef player, TransferVoiceState state) { }
}
