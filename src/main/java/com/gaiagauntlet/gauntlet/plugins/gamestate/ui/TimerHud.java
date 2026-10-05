package com.gaiagauntlet.gauntlet.plugins.gamestate.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.plugins.gamestate.GameStatePlugin;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.HudElement;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * The countdown banner at the top of the screen: the header art for the current phase over the
 * remaining time. Hidden until the match countdown exists.
 */
public final class TimerHud implements HudElement {

    @Nonnull @Override public String getId() {
        return "Timer";
    }

    @Nonnull @Override public String getMarkup() {
        return "Gauntlet/Plugins/" + GameStatePlugin.ID + "/TimerHud.ui";
    }

    @Override public int getOrder() {
        return 20;
    }

    @Override
    public boolean isVisible(@Nonnull PlayerRef player, @Nullable GameSession session) {
        return false;
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
        cmd.set("#TimerImage.Background", "GG/TimerHeaderUntilPortalOpens.png");
        cmd.set("#TimerLabel.Text", "00:00");
    }
}
