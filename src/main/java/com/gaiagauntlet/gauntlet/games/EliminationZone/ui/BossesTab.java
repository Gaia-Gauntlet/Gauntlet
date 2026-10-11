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
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.BossSpawner;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.components.BossesComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.utils.BossUtils;
import com.gaiagauntlet.gauntlet.games.EliminationZone.weather.utils.WeatherUtils;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.timer.utils.TimerUtils;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

/** Bosses to spawn and weather to force on the arena. */
public final class BossesTab implements AdminTab {

    private List<String> pickerWeathers = List.of();

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
        Widgets.fillPicker(cmd, "#BossPicker", Widgets.options(BossUtils.getBossNames()));
        pickerWeathers = List.of();
    }

    @Override
    public void render(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nullable GameSession session) {
        var game = EZAdmin.game(session).orElse(null);
        Widgets.fillList(cmd, "BossList", bossRows(game), "No GaiaBoss roles loaded");

        var weathers = WeatherUtils.weatherIds(game);
        if (!weathers.equals(pickerWeathers)) {
            pickerWeathers = weathers;
            Widgets.fillPicker(cmd, "#WeatherPicker", Widgets.options(weathers));
        }
        Widgets.field(cmd, "WeatherField", WeatherUtils.current(game)
                .filter(weather -> weather.getWeatherId() != null)
                .map(weather -> weather.getRemainingSeconds() == null ? weather.getWeatherId()
                        : weather.getWeatherId() + ", " + TimerUtils.clock(weather.getRemainingSeconds().floatValue()) + " left")
                .orElse("the world's own"));
    }

    @Nullable
    @Override
    public Message handle(@Nonnull String action, @Nonnull AdminPageEvent event, @Nullable GameSession session, @Nonnull AdminPage page) {
        return switch (action) {
            case "bosses.spawn" -> {
                var bossId = event.pick();
                if (bossId.isEmpty()) yield Widgets.fail("Pick a boss first");
                yield EZAdmin.onGame(session, page, action, game -> {
                    BossSpawner.spawn(game.getSessionId(), game, bossId);
                    return Widgets.ok("Spawning " + bossId);
                });
            }
            case "bosses.random" -> EZAdmin.onGame(session, page, action, game -> {
                BossSpawner.spawnRandom(game.getSessionId(), GameStore.getContext(game).getWorld());
                return Widgets.ok("Spawning a random boss");
            });
            case "bosses.weather" -> {
                var weatherId = event.pick();
                if (weatherId.isEmpty()) yield Widgets.fail("Pick a weather first");
                var seconds = EZAdmin.optionalSeconds(event.num());
                yield EZAdmin.onGame(session, page, action, game -> {
                    WeatherUtils.doWeather(game, weatherId, seconds);
                    return Widgets.ok("Weather forced to " + weatherId
                            + (seconds == null ? "" : " for " + TimerUtils.clock(seconds.floatValue())));
                });
            }
            case "bosses.weatherRandom" -> EZAdmin.onGame(session, page, action,
                    game -> Widgets.ok("Weather forced to " + WeatherUtils.doRandomWeather(game, null)));
            case "bosses.weatherClear" -> EZAdmin.onGame(session, page, action, game -> {
                WeatherUtils.doStop(game);
                return Widgets.ok("Weather returned to the world's own");
            });
            default -> null;
        };
    }

    @Nonnull
    private static List<String> bossRows(@Nullable GameEcs game) {
        var bosses = game == null ? null : game.get(BossesComponent.TYPE).orElse(null);
        var rows = new ArrayList<String>();
        for (var boss : BossUtils.getBosses().values()) {
            var id = boss.roleId();
            String status;
            if (!boss.isEnabled()) {
                status = "disabled";
            } else if (bosses != null && bosses.isActive(id)) {
                status = "out in " + bosses.active().stream().filter(a -> a.bossId().equals(id)).findFirst()
                        .map(BossesComponent.Active::zoneId).orElse("the arena");
            } else if (bosses != null && bosses.getPending().contains(id)) {
                status = "spawning";
            } else {
                status = "ready";
            }
            rows.add(boss.displayText() + " (" + id + ") - " + status);
        }
        return rows;
    }
}
