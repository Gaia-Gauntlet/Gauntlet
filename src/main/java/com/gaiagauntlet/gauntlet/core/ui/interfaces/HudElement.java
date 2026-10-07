package com.gaiagauntlet.gauntlet.core.ui.interfaces;

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

    /** Whether the element shows for this player right now. Checked on every refresh. */
    default boolean isVisible(@Nonnull PlayerRef player, @Nullable GameSession session) {
        return true;
    }

    /** Fills the parts that only change with the session. Runs when the HUD is shown and when the player's session changes. */
    default void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
    }

    /** Rewrites the live fields. Runs on the refresh timer while the element is visible. */
    default void render(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
    }
}
