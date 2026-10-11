package com.gaiagauntlet.gauntlet.games.EliminationZone.spawn;

import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.components.RisingBlockComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.cornucopia.CornucopiaMovingPlatformsHandler;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.listeners.LobbyEventListeners;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.systems.RisingBlockTickingSystem;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.systems.SpawnDamagePreventionSystem;
import com.gaiagauntlet.gauntlet.plugins.events.events.MatchEventRegistry;
import com.gaiagauntlet.gauntlet.plugins.gamestate.events.MatchStateEvent;
import com.gaiagauntlet.gauntlet.plugins.timer.utils.TimerUtils;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

public class EZSpawn {
    private EZSpawn() {
    }

    public static void setup(JavaPlugin plugin) {
        MatchEventRegistry.register(MatchStateEvent.class, EZController.ID, LobbyEventListeners::onMatchState);
        MatchEventRegistry.register(MatchStateEvent.class, EZController.ID,
                CornucopiaMovingPlatformsHandler::onMatchStart);

        RisingBlockComponent
                .setComponentType(plugin.getEntityStoreRegistry().registerComponent(RisingBlockComponent.class,
                        RisingBlockComponent::new));

        TimerUtils.registerOnComplete("SpawnProtection", EZController.ID, SpawnDamagePreventionSystem::onEnd);

        plugin.getEntityStoreRegistry().registerSystem(new RisingBlockTickingSystem());
        plugin.getEntityStoreRegistry().registerSystem(new SpawnDamagePreventionSystem());
    }
}
