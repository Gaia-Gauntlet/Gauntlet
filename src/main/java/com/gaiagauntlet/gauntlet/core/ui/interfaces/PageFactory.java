package com.gaiagauntlet.gauntlet.core.ui.interfaces;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/** Builds a fresh page for one player, optionally focused on a session. */
@FunctionalInterface
public interface PageFactory {
    @Nonnull
    CustomUIPage create(@Nonnull PlayerRef player, @Nullable GameSession session);
}
