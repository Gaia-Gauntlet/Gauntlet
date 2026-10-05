package com.gaiagauntlet.gauntlet.games.EliminationZone.ui;

import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.error;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.events.AdminPageEvent;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.core.ui.pages.AdminPage;
import com.gaiagauntlet.gauntlet.core.ui.pages.Widgets;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

/** The closing sequence of the arena and the phase pacing for the next match. */
public final class ZonesTab implements AdminTab {

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
        Widgets.field(cmd, "ZoneStateField", "not running");
        Widgets.field(cmd, "ZoneVoidField", "");
        Widgets.fillList(cmd, "ZoneList", List.of(), "No zones defined");
        Widgets.fillPicker(cmd, "#ZoneNextPicker", List.of());
    }

    @Nullable
    @Override
    public Message handle(@Nonnull String action, @Nonnull AdminPageEvent event, @Nullable GameSession session, @Nonnull AdminPage page) {
        return switch (action) {
            case "zones.skip" -> notYet("Skipping a zone step");
            case "zones.pause" -> notYet("Pausing zone closing");
            case "zones.resume" -> notYet("Resuming zone closing");
            case "zones.delay" -> notYet("Delaying zone closing");
            case "zones.next" -> notYet("Choosing the next zone");
            case "zones.phase" -> notYet("Editing phase pacing");
            default -> null;
        };
    }

    @Nonnull
    private static Message notYet(@Nonnull String what) {
        return error(what + " is not yet supported!");
    }
}
