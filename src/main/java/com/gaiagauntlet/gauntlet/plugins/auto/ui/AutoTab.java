package com.gaiagauntlet.gauntlet.plugins.auto.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import com.gaiagauntlet.gauntlet.plugins.auto.components.assets.AutoSessionAsset;
import com.gaiagauntlet.gauntlet.plugins.auto.utils.AutoUtils;
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
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nullable GameSession session) {
        Widgets.fillPicker(cmd, "#AutoConfigPicker", Widgets.options(assets()));
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
    }

    @Nullable
    @Override
    public Message handle(@Nonnull String action, @Nonnull AdminPageEvent event, @Nullable GameSession session, @Nonnull AdminPage page) {
        if (session == null) return Widgets.fail("Pick a session first");
        if (!AutoUtils.runs(session)) return Widgets.fail("Auto is not installed, so install it from the Session tab");
        return switch (action) {
            case "auto.apply" -> {
                var configId = event.pick();
                if (!AutoSessionAsset.getAssetMap().containsKey(configId)) yield Widgets.fail("Pick a loaded config first");
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
        var configId = auto == null ? session.getId() : AutoUtils.configId(session, auto);
        var bound = auto != null && auto.getConfigId() != null ? "" : " (named after the session)";
        var loaded = AutoSessionAsset.getAssetMap().containsKey(configId) ? "" : ", not loaded";
        return configId + bound + loaded;
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
    private static List<String> assets() {
        var assets = new ArrayList<>(AutoSessionAsset.getAssetMap().keySet());
        assets.sort(String.CASE_INSENSITIVE_ORDER);
        return assets;
    }
}
