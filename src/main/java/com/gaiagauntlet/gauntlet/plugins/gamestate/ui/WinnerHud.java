package com.gaiagauntlet.gauntlet.plugins.gamestate.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.plugins.gamestate.GameStatePlugin;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.HudElement;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * The end screen: the winner banner and winning team's name over a podium of the top three.
 * Hidden until match standings exist.
 */
public final class WinnerHud implements HudElement {

    @Nonnull @Override public String getId() {
        return "Winner";
    }

    @Nonnull @Override public String getMarkup() {
        return "Gauntlet/Plugins/" + GameStatePlugin.ID + "/WinnerHud.ui";
    }

    @Override public int getOrder() {
        return 100;
    }

    @Override
    public boolean isVisible(@Nonnull PlayerRef player, @Nullable GameSession session) {
        return false;
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
        cmd.set("#WinnerBanner.Background", "GG/WinnerBanner.png");
    }
}
