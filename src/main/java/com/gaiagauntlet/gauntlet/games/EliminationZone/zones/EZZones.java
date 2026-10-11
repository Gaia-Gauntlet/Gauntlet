package com.gaiagauntlet.gauntlet.games.EliminationZone.zones;

import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.components.RisingBlockComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.ZoneVisualisationComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.services.ZoneTickSystem;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.assets.ZonePhaseSet;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.utils.ZoneHandlers;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

public class EZZones {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void setup(JavaPlugin plugin) {
        LOGGER.atInfo().log("Setting up EZGame [Zones]!");

        ZoneVisualisationComponent.setComponentType(
                plugin.getEntityStoreRegistry().registerComponent(ZoneVisualisationComponent.class,
                        ZoneVisualisationComponent::new));

        plugin.getEntityStoreRegistry().registerSystem(new ZoneTickSystem());

        plugin.getAssetRegistry().register(HytaleAssetStore.builder(ZonePhaseSet.class, new DefaultAssetMap<>())
                .setPath("Gauntlet/Games/" + EZController.ID + "/ZonePhases")
                .setCodec(ZonePhaseSet.CODEC)
                .setKeyFunction(ZonePhaseSet::getId)
                .build());

        ZoneHandlers.register();
    }
}
