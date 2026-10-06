package com.gaiagauntlet.gauntlet.games.EliminationZone.bosses;

import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.components.BossMarkerComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.components.BossScalingComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.roles.GaiaBossRoleBuilder;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.systems.BossDeathSystem;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.systems.BossReloadSystem;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

// placeholder file to get the folder structure setup, will be deleted later prolly
public class EZBosses {
    public static void setup(JavaPlugin plugin) {
        var registry = plugin.getEntityStoreRegistry();

        BossScalingComponent.setType(registry.registerComponent(
            BossScalingComponent.class, "BossScaling", BossScalingComponent.CODEC));
        BossMarkerComponent.setType(registry.registerComponent(
            BossMarkerComponent.class, "GGBossMarker", BossMarkerComponent.CODEC));

        registry.registerSystem(new BossDeathSystem());
        registry.registerSystem(new BossReloadSystem());

        GaiaBossRoleBuilder.register();
    }
}
