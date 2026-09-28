package com.gaiagauntlet.gauntlet.core.games.registries;

import java.util.Map;
import java.util.function.Supplier;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GameController;
import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;

public class GameRegistry {
    private static Map<String, GameController> controllerRegistry;
    private static Map<String, GamePlugin> pluginRegistry;

    public static void register(String id, Supplier<GameController> controller) {
        controllerRegistry.put(id, controller.get());
    }

}
