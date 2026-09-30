package com.gaiagauntlet.gauntlet.core.games.registries;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GameController;
import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

public class GameRegistry {

    // Note: these shouldn't ever change. So it can be a hashmap instead of concurrent hash because there wont be any writes to the map
    private static Map<String, GameController> controllerRegistry = new HashMap<>();
    private static Map<String, GamePlugin> pluginRegistry = new HashMap<>();

    public static void registerGame(String id, Supplier<GameController> controller) {
        controllerRegistry.put(id, controller.get());
    }
    
    public static void registerPlugin(String id, JavaPlugin host, Supplier<GamePlugin> pluginSupplier) {
        var plugin = pluginSupplier.get();
        plugin.setup(host);
        pluginRegistry.put(id, plugin);
    }

    public static Optional<GameController> getGame(@Nullable String id) {
        return Optional.ofNullable(id == null ? null : controllerRegistry.get(id));
    }

    public static boolean hasGame(String id) {
        return controllerRegistry.containsKey(id);
    } 
    public static boolean hasPlugin(String id) {
        return pluginRegistry.containsKey(id);
    } 
}
