package com.gaiagauntlet.gauntlet.games.EliminationZone.zones.utils;

import com.gaiagauntlet.gg.arena.ArenaComponent;
import com.gaiagauntlet.gg.events.*;
import com.gaiagauntlet.gg.match.MatchState;
import com.gaiagauntlet.gg.settings.Settings;
import com.gaiagauntlet.gg.store.Game;
import com.gaiagauntlet.gg.store.GlobalStore;
import com.gaiagauntlet.gg.ui.PlayerHuds;
import com.gaiagauntlet.gg.world.Worlds;
import com.gaiagauntlet.gg.zone.ZoneComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

/**
 * Hooks the zone feature onto the match: arm the sequence and show the radial HUD when the match
 * goes live, stop and hide when it ends. The core never calls in here.
 */
public final class ZoneHandlers {

    private ZoneHandlers() {
    }

    public static void register() {
        Events.on(MatchStateChangedEvent.class, e -> {
            var store = GlobalStore.find();
            var game = store == null ? null : store.game(e.gameId()).orElse(null);
            if (game == null) {
                return;
            }
            if (e.entered(MatchState.ACTIVE)) {
                arm(game);
            } else if (e.entered(MatchState.ENDED) || e.entered(MatchState.RETURNING) || e.entered(MatchState.IDLE)) {
                disarm(game);
            }
        });
        Events.on(PlayerArrivedEvent.class, e -> {
            if (e.purpose() == TransferPurpose.TO_ARENA) {
                showHud(e.gameId(), e.player());
            }
        });
        Events.on(PlayerRejoinedEvent.class, e -> showHud(e.gameId(), e.player()));
    }

    /** Shows the zone HUD to a player, when the game has one running. */
    private static void showHud(@Nonnull String gameId, @Nonnull PlayerRef player) {
        var store = GlobalStore.find();
        var game = store == null ? null : store.game(gameId).orElse(null);
        if (game != null && game.has(ZoneComponent.TYPE)) {
            var hud = ZoneComponent.TYPE.of(game).hud();
            if (hud != null) {
                hud.show(player);
            }
        }
    }

    private static void arm(@Nonnull Game game) {
        var arena = ArenaComponent.TYPE.of(game).world();
        if (arena == null) {
            return;
        }
        Worlds.run(arena, () -> {
            var zones = ZoneComponent.TYPE.of(game);
            var file = Zones.get().file();
            var delay = GlobalStore.get().settingsOf(game).get(Settings.ZONE_START_SECONDS);
            zones.begin(game.id(), arena, file.zones(), file.phases(), delay);
            var hud = new PlayerHuds(ZoneHud.KEY, viewer -> new ZoneHud(viewer, game), 1000);
            zones.setHud(hud);
            hud.showAll(arena);
        });
    }

    private static void disarm(@Nonnull Game game) {
        if (!game.has(ZoneComponent.TYPE)) {
            return;
        }
        var zones = ZoneComponent.TYPE.of(game);
        zones.stop();
        var hud = zones.hud();
        if (hud != null) {
            hud.hideAll();
            zones.setHud(null);
        }
    }
}
