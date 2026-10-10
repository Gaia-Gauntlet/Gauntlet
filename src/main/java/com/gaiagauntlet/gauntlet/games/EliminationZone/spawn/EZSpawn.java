package com.gaiagauntlet.gauntlet.games.EliminationZone.spawn;

import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.components.RisingBlockComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.cornucopia.CornucopiaMovingPlatformsHandler;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.listeners.LobbyEventListeners;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.systems.GameTimerSystem;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.systems.RisingBlockSystem;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.systems.SpawnDamagePreventionSystem;
import com.gaiagauntlet.gauntlet.plugins.events.events.MatchEventRegistry;
import com.gaiagauntlet.gauntlet.plugins.gamestate.events.MatchStateEvent;
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

        plugin.getEntityStoreRegistry().registerSystem(new RisingBlockSystem());
        plugin.getEntityStoreRegistry().registerSystem(new GameTimerSystem());
        plugin.getEntityStoreRegistry().registerSystem(new SpawnDamagePreventionSystem());
    }
}
