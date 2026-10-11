package com.gaiagauntlet.gauntlet.games.EliminationZone.ui;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.hud.events.HudEvent;
import com.gaiagauntlet.gauntlet.core.hud.huds.HudWidgets;
import com.gaiagauntlet.gauntlet.core.hud.interfaces.HudDataSnapshot;
import com.gaiagauntlet.gauntlet.core.hud.interfaces.HudElement;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state.EZState;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state.EZStates;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.ZoneComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * The radial zone map in the top left corner: six fixed slots, each showing its
 * zone as active,
 * closing, or closed. Shows while the match is live and the zones have begun.
 */
public final class ZonesHud implements HudElement {

    public static final String ID = "Zones";

    /** Slots on the radial, clockwise from the top. */
    public static final int SLOTS = 6;

    private final HudWidgets.Sent sent = new HudWidgets.Sent();

    public enum ZoneState {
        ACTIVE,
        CLOSING,
        CLOSED
    }

    /**
     * One zone on the radial. The image is the art suffix under GG/ZoneRadial, for
     * example "Gaia_City" for GG/ZoneRadial/ACTIVE_Gaia_City.png.
     */
    public record Zone(int slot, @Nonnull String image, @Nonnull ZoneState state) {
    }

    public record ZonesHudSnapshot(
            GameEcs gameStore,
            List<Zone> zones) implements HudDataSnapshot {
    }

    @Nonnull
    @Override
    public String getId() {
        return ID;
    }

    @Nonnull
    @Override
    public String getMarkup() {
        return "Gauntlet/Games/EliminationZone/ZonesHud.ui";
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Nullable
    public static ZonesHudSnapshot snapshot(@Nullable GameEcs game) {
        if (game == null)
            return null;
        var zones = game.get(ZoneComponent.TYPE).orElse(null);
        if (zones == null || !zones.isActive())
            return null;
        var state = EZState.currentState(game);
        if (state != EZStates.RUNNING && state != EZStates.SUDDEN_DEATH)
            return null;

        var closed = zones.closedZones();
        var closing = zones.isClosingStarted() && !zones.isHolding() ? zones.activeZone() : null;
        var radial = new ArrayList<Zone>();
        for (var zone : zones.order()) {
            if (zone.hudSlot() < 0 || zone.hudSlot() >= SLOTS || zone.hudImage().isBlank())
                continue;
            var zoneState = closed.contains(zone) ? ZoneState.CLOSED
                    : zone == closing ? ZoneState.CLOSING : ZoneState.ACTIVE;
            radial.add(new Zone(zone.hudSlot(), zone.hudImage(), zoneState));
        }
        return new ZonesHudSnapshot(game, List.copyOf(radial));
    }

    public static void update(@Nonnull GameEcs game) {
        new HudEvent(game.getSessionId(), ID, snapshot(game)).dispatch();
    }

    @Override
    public ZonesHudSnapshot gather(@Nonnull PlayerRef player, @Nullable GameSession session) {
        if (session == null || session.available() || !EZController.isEz(session))
            return null;
        return snapshot(GameStore.withStore(player, session.getId()).orElse(null));
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
        sent.clear();
        cmd.set("#ZoneRadialBackground.Background", "GG/ZoneRadial/ZoneRadialBackground.png");
        cmd.set("#ZoneRadialFrame.Background", "GG/ZoneRadial/ZoneRadial.png");
    }

    @Override
    public boolean render(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session,
            @Nonnull HudDataSnapshot data) {
        if (!(data instanceof ZonesHudSnapshot zonesData)) {
            GaiaLog.atWarning().withSession(session)
                    .log("Attempted to update the Zones Hud with the incorrect snapshot!");
            return false;
        }
        if (!GameStore.isIn(player, zonesData.gameStore))
            return false;

        var filled = new boolean[SLOTS];
        for (var zone : zonesData.zones) {
            filled[zone.slot()] = true;
            var group = "#Zone" + zone.slot();
            sent.background(cmd, group, "GG/ZoneRadial/" + zone.state().name() + "_" + zone.image() + ".png");
        }
        for (int slot = 0; slot < SLOTS; slot++) {
            sent.visible(cmd, "#Zone" + slot, filled[slot]);
        }
        return true;
    }
}
