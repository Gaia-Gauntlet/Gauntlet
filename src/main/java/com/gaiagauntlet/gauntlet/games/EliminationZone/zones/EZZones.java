package com.gaiagauntlet.gauntlet.games.EliminationZone.zones;

import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.components.RisingBlockComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.ZoneVisualisationComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.services.ZoneTickSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

public class EZZones {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void setup(JavaPlugin plugin) {
        LOGGER.atInfo().log("Setting up EZGame [Zones]!");

        ZoneVisualisationComponent.setComponentType(
                plugin.getEntityStoreRegistry().registerComponent(ZoneVisualisationComponent.class,
                        ZoneVisualisationComponent::new));

        plugin.getEntityStoreRegistry().registerSystem(new ZoneTickSystem());
    }
}
