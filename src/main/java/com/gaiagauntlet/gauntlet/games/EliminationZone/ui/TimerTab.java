package com.gaiagauntlet.gauntlet.games.EliminationZone.ui;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.events.AdminPageEvent;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.core.ui.pages.AdminPage;
import com.gaiagauntlet.gauntlet.core.ui.pages.Widgets;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.timer.components.GameTimerComponent;
import com.gaiagauntlet.gauntlet.plugins.timer.utils.TimerUtils;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

public final class TimerTab implements AdminTab {

    private static final String NO_BANNER = "None";
    private static final List<Widgets.Option> BANNERS = List.of(
            new Widgets.Option("No HUD banner", NO_BANNER),
            new Widgets.Option("Match starting soon", TimerUtils.IMG_COUNTDOWN),
            new Widgets.Option("Get to the portal", TimerUtils.IMG_PORTAL_OPEN),
            new Widgets.Option("Until match start", TimerUtils.IMG_STAGING),
            new Widgets.Option("Grace period", TimerUtils.IMG_GRACE_PERIOD));
    private static final List<String> QUICK_SECONDS = List.of("-30", "30", "60", "300");
    private static final List<String> QUICK_BUTTONS = List.of("#GameTimerMinus30", "#GameTimerPlus30", "#GameTimerPlus60", "#GameTimerPlus300");
    private static final List<String> TIMER_BUTTONS = List.of("#GameTimerToggle", "#GameTimerComplete", "#GameTimerCancel",
            "#GameTimerAdd", "#GameTimerSet");
    private static final float DRIFT_SECONDS = 1.5f;

    @Nullable private GameTimerComponent sentTimer;
    private float sentSeconds;
    private long sentAt;

    @Nonnull @Override public String getId() {
        return "Timer";
    }

    @Nonnull @Override public String getPanel() {
        return "Gauntlet/Games/EliminationZone/TimerPanel.ui";
    }

    @Override public int getOrder() {
        return 20;
    }

    @Override
    public void bind(@Nonnull UIEventBuilder evt) {
        Widgets.bind(evt, "#GameTimerToggle", "timer.toggle");
        Widgets.bind(evt, "#GameTimerComplete", "timer.complete");
        Widgets.bind(evt, "#GameTimerCancel", "timer.cancel");
        for (int i = 0; i < QUICK_BUTTONS.size(); i++) {
            Widgets.bindArg(evt, QUICK_BUTTONS.get(i), "timer.add", QUICK_SECONDS.get(i));
        }
        Widgets.bindValues(evt, "#GameTimerAdd", "timer.add", "", Map.of("@Num", "#GameTimerAdjustSeconds.Value"));
        Widgets.bindValues(evt, "#GameTimerSet", "timer.set", "", Map.of("@Num", "#GameTimerAdjustSeconds.Value"));
        Widgets.bindValues(evt, "#GameTimerStart", "timer.start", "", Map.of(
                "@Text", "#GameTimerStartId.Value",
                "@Num", "#GameTimerStartSeconds.Value",
                "@Pick", "#GameTimerBanner.Value"));
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nullable GameSession session) {
        sentTimer = null;
        Widgets.fillPicker(cmd, "#GameTimerBanner", BANNERS);
        var timer = EZAdmin.game(session).flatMap(TimerUtils::get).orElse(null);
        if (timer == null) return;
        cmd.set("#GameTimerStartId.Value", timer.getTimerId());
        cmd.set("#GameTimerBanner.Value", timer.getTimerImageId() == null ? NO_BANNER : timer.getTimerImageId());
    }

    @Override
    public void render(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nullable GameSession session) {
        var game = EZAdmin.game(session).orElse(null);
        var timer = game == null ? null : TimerUtils.get(game).orElse(null);
        var running = timer != null && timer.isRunning();

        cmd.set("#GameTimerLive.Visible", running);
        cmd.set("#GameTimerStill.Visible", !running);
        if (running) {
            syncClock(cmd, timer);
        } else {
            sentTimer = null;
            Widgets.text(cmd, "#GameTimerStill", timer == null ? "--:--" : TimerUtils.clock(timer.getCountdownRemaining()));
        }

        Widgets.field(cmd, "GameTimerIdField", timer == null ? "none" : timer.getTimerId());
        Widgets.field(cmd, "GameTimerStateField", state(session, game, timer));
        Widgets.field(cmd, "GameTimerBannerField", timer == null ? "" : banner(timer.getTimerImageId()));
        Widgets.field(cmd, "GameTimerMarkersField", timer == null ? "" : timer.getPois().size() + " in the world");

        for (var button : TIMER_BUTTONS) {
            cmd.set(button + ".Disabled", timer == null);
        }
        for (var button : QUICK_BUTTONS) {
            cmd.set(button + ".Disabled", timer == null);
        }
        cmd.set("#GameTimerStart.Disabled", game == null);
        Widgets.text(cmd, "#GameTimerToggle", timer != null && !running ? "Resume" : "Pause");
    }

    @Nullable
    @Override
    public Message handle(@Nonnull String action, @Nonnull AdminPageEvent event, @Nullable GameSession session, @Nonnull AdminPage page) {
        return switch (action) {
            case "timer.toggle" -> onTimer(session, page, action, (game, timer) -> {
                var clock = TimerUtils.clock(timer.getCountdownRemaining());
                if (timer.isRunning()) {
                    TimerUtils.stop(game);
                    return Widgets.ok("Paused " + timer.getTimerId() + " at " + clock);
                }
                TimerUtils.play(game);
                return Widgets.ok("Resumed " + timer.getTimerId() + " at " + clock);
            });
            case "timer.complete" -> onTimer(session, page, action, (game, timer) -> {
                TimerUtils.complete(game);
                return Widgets.ok("Completed " + timer.getTimerId());
            });
            case "timer.cancel" -> onTimer(session, page, action, (game, timer) -> {
                TimerUtils.cancel(game);
                return Widgets.ok("Cancelled " + timer.getTimerId() + " without completing it");
            });
            case "timer.add" -> {
                var seconds = Widgets.parseInt(event.arg().isEmpty() ? event.num() : event.arg(), "seconds");
                if (seconds == 0) yield Widgets.fail("Enter how many seconds to add");
                yield onTimer(session, page, action, (game, timer) -> {
                    timer.setTimer(timer.getCountdownRemaining() + seconds);
                    var verb = seconds > 0 ? "Added " : "Took ";
                    return Widgets.ok(verb + TimerUtils.clock(Math.abs(seconds)) + ", " + timer.getTimerId() + " now at "
                            + TimerUtils.clock(timer.getCountdownRemaining()));
                });
            }
            case "timer.set" -> {
                var seconds = Widgets.parseInt(event.num(), "seconds");
                if (seconds <= 0) yield Widgets.fail("The timer needs at least one second");
                yield onTimer(session, page, action, (game, timer) -> {
                    timer.setTimer(seconds);
                    return Widgets.ok(timer.getTimerId() + " set to " + TimerUtils.clock(seconds));
                });
            }
            case "timer.start" -> {
                var timerId = event.text();
                if (timerId.isEmpty()) yield Widgets.fail("Name the timer first");
                var seconds = Widgets.parseInt(event.num(), "seconds");
                if (seconds <= 0) yield Widgets.fail("The timer needs at least one second");
                var banner = event.pick().isEmpty() || NO_BANNER.equals(event.pick()) ? null : event.pick();
                yield EZAdmin.onGame(session, page, action, game -> {
                    var replaced = TimerUtils.get(game).map(GameTimerComponent::getTimerId).orElse(null);
                    if (replaced != null) TimerUtils.cancel(game);
                    TimerUtils.start(game, seconds, new GameTimerComponent(timerId, banner));
                    var started = "Started " + timerId + " for " + TimerUtils.clock(seconds);
                    return Widgets.ok(replaced == null ? started : started + ", replacing " + replaced);
                });
            }
            default -> null;
        };
    }

    private void syncClock(@Nonnull UICommandBuilder cmd, @Nonnull GameTimerComponent timer) {
        var now = System.currentTimeMillis();
        var remaining = timer.getCountdownRemaining();
        var predicted = sentSeconds - (now - sentAt) / 1000f;
        if (timer == sentTimer && Math.abs(predicted - remaining) <= DRIFT_SECONDS) return;
        sentTimer = timer;
        sentSeconds = Math.round(remaining);
        sentAt = now;
        cmd.set("#GameTimerLive.Seconds", Math.round(remaining));
    }

    @Nullable
    private static Message onTimer(@Nullable GameSession session, @Nonnull AdminPage page, @Nonnull String action,
            @Nonnull BiFunction<GameEcs, GameTimerComponent, Message> task) {
        return EZAdmin.onGame(session, page, action, game -> task.apply(game, TimerUtils.get(game)
                .orElseThrow(() -> new IllegalStateException("No timer is running in " + game.getSessionId()))));
    }

    @Nonnull
    private static String state(@Nullable GameSession session, @Nullable GameEcs game, @Nullable GameTimerComponent timer) {
        if (session == null) return "no session selected";
        if (game == null) return session.getId() + " has no game in a live arena";
        if (timer == null) return "no timer";
        return timer.isRunning() ? "running" : "paused";
    }

    @Nonnull
    private static String banner(@Nullable String imageId) {
        for (var option : BANNERS) {
            if (option.value().equals(imageId)) return option.label();
        }
        return imageId == null ? "none" : imageId;
    }
}
