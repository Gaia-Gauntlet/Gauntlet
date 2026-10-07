package com.gaiagauntlet.gauntlet.core.ui.tabs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.games.interfaces.CommandGamePlugin;
import com.gaiagauntlet.gauntlet.core.games.interfaces.GameController;
import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.gaiagauntlet.gauntlet.core.games.interfaces.SimpleGamePlugin;
import com.gaiagauntlet.gauntlet.core.games.interfaces.UiGamePlugin;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.core.ui.pages.Widgets;
import com.gaiagauntlet.gauntlet.plugins.gamestore.interfaces.PersistentGamePlugin;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

/**
 * What is registered: each game and the plugins it requires, each plugin with its capabilities,
 * dependencies and the games that use it, and anything required that is not registered. The
 * registry is fixed once the server starts, so this is read only and filled once.
 */
public final class PluginsTab implements AdminTab {

    @Nonnull @Override public String getId() {
        return "Plugins";
    }

    @Nonnull @Override public String getPanel() {
        return "Gauntlet/Admin/Panels/PanelPlugins.ui";
    }

    @Override public int getOrder() {
        return 90;
    }

    @Override
    public void bind(@Nonnull UIEventBuilder evt) {
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nullable GameSession session) {
        var games = games();
        var plugins = new ArrayList<>(GameRegistry.getPlugins(GamePlugin.class));
        plugins.sort(Comparator.comparing(GamePlugin::getId, String.CASE_INSENSITIVE_ORDER));
        var problems = new ArrayList<String>();

        var gameRows = new ArrayList<String>();
        for (var game : games) {
            gameRows.add(game.getDisplayName() + " (" + game.getId() + ") needs " + joined(game.getRequiredPlugins()));
            for (var required : game.getRequiredPlugins()) {
                if (!GameRegistry.hasPlugin(required)) {
                    problems.add(game.getDisplayName() + " needs " + required + ", which is not registered");
                }
            }
        }

        var pluginRows = new ArrayList<String>();
        for (var plugin : plugins) {
            var usedBy = games.stream()
                    .filter(game -> game.getRequiredPlugins().contains(plugin.getId()))
                    .map(GameController::getDisplayName)
                    .toList();
            pluginRows.add(plugin.getId() + " [" + capabilities(plugin) + "]. Depends on " + joined(plugin.getDependencies())
                    + ". Used by " + joined(usedBy) + ".");
            for (var dependency : plugin.getDependencies()) {
                if (!GameRegistry.hasPlugin(dependency)) {
                    problems.add(plugin.getId() + " depends on " + dependency + ", which is not registered");
                }
            }
        }

        Widgets.fillList(cmd, "GameRegistryList", gameRows, "No games are registered");
        Widgets.fillList(cmd, "PluginRegistryList", pluginRows, "No plugins are registered");
        Widgets.fillList(cmd, "PluginProblems", problems, "Every requirement is registered");
    }

    @Nonnull
    private static List<GameController> games() {
        var games = new ArrayList<GameController>();
        for (var id : GameRegistry.getGameIds()) {
            GameRegistry.getGame(id).ifPresent(games::add);
        }
        games.sort(Comparator.comparing(GameController::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        return games;
    }

    @Nonnull
    private static String capabilities(@Nonnull GamePlugin plugin) {
        var capabilities = new ArrayList<String>();
        if (plugin instanceof PersistentGamePlugin) capabilities.add("persistent");
        if (plugin instanceof SimpleGamePlugin) capabilities.add("per game");
        if (plugin instanceof UiGamePlugin) capabilities.add("UI");
        if (plugin instanceof CommandGamePlugin) capabilities.add("commands");
        return capabilities.isEmpty() ? "none" : String.join(", ", capabilities);
    }

    @Nonnull
    private static String joined(@Nonnull List<String> values) {
        return values.isEmpty() ? "nothing" : String.join(", ", values);
    }
}
