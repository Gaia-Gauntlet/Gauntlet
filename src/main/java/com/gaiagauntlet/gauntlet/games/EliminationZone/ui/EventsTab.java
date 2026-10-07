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

/** The live event queue, ad hoc events, and the preset. */
public final class EventsTab implements AdminTab {

    @Nonnull @Override public String getId() {
        return "Events";
    }

    @Nonnull @Override public String getPanel() {
        return "Gauntlet/Games/EliminationZone/EventsPanel.ui";
    }

    @Override public int getOrder() {
        return 50;
    }

    @Override
    public void bind(@Nonnull UIEventBuilder evt) {
        Widgets.bind(evt, "#EventNext", "events.next");
        Widgets.bind(evt, "#EventPause", "events.pause");
        Widgets.bind(evt, "#EventResume", "events.resume");
        Widgets.bindValues(evt, "#EventDelay", "events.delay", "", Map.of("@Num", "#EventDelaySeconds.Value"));
        Widgets.bindValues(evt, "#EventQueue", "events.queue", "",
                Map.of("@Pick", "#EventTypePicker.Value", "@Text", "#EventArg.Value", "@Num", "#EventInSeconds.Value"));
        Widgets.bindValues(evt, "#EventForce", "events.force", "", Map.of("@Pick", "#EventTypePicker.Value", "@Text", "#EventArg.Value"));
        Widgets.bindValues(evt, "#EventPresetGame", "events.presetGame", "", Map.of("@Pick", "#EventPresetPicker.Value"));
        Widgets.bindValues(evt, "#EventPresetGlobal", "events.presetGlobal", "", Map.of("@Pick", "#EventPresetPicker.Value"));
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nullable GameSession session) {
        Widgets.fillPicker(cmd, "#EventTypePicker", Widgets.options(List.of("Weather", "Boss")));
        Widgets.fillPicker(cmd, "#EventPresetPicker", List.of());
        Widgets.fillList(cmd, "PresetList", List.of(), "No presets loaded");
        Widgets.field(cmd, "EventPresetField", "N/A");
        Widgets.field(cmd, "EventClockField", "no match running");
        Widgets.fillList(cmd, "EventList", List.of(), "The queue fills when the match goes live");
    }

    @Nullable
    @Override
    public Message handle(@Nonnull String action, @Nonnull AdminPageEvent event, @Nullable GameSession session, @Nonnull AdminPage page) {
        return switch (action) {
            case "events.next" -> notYet("Firing the next event");
            case "events.pause" -> notYet("Pausing events");
            case "events.resume" -> notYet("Resuming events");
            case "events.delay" -> notYet("Delaying events");
            case "events.cancel" -> notYet("Cancelling an event");
            case "events.queue" -> notYet("Queueing an event");
            case "events.force" -> notYet("Running an event");
            case "events.presetGame", "events.presetGlobal" -> notYet("Choosing an event preset");
            default -> null;
        };
    }

    @Nonnull
    private static Message notYet(@Nonnull String what) {
        return error(what + " is not yet supported!");
    }
}
