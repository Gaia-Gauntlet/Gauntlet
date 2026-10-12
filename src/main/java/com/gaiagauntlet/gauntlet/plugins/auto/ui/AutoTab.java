package com.gaiagauntlet.gauntlet.plugins.auto.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.events.AdminPageEvent;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.core.ui.pages.AdminPage;
import com.gaiagauntlet.gauntlet.core.ui.pages.Widgets;
import com.gaiagauntlet.gauntlet.plugins.auto.AutoPlugin;
import com.gaiagauntlet.gauntlet.plugins.auto.components.AutoComponent;
import com.gaiagauntlet.gauntlet.plugins.auto.components.assets.AutoSession;
import com.gaiagauntlet.gauntlet.plugins.auto.components.assets.SessionPreset;
import com.gaiagauntlet.gauntlet.plugins.auto.components.rotation.PresetRotation;
import com.gaiagauntlet.gauntlet.plugins.auto.components.rotation.SequentialRotation;
import com.gaiagauntlet.gauntlet.plugins.auto.components.rotation.VoteRotation;
import com.gaiagauntlet.gauntlet.plugins.auto.utils.AutoUtils;
import com.gaiagauntlet.gauntlet.plugins.auto.utils.VoteUtils;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.ContextComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.timer.utils.TimerUtils;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

public final class AutoTab implements AdminTab {

    @Nonnull @Override public String getId() {
        return "Auto";
    }

    @Nonnull @Override public String getPanel() {
        return "Gauntlet/Plugins/" + AutoPlugin.ID + "/AutoPanel.ui";
    }

    @Override public int getOrder() {
        return 70;
    }

    @Override
    public void bind(@Nonnull UIEventBuilder evt) {
        Widgets.bindValues(evt, "#AutoApply", "auto.apply", "", Map.of("@Pick", "#AutoConfigPicker.Value"));
        Widgets.bind(evt, "#AutoReset", "auto.reset");
        Widgets.bind(evt, "#AutoPause", "auto.pause");
        Widgets.bind(evt, "#AutoResume", "auto.resume");
        Widgets.bindValues(evt, "#AutoPlayNext", "auto.playnext", "", Map.of("@Pick", "#AutoPresetPicker.Value"));
        Widgets.bind(evt, "#AutoClearPick", "auto.clearpick");
        Widgets.bind(evt, "#AutoOpenVote", "auto.openvote");
        Widgets.bind(evt, "#AutoCloseVote", "auto.closevote");
        Widgets.bind(evt, "#AutoStartNow", "auto.startnow");
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nullable GameSession session) {
        Widgets.fillPicker(cmd, "#AutoConfigPicker", Widgets.options(sorted(AutoSession.getAssetMap().keySet())));
        Widgets.fillPicker(cmd, "#AutoPresetPicker", Widgets.options(sorted(SessionPreset.getAssetMap().keySet())));
    }

    @Override
    public void render(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nullable GameSession session) {
        var installed = session != null && AutoUtils.runs(session);
        var auto = installed ? session.get(AutoComponent.getComponentType()).orElse(null) : null;
        Widgets.field(cmd, "AutoInstalledField", session == null ? "N/A"
                : installed ? "yes" : "no, install Auto from the Session tab");
        Widgets.field(cmd, "AutoStateField", session == null ? "N/A" : session.getSessionState().name());
        Widgets.field(cmd, "AutoConfigField", installed ? config(session, auto) : "N/A");
        Widgets.field(cmd, "AutoPausedField", auto == null ? "N/A" : auto.isPaused() ? "yes" : "no");
        Widgets.field(cmd, "AutoFailuresField", auto == null ? "N/A" : String.valueOf(auto.getFailures()));
        Widgets.field(cmd, "AutoTimerField", installed ? timer(session) : "N/A");
        var rotation = installed ? rotation(session, auto) : null;
        Widgets.field(cmd, "AutoRotationField", installed ? rotationType(rotation) : "N/A");
        Widgets.field(cmd, "AutoPoolField", installed ? pool(rotation) : "N/A");
        Widgets.field(cmd, "AutoCurrentField", auto == null ? "N/A" : Objects.requireNonNullElse(auto.getCurrentPreset(), "none"));
        Widgets.field(cmd, "AutoNextField", auto == null ? "N/A" : upNext(session, auto));
        Widgets.field(cmd, "AutoVoteField", installed ? vote(session) : "N/A");
        Widgets.field(cmd, "AutoStartsField", installed ? startsIn(session) : "N/A");
    }

    @Nullable
    @Override
    public Message handle(@Nonnull String action, @Nonnull AdminPageEvent event, @Nullable GameSession session, @Nonnull AdminPage page) {
        if (session == null) return Widgets.fail("Pick a session first");
        if (!AutoUtils.runs(session)) return Widgets.fail("Auto is not installed, so install it from the Session tab");
        return switch (action) {
            case "auto.apply" -> {
                var configId = event.pick();
                if (!AutoSession.getAssetMap().containsKey(configId)) yield Widgets.fail("Pick a loaded config first");
                mutate(session, auto -> auto.setConfigId(configId));
                yield Widgets.ok(session.getId() + " will follow " + configId);
            }
            case "auto.reset" -> {
                mutate(session, auto -> auto.setConfigId(null));
                yield Widgets.ok(session.getId() + " will follow the config named after it");
            }
            case "auto.pause" -> {
                mutate(session, auto -> auto.setPaused(true));
                yield Widgets.ok("Auto paused for " + session.getId());
            }
            case "auto.resume" -> {
                mutate(session, auto -> {
                    auto.setPaused(false);
                    auto.setFailures(0);
                });
                yield Widgets.ok("Auto resumed for " + session.getId());
            }
            case "auto.playnext" -> {
                var presetId = event.pick();
                if (!SessionPreset.getAssetMap().containsKey(presetId)) yield Widgets.fail("Pick a loaded preset first");
                mutate(session, auto -> AutoUtils.pickNext(session, auto, presetId));
                yield Widgets.ok(session.getId() + " will play " + presetId + " next");
            }
            case "auto.clearpick" -> {
                mutate(session, auto -> auto.setNextPreset(null));
                yield Widgets.ok(session.getId() + " will play the rotation's pick next");
            }
            case "auto.openvote" -> {
                if (!session.available()) yield Widgets.fail("A vote can only open between games");
                var auto = session.get(AutoComponent.getComponentType()).orElse(null);
                if (!(rotation(session, auto) instanceof VoteRotation)) yield Widgets.fail(configId(session, auto) + " does not use a vote rotation");
                if (VoteUtils.isOpen(session)) yield Widgets.fail("A vote is already open");
                if (auto != null && auto.getNextPreset() != null) yield Widgets.fail("An admin already picked the next game, so clear the pick first");
                GauntletUtils.withHubWorld().execute(() -> {
                    if (!AutoUtils.openVote(session)) page.pushStatus(Widgets.fail("The vote did not open, so check the rotation's pool"));
                });
                yield Widgets.ok("Vote opened for " + session.getId());
            }
            case "auto.closevote" -> {
                if (!VoteUtils.isOpen(session)) yield Widgets.fail("No vote is open");
                GauntletUtils.withHubWorld().execute(() -> {
                    if (!VoteUtils.close(session)) page.pushStatus(Widgets.fail("The vote had already closed"));
                });
                yield Widgets.ok("Vote closed for " + session.getId());
            }
            case "auto.startnow" -> {
                if (AutoUtils.secondsToStart(session) < 0) yield Widgets.fail(session.getId() + " is not waiting to start a game");
                GauntletUtils.withHubWorld().execute(() -> {
                    if (!AutoUtils.startNow(session)) page.pushStatus(Widgets.fail(session.getId() + " stopped waiting before it could start"));
                });
                yield Widgets.ok("Starting the next game in " + session.getId());
            }
            default -> null;
        };
    }

    private static void mutate(@Nonnull GameSession session, @Nonnull Consumer<AutoComponent> change) {
        GauntletUtils.withHubWorld().execute(() -> {
            var auto = AutoUtils.auto(session);
            if (auto == null) return;
            change.accept(auto);
            AutoUtils.schedule(session.getId());
        });
    }

    @Nonnull
    private static String config(@Nonnull GameSession session, @Nullable AutoComponent auto) {
        var configId = configId(session, auto);
        var bound = auto != null && auto.getConfigId() != null ? "" : " (named after the session)";
        var loaded = AutoSession.getAssetMap().containsKey(configId) ? "" : ", not loaded";
        return configId + bound + loaded;
    }

    @Nonnull
    private static String configId(@Nonnull GameSession session, @Nullable AutoComponent auto) {
        return auto == null ? session.getId() : AutoUtils.configId(session, auto);
    }

    @Nullable
    private static PresetRotation rotation(@Nonnull GameSession session, @Nullable AutoComponent auto) {
        var config = AutoSession.getAssetMap().get(configId(session, auto));
        return config == null ? null : config.getRotation();
    }

    @Nonnull
    private static String rotationType(@Nullable PresetRotation rotation) {
        return switch (rotation) {
            case null -> "none";
            case SequentialRotation _ -> SequentialRotation.TYPE;
            case VoteRotation _ -> VoteRotation.TYPE;
            default -> rotation.getClass().getSimpleName();
        };
    }

    @Nonnull
    private static String pool(@Nullable PresetRotation rotation) {
        var presets = switch (rotation) {
            case SequentialRotation sequential -> sequential.getPresets();
            case VoteRotation vote -> vote.getPool();
            case null, default -> null;
        };
        if (presets == null) return "N/A";
        return presets.length == 0 ? "empty" : String.join(", ", presets);
    }

    @Nonnull
    private static String upNext(@Nonnull GameSession session, @Nonnull AutoComponent auto) {
        var next = AutoUtils.upNext(session, auto);
        if (next == null) return "none";
        return auto.getNextPreset() != null ? next + " (picked)" : next;
    }

    @Nonnull
    private static String vote(@Nonnull GameSession session) {
        var vote = VoteUtils.get(session);
        if (vote == null) return "none";
        var tally = vote.tally();
        var counts = vote.getOptions().stream().map(option -> option + " " + tally.get(option)).toList();
        return (vote.isClosed() ? "closed, " : "open, ") + vote.totalVotes() + " votes: " + String.join(", ", counts);
    }

    @Nonnull
    private static String startsIn(@Nonnull GameSession session) {
        var seconds = AutoUtils.secondsToStart(session);
        return seconds < 0 ? "not waiting" : seconds + "s";
    }

    @Nonnull
    private static String timer(@Nonnull GameSession session) {
        return GameStore.withStore(GauntletUtils.withHubWorld(), session.getId())
                .filter(store -> store.get(ContextComponent.getComponentType())
                        .map(context -> AutoPlugin.ID.equals(context.getGameId())).orElse(false))
                .flatMap(TimerUtils::get)
                .map(timer -> timer.getTimerId() + ", " + (int) Math.ceil(timer.getCountdownRemaining()) + "s left")
                .orElse("none");
    }

    @Nonnull
    private static List<String> sorted(@Nonnull Collection<String> ids) {
        var sorted = new ArrayList<>(ids);
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        return sorted;
    }
}
