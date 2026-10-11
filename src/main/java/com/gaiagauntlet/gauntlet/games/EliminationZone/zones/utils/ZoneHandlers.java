package com.gaiagauntlet.gauntlet.games.EliminationZone.zones.utils;

import java.util.List;
import java.util.Objects;

import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfig;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state.EZStates;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.components.SpawnProtectionComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.ui.ZonesHud;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.ZoneComponent;
import com.gaiagauntlet.gauntlet.plugins.events.events.MatchEventRegistry;
import com.gaiagauntlet.gauntlet.plugins.gamestate.events.MatchStateEvent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.timer.events.TimerCompleteEvent;
import com.gaiagauntlet.gauntlet.plugins.timer.utils.TimerUtils;

public final class ZoneHandlers {

    private ZoneHandlers() {
    }

    public static void register() {
        TimerUtils.registerOnComplete(SpawnProtectionComponent.TIMER_ID, EZController.ID, ZoneHandlers::onGraceEnd);
        MatchEventRegistry.register(MatchStateEvent.class, EZController.ID, ZoneHandlers::onMatchState);
    }

    private static void onGraceEnd(TimerCompleteEvent evt) {
        var game = evt.getGameStore();
        var zones = game.get(ZoneComponent.TYPE).orElse(null);
        if (zones == null)
            return;
        var arena = GameStore.getContext(game).getWorld();
        var config = EZGameConfig.get(game);
        // timers complete inside a system tick, and the first zone announces and paints as it begins
        arena.execute(() -> {
            zones.begin(game.getSessionId(), arena, List.of(config.getZones()), config.getZonePhases(), 0);
            ZonesHud.update(game);
        });
    }

    private static void onMatchState(MatchStateEvent evt) {
        if (!Objects.equals(evt.getTo(), EZStates.ENDED.name()))
            return;
        evt.getGame().get(ZoneComponent.TYPE).ifPresent(ZoneComponent::stop);
    }
}
