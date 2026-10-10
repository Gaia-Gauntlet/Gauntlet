package com.gaiagauntlet.gauntlet.plugins.config.utils;

import com.gaiagauntlet.gauntlet.plugins.config.components.GameConfigComponent;
import com.gaiagauntlet.gauntlet.plugins.config.components.assets.GameConfigAsset;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.hypixel.hytale.server.core.universe.world.World;

public class ConfigUtils {
    private ConfigUtils() {
    }

    public static GameConfigAsset getGameConfig(World world, String sessionId) {
        return getGameConfig(GameStore.ensureStore(world, sessionId));
    }

    public static GameConfigAsset getGameConfig(GameEcs store) {
        var configComponent = store
                .get(GameConfigComponent.getComponentType())
                .orElse(null);
                
        if (configComponent == null)
            return null;
        return configComponent.getConfig();
    }
}
