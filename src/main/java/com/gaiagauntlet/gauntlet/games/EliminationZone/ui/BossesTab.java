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

/** Bosses to spawn and weather to force on the arena. */
public final class BossesTab implements AdminTab {

    @Nonnull @Override public String getId() {
        return "Bosses";
    }

    @Nonnull @Override public String getPanel() {
        return "Gauntlet/Games/EliminationZone/BossesPanel.ui";
    }

    @Override public int getOrder() {
        return 40;
    }

    @Override
    public void bind(@Nonnull UIEventBuilder evt) {
        Widgets.bindValues(evt, "#BossSpawn", "bosses.spawn", "", Map.of("@Pick", "#BossPicker.Value"));
        Widgets.bind(evt, "#BossRandom", "bosses.random");
        Widgets.bindValues(evt, "#WeatherApply", "bosses.weather", "", Map.of("@Pick", "#WeatherPicker.Value", "@Num", "#WeatherSeconds.Value"));
        Widgets.bind(evt, "#WeatherRandom", "bosses.weatherRandom");
        Widgets.bind(evt, "#WeatherClear", "bosses.weatherClear");
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nullable GameSession session) {
        Widgets.fillPicker(cmd, "#BossPicker", List.of());
        Widgets.fillPicker(cmd, "#WeatherPicker", List.of());
        Widgets.fillList(cmd, "BossList", List.of(), "No GaiaBoss roles loaded");
        Widgets.field(cmd, "WeatherField", "the world's own");
    }

    @Nullable
    @Override
    public Message handle(@Nonnull String action, @Nonnull AdminPageEvent event, @Nullable GameSession session, @Nonnull AdminPage page) {
        return switch (action) {
            case "bosses.spawn" -> notYet("Spawning a boss");
            case "bosses.random" -> notYet("Spawning a random boss");
            case "bosses.weather" -> notYet("Forcing weather");
            case "bosses.weatherRandom" -> notYet("Random weather");
            case "bosses.weatherClear" -> notYet("Clearing weather");
            default -> null;
        };
    }

    @Nonnull
    private static Message notYet(@Nonnull String what) {
        return error(what + " is not yet supported!");
    }
}
