package com.gaiagauntlet.gauntlet.core.games.registries;

import java.util.*;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GameController;
import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

public class GameRegistry {

    // Note: these shouldn't ever change. So it can be a hashmap instead of
    // concurrent hash because there won't be any writes to the map
    private static Map<String, GameController> controllerRegistry = new HashMap<>();
    private static Map<String, GamePlugin> pluginRegistry = new HashMap<>();

    public static void registerGame(String id, Supplier<GameController> controller) {
        controllerRegistry.put(id, controller.get());
    }

    public static void registerPlugin(String id, JavaPlugin host, Supplier<GamePlugin> pluginSupplier) {
        var plugin = pluginSupplier.get();
        plugin.init(host);
        pluginRegistry.put(id, plugin);
    }

    public static Optional<GameController> getGame(@Nullable String id) {
        return Optional.ofNullable(id == null ? null : controllerRegistry.get(id));
    }

    public static Set<String> getGameIds() {
        return controllerRegistry.keySet();
    }

    public static boolean hasGame(String id) {
        return controllerRegistry.containsKey(id);
    }

    public static boolean hasPlugin(String id) {
        return pluginRegistry.containsKey(id);
    }

    public static Optional<GamePlugin> getPlugin(String id) {
        return Optional.ofNullable(id == null ? null : pluginRegistry.get(id));
    }

    public static <T extends GamePlugin> List<T> getPlugins(Class<T> type) {
        return pluginRegistry.values().stream()
            .filter(type::isInstance)
            .map(type::cast)
            .toList();
    }

    /** The plugins and everything they depend on, directly or not. Unregistered ids are kept so callers can report them */
    public static Set<String> withDependencies(Collection<String> ids) {
        var resolved = new LinkedHashSet<String>();
        var pending = new ArrayDeque<String>(ids);
        while (!pending.isEmpty()) {
            var id = pending.poll();
            if (!resolved.add(id))
                continue;
            var plugin = pluginRegistry.get(id);
            if (plugin != null)
                pending.addAll(plugin.getDependencies());
        }
        return resolved;
    }

    public static List<GamePlugin> getPlugins(List<String> ids) {
        return ids.stream()
                .map(pluginRegistry::get)
                .toList();
    }
    public static <T extends GamePlugin> List<T> getPlugins(List<String> ids, Class<T> type) {
        return ids.stream()
                .map(pluginRegistry::get)
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }
}
