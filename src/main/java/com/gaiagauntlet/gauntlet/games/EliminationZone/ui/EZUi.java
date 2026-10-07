package com.gaiagauntlet.gauntlet.games.EliminationZone.ui;

import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.session.registry.SessionRegistry;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;

/** Registration and lookups shared by the Elimination Zone HUD elements. */
public final class EZUi {

    private EZUi() {
    }

    public static void setup() {
        ZoneHudComponent.setSessionComponentType(
                SessionRegistry.register(ZoneHudComponent.ID, ZoneHudComponent.class, null));
    }

    /** Every game's HUD elements are on every player's HUD, so each EZ element checks the session is playing EZ. */
    public static boolean isEz(@Nullable GameSession session) {
        return session != null && EZController.ID.equals(session.getCurrentGame());
    }
}
