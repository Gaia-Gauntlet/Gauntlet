package com.gaiagauntlet.gauntlet.games.EliminationZone.ui;

import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.events.GauntletEventRegistry;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;
import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameComponentRegistry;

/** Registration and lookups shared by the Elimination Zone HUD elements. */
public final class EZUi {

    private EZUi() {
    }

    public static void setup() {
        ZoneHudComponent.setComponentType(GameComponentRegistry.register(ZoneHudComponent.ID, ZoneHudComponent.class));
        // GauntletEventRegistry.on(MatchStateEvent.class, EZTitles::onMatchState);
    }
}
