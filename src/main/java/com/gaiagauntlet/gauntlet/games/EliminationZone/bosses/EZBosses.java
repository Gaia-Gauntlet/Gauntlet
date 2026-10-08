package com.gaiagauntlet.gauntlet.games.EliminationZone.bosses;

import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.components.BossMarkerComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.components.BossScalingComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.roles.GaiaBossRoleBuilder;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.systems.BossDeathSystem;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.systems.BossReloadSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

public class EZBosses {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void setup(JavaPlugin plugin) {
        LOGGER.atInfo().log("Setting up EZGame [Bosses]!");
        var registry = plugin.getEntityStoreRegistry();

        BossScalingComponent.setComponentType(registry.registerComponent(
            BossScalingComponent.class, BossScalingComponent.ID, BossScalingComponent.CODEC));
        BossMarkerComponent.setComponentType(registry.registerComponent(
            BossMarkerComponent.class, BossMarkerComponent.ID, BossMarkerComponent.CODEC));

        registry.registerSystem(new BossDeathSystem());
        registry.registerSystem(new BossReloadSystem());

        GaiaBossRoleBuilder.register();
    }
}
