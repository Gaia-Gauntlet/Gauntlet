package com.gaiagauntlet.gauntlet.games.EliminationZone.ui;

import com.gaiagauntlet.gauntlet.core.hud.events.HudEvent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.events.ZoneEvents;
import com.gaiagauntlet.gauntlet.plugins.events.events.MatchEventRegistry;
import com.gaiagauntlet.gauntlet.plugins.gamestate.events.MatchStateEvent;

/** Registration shared by the Elimination Zone HUD elements. */
public final class EZUi {

    private EZUi() {
    }

    public static void setup() {
        MatchEventRegistry.register(MatchStateEvent.class, EZController.ID, EZUi::onMatchState);
        MatchEventRegistry.register(ZoneEvents.ClosingStarted.class, EZController.ID,
                evt -> HudEvent.refresh(evt.getSessionId(), ZonesHud.ID).dispatch());
        MatchEventRegistry.register(ZoneEvents.Closed.class, EZController.ID,
                evt -> HudEvent.refresh(evt.getSessionId(), ZonesHud.ID).dispatch());
    }

    private static void onMatchState(MatchStateEvent evt) {
        ZonesHud.update(evt.getGame());
        WinnerHud.update(evt.getGame());
    }
}
