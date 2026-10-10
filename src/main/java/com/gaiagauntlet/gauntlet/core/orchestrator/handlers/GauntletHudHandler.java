package com.gaiagauntlet.gauntlet.core.orchestrator.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.games.interfaces.UiGamePlugin;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.hud.events.HudEvent;
import com.gaiagauntlet.gauntlet.core.hud.huds.GauntletHud;
import com.gaiagauntlet.gauntlet.core.hud.huds.SessionHud;
import com.gaiagauntlet.gauntlet.core.hud.interfaces.HudElement;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class GauntletHudHandler {
    /**
     * The HUD currently shown to each player, so a replaced or abandoned HUD stops
     * refreshing
     */
    private static final Map<UUID, GauntletHud> huds = new ConcurrentHashMap<>();

    /**
     * Shows the player HUD, replacing the one the player had. Every HUD is shown
     * through here
     */
    public static void showHud(Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef) {
        var player = store.getComponent(ref, Player.getComponentType());
        if (player == null)
            return;

        var hud = new GauntletHud(playerRef);
        var previous = huds.put(playerRef.getUuid(), hud);
        if (previous != null)
            previous.stop();
        player.getHudManager().addCustomHud(playerRef, hud);
    }

    /** Stops the HUD of a player who left */
    public static void forgetHud(PlayerRef playerRef) {
        var hud = huds.remove(playerRef.getUuid());
        if (hud != null)
            hud.stop();
    }

    public static Map<String, HudElement> getHudElements() {
        var elements = new ArrayList<HudElement>(List.of(new SessionHud()));

        for (var plugin : GameRegistry.getPlugins(UiGamePlugin.class)) {
            elements.addAll(plugin.getHudElements());
        }
        for (var gameId : GameRegistry.getGameIds()) {
            GameRegistry.getGame(gameId).ifPresent(game -> elements.addAll(game.getHudElements()));
        }
        elements.sort(Comparator.comparingInt(HudElement::getOrder).thenComparing(HudElement::getId));

        var elementMap = new LinkedHashMap<String, HudElement>();
        for (var element : elements) {
            var previous = elementMap.putIfAbsent(element.getId(), element);
            if (previous != null) {
                GaiaLog.atWarning().log("Duplicate HUD element id: " + element.getId());
            }
        }

        return elementMap;
    }

    public static void onHudEvent(HudEvent evt) {
        var players = gatherEventPlayers(evt);
        GauntletUtils.run(evt.getWorld(), () -> {
            for (var player : players) {
                var hud = huds.get(player.getUuid());
                if (hud != null) {
                    hud.onEvent(evt.getHudData(), evt.getHudId());
                }
            }
        });
    }

    private static Collection<PlayerRef> gatherEventPlayers(HudEvent evt) {
        if (evt.getSessionId() != null) {
            var players = GauntletUtils.playersFor(evt.getSessionId());
            return players;
        }

        return Universe.get().getPlayers();
    }

}
