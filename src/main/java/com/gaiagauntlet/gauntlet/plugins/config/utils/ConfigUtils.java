package com.gaiagauntlet.gauntlet.plugins.config.utils;

import com.gaiagauntlet.gauntlet.plugins.config.components.GameConfigComponent;
import com.gaiagauntlet.gauntlet.plugins.config.components.assets.GameConfigAsset;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.hypixel.hytale.server.core.universe.world.World;

public class ConfigUtils {
    private ConfigUtils() {}

    public static GameConfigAsset getGameConfig(World world, String game) {
        var configComponent = GameStore.ensureStore(world, game)
            .get(GameConfigComponent.getComponentType())
            .orElse(null);
        if (configComponent == null) return null;
        return configComponent.getConfig();
    }
}
