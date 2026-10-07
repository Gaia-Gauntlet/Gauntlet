package com.gaiagauntlet.gauntlet.games.EliminationZone.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.HudElement;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * The radial zone map in the top left corner: six fixed slots, each showing its zone as active,
 * closing, or closed. Hidden until zones exist.
 */
public final class ZonesHud implements HudElement {

    @Nonnull @Override public String getId() {
        return "Zones";
    }

    @Nonnull @Override public String getMarkup() {
        return "Gauntlet/Games/EliminationZone/ZonesHud.ui";
    }

    @Override public int getOrder() {
        return 10;
    }

    @Override
    public boolean isVisible(@Nonnull PlayerRef player, @Nullable GameSession session) {
        return false;
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
        cmd.set("#ZoneRadialBackground.Background", "GG/ZoneRadial/ZoneRadialBackground.png");
        cmd.set("#ZoneRadialFrame.Background", "GG/ZoneRadial/ZoneRadial.png");
    }
}
