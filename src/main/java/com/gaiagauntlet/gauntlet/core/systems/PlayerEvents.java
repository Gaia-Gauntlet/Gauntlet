package com.gaiagauntlet.gauntlet.core.systems;

import javax.annotation.Nonnull;

import com.gaiagauntlet.gauntlet.core.GauntletOrchestrator;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class PlayerEvents {

    /** Fires each time a player arrives in a world, so the HUD is sent again after every world change */
    public static void onPlayerReady(@Nonnull PlayerReadyEvent event) {
        var ref = event.getPlayerRef();
        var store = ref.getStore();
        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;

        GauntletOrchestrator.showHud(store, ref, playerRef);
    }

    public static void onPlayerDisconnect(@Nonnull PlayerDisconnectEvent event) {
        GauntletOrchestrator.forgetHud(event.getPlayerRef());
    }
}
