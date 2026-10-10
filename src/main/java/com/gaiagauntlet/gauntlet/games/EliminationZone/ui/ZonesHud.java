package com.gaiagauntlet.gauntlet.games.EliminationZone.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.components.PlayerComponent;
import com.gaiagauntlet.gauntlet.core.hud.huds.HudWidgets;
import com.gaiagauntlet.gauntlet.core.hud.interfaces.HudDataSnapshot;
import com.gaiagauntlet.gauntlet.core.hud.interfaces.HudElement;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state.EZState;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state.EZStates;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * The radial zone map in the top left corner: six fixed slots, each showing its
 * zone as active,
 * closing, or closed. Shows while the match is live and the zone logic has
 * published zones.
 */
public final class ZonesHud implements HudElement {

    private final HudWidgets.Sent sent = new HudWidgets.Sent();

    @Nonnull
    @Override
    public String getId() {
        return "Zones";
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

    private record ZonesHudSnapshot(
            GameEcs gameStore,
            ZoneHudComponent zoneHud) implements HudDataSnapshot {
    }

    @Override
    public ZonesHudSnapshot gather(@Nonnull PlayerRef player, @Nullable GameSession session) {
        if (session == null || session.available())
            return null; // not in a game at all
        var playerComp = player.getComponentConcurrent(PlayerComponent.getComponentType());
        if (!EZController.isEz(session))
            return null; // session is not in the game
        if (playerComp == null || playerComp.getCurrentGame() != EZController.ID)
            return null; // not in the ezgame

        // get the store
        var store = GameStore.withStore(player, session.getId()).orElse(null);

        if (store == null)
            return null;

        var zoneHudComp = store.get(ZoneHudComponent.getComponentType());

        if (zoneHudComp.isEmpty())
            return null;

        var state = EZState.currentState(store);
        if (!state.equals(EZStates.RUNNING) && !state.equals(EZStates.SUDDEN_DEATH))
            return null;
        // var phase = MatchUtils.phase(session);
        // return EZController.isEz(session)
        // && (phase == MatchState.ACTIVE || phase == MatchState.SUDDEN_DEATH)
        // && !ZoneHudComponent.of(session).isEmpty();
        return new ZonesHudSnapshot(store, zoneHudComp.get());
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
        sent.clear();
        cmd.set("#ZoneRadialBackground.Background", "GG/ZoneRadial/ZoneRadialBackground.png");
        cmd.set("#ZoneRadialFrame.Background", "GG/ZoneRadial/ZoneRadial.png");
    }

    @Override
    public void render(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session,
            HudDataSnapshot data) {
        if (!(data instanceof ZonesHudSnapshot zonesData)) {
            GaiaLog.atWarning().withSession(session)
                    .log("Attempted to update the Party Hud with the incorrect snapshot!");
            return;
        }

        if (session == null)
            return;
        var store = zonesData.gameStore;
        if (store == null)
            return; // no store found - probably not in the game

        var zoneComp = zonesData.zoneHud;
        if (zoneComp == null)
            return;

        var filled = new boolean[ZoneHudComponent.SLOTS];
        for (var zone : zoneComp.getZones()) {
            if (zone.slot() < 0 || zone.slot() >= ZoneHudComponent.SLOTS || zone.image().isBlank())
                continue;
            filled[zone.slot()] = true;
            var group = "#Zone" + zone.slot();
            sent.background(cmd, group, "GG/ZoneRadial/" + zone.state().name() + "_" + zone.image() + ".png");
        }
        for (int slot = 0; slot < ZoneHudComponent.SLOTS; slot++) {
            sent.visible(cmd, "#Zone" + slot, filled[slot]);
        }
    }
}
