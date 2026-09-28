package com.gaiagauntlet.gauntlet.core.games.registries;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GameController;
import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;

public class GameRegistry {

    // Note: these shouldn't ever change. So it can be a hashmap instead of concurrent hash because there wont be any writes to the map
    private static Map<String, GameController> controllerRegistry = new HashMap<>();
    private static Map<String, GamePlugin> pluginRegistry = new HashMap<>();

    public static void registerGame(String id, Supplier<GameController> controller) {
        controllerRegistry.put(id, controller.get());
    }
    
    public static void registerPlugin(String id, Supplier<GamePlugin> plugin) {
        pluginRegistry.put(id, plugin.get());
    }
}
