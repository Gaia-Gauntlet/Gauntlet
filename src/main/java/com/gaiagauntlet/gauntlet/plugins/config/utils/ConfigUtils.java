package com.gaiagauntlet.gauntlet.plugins.config.utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.plugins.config.components.GameConfigComponent;
import com.gaiagauntlet.gauntlet.plugins.config.components.SessionGameConfigComponent;
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

    @Nonnull
    public static String configIdFor(@Nonnull GameSession session, @Nonnull String gameId) {
        return session.get(SessionGameConfigComponent.getSessionComponentType())
                .map(configs -> configs.getConfigId(gameId))
                .orElse(gameId);
    }

    @Nullable
    public static GameConfigAsset getGameConfig(@Nonnull GameSession session, @Nonnull String gameId) {
        return GameConfigAsset.getAssetMap().get(configIdFor(session, gameId));
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
