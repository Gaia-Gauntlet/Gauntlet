package com.gaiagauntlet.gauntlet.plugins.config.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.session.constants.SessionState;
import com.gaiagauntlet.gauntlet.core.ui.SessionText;
import com.gaiagauntlet.gauntlet.core.ui.events.AdminPageEvent;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.core.ui.pages.AdminPage;
import com.gaiagauntlet.gauntlet.core.ui.pages.Widgets;
import com.gaiagauntlet.gauntlet.plugins.config.ConfigPlugin;
import com.gaiagauntlet.gauntlet.plugins.config.components.SessionGameConfigComponent;
import com.gaiagauntlet.gauntlet.plugins.config.components.assets.GameConfigAsset;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

/** The game config each game uses in the selected session, and overriding it for that session. */
public final class ConfigTab implements AdminTab {

    @Nonnull @Override public String getId() {
        return "Config";
    }

    @Nonnull @Override public String getPanel() {
        return "Gauntlet/Plugins/" + ConfigPlugin.ID + "/ConfigPanel.ui";
    }

    @Override public int getOrder() {
        return 80;
    }

    @Override
    public void bind(@Nonnull UIEventBuilder evt) {
        Widgets.bindValues(evt, "#ConfigApply", "config.apply", "",
                Map.of("@Pick", "#ConfigGamePicker.Value", "@Text", "#ConfigAssetPicker.Value"));
        Widgets.bindValues(evt, "#ConfigClear", "config.clear", "", Map.of("@Pick", "#ConfigGamePicker.Value"));
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nullable GameSession session) {
        Widgets.fillPicker(cmd, "#ConfigGamePicker",
                games().stream().map(id -> new Widgets.Option(SessionText.game(id), id)).toList());
        var assets = assets();
        Widgets.fillPicker(cmd, "#ConfigAssetPicker", Widgets.options(assets));
        Widgets.fillList(cmd, "ConfigAssetList", assets, "No game configs loaded");
    }

    @Override
    public void render(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nullable GameSession session) {
        var overrides = overrides(session);
        var rows = new ArrayList<String>();
        for (var game : games()) {
            var override = overrides.get(game);
            rows.add(SessionText.game(game) + ": " + (override == null ? "default" : override + " (override)"));
        }
        Widgets.fillList(cmd, "ConfigList", session == null ? List.of() : rows, "Pick a session first");
    }

    @Nullable
    @Override
    public Message handle(@Nonnull String action, @Nonnull AdminPageEvent event, @Nullable GameSession session, @Nonnull AdminPage page) {
        if (session == null) return Widgets.fail("Pick a session first");
        var state = session.getSessionState();
        if (state == SessionState.SETTING_UP || state == SessionState.RUNNING) {
            return Widgets.fail("The session is locked while a game runs, so change overrides between games");
        }
        var game = event.pick();
        if (!GameRegistry.hasGame(game)) return Widgets.fail("Pick a game first");
        return switch (action) {
            case "config.apply" -> {
                var asset = event.text();
                if (!GameConfigAsset.getAssetMap().containsKey(asset)) yield Widgets.fail("Pick a loaded config first");
                GauntletUtils.run(GauntletUtils.withHubWorld(), () -> session
                        .ensure(SessionGameConfigComponent.getSessionComponentType(), new SessionGameConfigComponent())
                        .getGameToConfigMap().put(game, asset));
                yield Widgets.ok(SessionText.game(game) + " will use " + asset + " in " + session.getId());
            }
            case "config.clear" -> {
                GauntletUtils.run(GauntletUtils.withHubWorld(), () -> session
                        .get(SessionGameConfigComponent.getSessionComponentType())
                        .ifPresent(configs -> configs.getGameToConfigMap().remove(game)));
                yield Widgets.ok(SessionText.game(game) + " is back to its default config in " + session.getId());
            }
            default -> null;
        };
    }

    @Nonnull
    private static Map<String, String> overrides(@Nullable GameSession session) {
        var type = SessionGameConfigComponent.getSessionComponentType();
        if (session == null || type == null) return Map.of();
        return session.get(type).map(c -> Map.copyOf(c.getGameToConfigMap())).orElse(Map.of());
    }

    @Nonnull
    private static List<String> games() {
        var games = new ArrayList<>(GameRegistry.getGameIds());
        games.sort(String.CASE_INSENSITIVE_ORDER);
        return games;
    }

    @Nonnull
    private static List<String> assets() {
        var assets = new ArrayList<>(GameConfigAsset.getAssetMap().keySet());
        assets.sort(String.CASE_INSENSITIVE_ORDER);
        return assets;
    }
}
