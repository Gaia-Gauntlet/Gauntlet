package com.gaiagauntlet.gauntlet.plugins.teams.listeners;

import java.util.Set;

import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.teams.utils.TeamUtils;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;

public final class TeamHudListeners {

    private TeamHudListeners() {
    }

    /** Registered early, while the leaving player is still in their party and so still resolves to their game. */
    public static void onPlayerDisconnect(PlayerDisconnectEvent evt) {
        var player = evt.getPlayerRef();
        if (player == null)
            return;
        GameStore.withStore(player).ifPresent(game -> TeamUtils.updateHuds(game, Set.of(player.getUuid())));
    }
}
