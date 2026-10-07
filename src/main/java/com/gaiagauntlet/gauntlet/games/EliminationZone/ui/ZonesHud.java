package com.gaiagauntlet.gauntlet.games.EliminationZone.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.huds.HudWidgets;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.HudElement;
import com.gaiagauntlet.gauntlet.plugins.gamestate.constants.MatchState;
import com.gaiagauntlet.gauntlet.plugins.gamestate.utils.MatchUtils;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * The radial zone map in the top left corner: six fixed slots, each showing its zone as active,
 * closing, or closed. Shows while the match is live and the zone logic has published zones.
 */
public final class ZonesHud implements HudElement {

    private final HudWidgets.Sent sent = new HudWidgets.Sent();

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
        var phase = MatchUtils.phase(session);
        return EZUi.isEz(session)
                && (phase == MatchState.ACTIVE || phase == MatchState.SUDDEN_DEATH)
                && !ZoneHudComponent.of(session).isEmpty();
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
        sent.clear();
        cmd.set("#ZoneRadialBackground.Background", "GG/ZoneRadial/ZoneRadialBackground.png");
        cmd.set("#ZoneRadialFrame.Background", "GG/ZoneRadial/ZoneRadial.png");
    }

    @Override
    public void render(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
        var filled = new boolean[ZoneHudComponent.SLOTS];
        for (var zone : ZoneHudComponent.of(session)) {
            if (zone.slot() < 0 || zone.slot() >= ZoneHudComponent.SLOTS || zone.image().isBlank()) continue;
            filled[zone.slot()] = true;
            var group = "#Zone" + zone.slot();
            sent.background(cmd, group, "GG/ZoneRadial/" + zone.state().name() + "_" + zone.image() + ".png");
        }
        for (int slot = 0; slot < ZoneHudComponent.SLOTS; slot++) {
            sent.visible(cmd, "#Zone" + slot, filled[slot]);
        }
    }
}
