package com.gaiagauntlet.gauntlet.games.EliminationZone.ui;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.events.AdminPageEvent;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.core.ui.pages.AdminPage;
import com.gaiagauntlet.gauntlet.core.ui.pages.Widgets;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfig;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.events.BossSpawnEventConfig;
import com.gaiagauntlet.gauntlet.games.EliminationZone.weather.events.WeatherEventConfig;
import com.gaiagauntlet.gauntlet.plugins.events.assets.GameEventConfig;
import com.gaiagauntlet.gauntlet.plugins.events.components.GameEventComponent;
import com.gaiagauntlet.gauntlet.plugins.events.components.SequenceHolder;
import com.gaiagauntlet.gauntlet.plugins.events.utils.EventUtils;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.timer.utils.TimerUtils;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

/** The live event queue, ad hoc events, and the events the game config schedules. */
public final class EventsTab implements AdminTab {

    private static final String EVENT_ROW = "Gauntlet/Games/EliminationZone/EventRow.ui";

    private List<String> rowKeys = List.of();

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
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nullable GameSession session) {
        Widgets.fillPicker(cmd, "#EventTypePicker", List.of(
                new Widgets.Option("Weather", WeatherEventConfig.Id),
                new Widgets.Option("Boss", BossSpawnEventConfig.Id)));
        rowKeys = List.of("");
    }

    @Override
    public void render(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nullable GameSession session) {
        var game = EZAdmin.game(session).orElse(null);
        var events = EventUtils.get(game).orElse(null);

        Widgets.field(cmd, "EventClockField", clock(events));
        renderQueue(cmd, evt, events == null ? List.of() : events.scheduled());

        var configured = game == null ? null : EZGameConfig.get(game).getGameEvents();
        var entries = configured == null || configured.getEntries() == null ? new GameEventConfig[0] : configured.getEntries();
        Widgets.field(cmd, "EventDrawField", entries.length == 0 ? "none"
                : "every " + TimerUtils.clock((float) configured.getIntervalSeconds())
                        + ", first at " + TimerUtils.clock((float) (configured.getIntervalSeconds() + configured.getFirstDrawAtSeconds())));
        Widgets.fillList(cmd, "EventConfigList", Arrays.stream(entries).map(GameEventConfig::describe).toList(),
                "Nothing is drawn at random");
    }

    @Nullable
    @Override
    public Message handle(@Nonnull String action, @Nonnull AdminPageEvent event, @Nullable GameSession session, @Nonnull AdminPage page) {
        return switch (action) {
            case "events.next" -> EZAdmin.onGame(session, page, action,
                    game -> Widgets.ok("Fired " + EventUtils.fireNext(game).describe()));
            case "events.pause" -> EZAdmin.onGame(session, page, action, game -> {
                EventUtils.pause(game);
                return Widgets.ok("Events paused");
            });
            case "events.resume" -> EZAdmin.onGame(session, page, action, game -> {
                EventUtils.resume(game);
                return Widgets.ok("Events resumed");
            });
            case "events.delay" -> {
                var seconds = Widgets.parseInt(event.num(), "seconds");
                yield EZAdmin.onGame(session, page, action, game -> {
                    EventUtils.delay(game, seconds);
                    return Widgets.ok("Events delayed by " + TimerUtils.clock(seconds));
                });
            }
            case "events.cancel" -> {
                var parts = event.arg().split(":", 2);
                var index = Integer.parseInt(parts[0]);
                var scheduledTime = Double.parseDouble(parts[1]);
                yield EZAdmin.onGame(session, page, action, game -> {
                    EventUtils.cancel(game, index, scheduledTime);
                    return Widgets.ok("Event cancelled");
                });
            }
            case "events.queue" -> {
                var config = create(event.pick(), event.text());
                var seconds = Widgets.parseInt(event.num(), "seconds");
                yield EZAdmin.onGame(session, page, action, game -> {
                    EventUtils.queue(game, config, seconds);
                    return Widgets.ok(config.describe() + " queued in " + TimerUtils.clock(seconds));
                });
            }
            case "events.force" -> {
                var config = create(event.pick(), event.text());
                yield EZAdmin.onGame(session, page, action, game -> {
                    EventUtils.fire(game, config);
                    return Widgets.ok("Ran " + config.describe());
                });
            }
            default -> null;
        };
    }

    @Nonnull
    private static GameEventConfig create(@Nonnull String type, @Nonnull String argument) {
        return switch (type) {
            case WeatherEventConfig.Id -> new WeatherEventConfig(argument);
            case BossSpawnEventConfig.Id -> new BossSpawnEventConfig(argument);
            default -> throw new IllegalArgumentException("Pick an event type first");
        };
    }

    private void renderQueue(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nonnull List<SequenceHolder> queue) {
        var keys = queue.stream().map(holder -> holder.getScheduledTime() + ":" + holder.getGameConfig().describe()).toList();
        if (keys.equals(rowKeys)) {
            return;
        }
        rowKeys = keys;
        if (queue.isEmpty()) {
            Widgets.fillList(cmd, "EventList", List.of(), "The queue is empty");
            return;
        }
        cmd.clear("#EventList");
        for (int i = 0; i < queue.size(); i++) {
            var holder = queue.get(i);
            var row = "#EventList[" + i + "]";
            cmd.append("#EventList", EVENT_ROW);
            Widgets.text(cmd, row + " #Text", TimerUtils.clock((float) holder.getScheduledTime()) + "  " + holder.getGameConfig().describe());
            Widgets.bindArg(evt, row + " #Cancel", "events.cancel", i + ":" + holder.getScheduledTime());
        }
    }

    @Nonnull
    private static String clock(@Nullable GameEventComponent events) {
        if (events == null) return "no match running";
        var clock = TimerUtils.clock(events.getAccumulatedGameTimeSeconds());
        var next = events.getNextRandomEventTime() == Double.MAX_VALUE ? ""
                : ", next draw at " + TimerUtils.clock((float) events.getNextRandomEventTime());
        return clock + next + (events.isPaused() ? " (paused)" : "");
    }
}
