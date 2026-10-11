package com.gaiagauntlet.gauntlet.core.hud.interfaces;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * One element of the player HUD. The plugin that owns the element also ships its markup; the element
 * fills the parts that only change when the player's session changes and renders its live fields on
 * every refresh. Every element shares one HUD document per player, so element ids must be unique.
 */
public interface HudElement {

    @Nonnull
    String getId();

    /** Path of the element markup, relative to Common/UI/Custom. Its root is anchored against the whole screen. */
    @Nonnull
    String getMarkup();

    /** Draw order. Lower is drawn first, so later elements cover earlier ones; ties are ordered by id. */
    default int getOrder() {
        return 0;
    }

    /**
     * The session-wide data the element draws from, or null when the element does
     * not apply to this player. Returning null hides the element.
     */
    @Nullable
    default HudDataSnapshot gather(@Nonnull PlayerRef player, @Nullable GameSession session) {
        return null;
    }

    /** Fills the parts that only change with the session. Runs when the HUD is shown and when the player's session changes. */
    default void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
    }

    /**
     * Rewrites the live fields from session-wide data. Event snapshots reach every
     * player in the session, so this returns false without drawing when the data
     * does not apply to this player.
     */
    boolean render(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session,
            @Nonnull HudDataSnapshot data);
}
