package com.gaiagauntlet.gauntlet.core.games.registries;

import java.util.Map;
import java.util.function.Supplier;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GameController;

public class GameRegistry {
    private static Map<String, GameController> controllerRegistry;

    public static void register(String id, Supplier<GameController> controller) {
        controllerRegistry.put(id, controller.get());
    }
}
