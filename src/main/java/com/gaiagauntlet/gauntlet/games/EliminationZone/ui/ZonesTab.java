package com.gaiagauntlet.gauntlet.games.EliminationZone.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.events.AdminPageEvent;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.core.ui.pages.AdminPage;
import com.gaiagauntlet.gauntlet.core.ui.pages.Widgets;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.ZoneComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.ZoneDefinition;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.timer.utils.TimerUtils;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

/** The closing sequence of the arena. */
public final class ZonesTab implements AdminTab {

    private List<String> pickerZones = List.of();

    @Nonnull @Override public String getId() {
        return "Zones";
    }

    @Nonnull @Override public String getPanel() {
        return "Gauntlet/Games/EliminationZone/ZonesPanel.ui";
    }

    @Override public int getOrder() {
        return 30;
    }

    @Override
    public void bind(@Nonnull UIEventBuilder evt) {
        Widgets.bind(evt, "#ZoneSkip", "zones.skip");
        Widgets.bind(evt, "#ZonePause", "zones.pause");
        Widgets.bind(evt, "#ZoneResume", "zones.resume");
        Widgets.bindValues(evt, "#ZoneDelay", "zones.delay", "", Map.of("@Num", "#ZoneDelaySeconds.Value"));
        Widgets.bindValues(evt, "#ZoneNext", "zones.next", "", Map.of("@Pick", "#ZoneNextPicker.Value"));
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nullable GameSession session) {
        pickerZones = List.of();
        Widgets.fillPicker(cmd, "#ZoneNextPicker", List.of());
    }

    @Override
    public void render(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nullable GameSession session) {
        var zones = EZAdmin.game(session).flatMap(game -> game.get(ZoneComponent.TYPE)).orElse(null);
        Widgets.field(cmd, "ZoneStateField", state(zones));
        Widgets.field(cmd, "ZoneVoidField", zones == null ? "" : zones.voidTerrain().paintedBlocks() + " painted, "
                + zones.voidTerrain().queuedBlocks() + " queued");
        Widgets.fillList(cmd, "ZoneList", rows(zones), "No zones are closing");

        var waiting = zones == null || !zones.isActive() ? List.<String>of() : waiting(zones);
        if (!waiting.equals(pickerZones)) {
            pickerZones = waiting;
            Widgets.fillPicker(cmd, "#ZoneNextPicker", Widgets.options(waiting));
        }
    }

    @Nullable
    @Override
    public Message handle(@Nonnull String action, @Nonnull AdminPageEvent event, @Nullable GameSession session, @Nonnull AdminPage page) {
        return switch (action) {
            case "zones.skip" -> EZAdmin.onGame(session, page, action, game -> {
                zones(game).skip();
                return Widgets.ok("Skipped the current zone step");
            });
            case "zones.pause" -> EZAdmin.onGame(session, page, action, game -> {
                zones(game).pause();
                return Widgets.ok("Zone closing paused");
            });
            case "zones.resume" -> EZAdmin.onGame(session, page, action, game -> {
                zones(game).resume();
                return Widgets.ok("Zone closing resumed");
            });
            case "zones.delay" -> {
                var seconds = Widgets.parseInt(event.num(), "seconds");
                yield EZAdmin.onGame(session, page, action, game -> {
                    zones(game).delay(seconds);
                    return Widgets.ok("Zone closing delayed by " + TimerUtils.clock(seconds));
                });
            }
            case "zones.next" -> {
                var zoneId = event.pick();
                if (zoneId.isEmpty()) yield Widgets.fail("Pick a zone first");
                yield EZAdmin.onGame(session, page, action, game -> {
                    zones(game).setNext(zoneId);
                    return Widgets.ok(zoneId + " closes next");
                });
            }
            default -> null;
        };
    }

    @Nonnull
    private static ZoneComponent zones(@Nonnull GameEcs game) {
        return game.get(ZoneComponent.TYPE).orElseThrow(() -> new IllegalStateException("This game has no zones"));
    }

    @Nonnull
    private static List<String> waiting(@Nonnull ZoneComponent zones) {
        var order = zones.order();
        return order.subList(Math.min(zones.stepIndex() + 1, order.size()), order.size()).stream().map(ZoneDefinition::id).toList();
    }

    @Nonnull
    private static String state(@Nullable ZoneComponent zones) {
        if (zones == null || !zones.isActive()) return "not running";
        if (zones.isFinished()) return "every zone is closed";
        var clock = TimerUtils.clock((float) zones.timerSeconds());
        var active = zones.activeZone();
        var text = active == null ? "first zone in " + clock
                : zones.isHolding() ? "holding, next zone in " + clock
                : active.id() + " seals in " + clock;
        return zones.isPaused() ? text + " (paused)" : text;
    }

    @Nonnull
    private static List<String> rows(@Nullable ZoneComponent zones) {
        if (zones == null || !zones.isActive()) return List.of();
        var closed = zones.closedZones();
        var active = zones.activeZone();
        var rows = new ArrayList<String>();
        var order = zones.order();
        for (int i = 0; i < order.size(); i++) {
            var zone = order.get(i);
            String status;
            if (closed.contains(zone)) {
                status = "sealed";
            } else if (zone == active) {
                status = "closing, " + Math.round(zones.progress() * 100) + "%";
            } else {
                status = "waiting";
            }
            rows.add((i + 1) + ". " + zone.id() + " - " + status);
        }
        return rows;
    }
}
