package com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.systems;

import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfig;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.TimerDisplay;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.components.GameTimerComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.interfaces.GameTickingSystem;
import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameQuery;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class GameTimerSystem extends GameTickingSystem {

    public GameTimerSystem() {}

    @Override
    public void tick(float dt, String sessionId, GameEcs gameStore, Store<EntityStore> store) {
        var gameTimerComponent = gameStore.get(GameTimerComponent.getComponentType()).orElse(null);
        if (gameTimerComponent == null || !gameTimerComponent.isRunning()) return;
        var world = store.getExternalData().getWorld();

        var continueTimer = gameTimerComponent.progressTimer(dt);
        var targets = EZGameConfig.get(gameStore).getArenaTimerPois();

        if (!continueTimer) {
            // do end of timer stuff
            gameTimerComponent.getOnComplete().run();
            for (var target : targets) {
                TimerDisplay.clear(world, target.getTransform());
            }
            return;
        }
        var lastUpdateTime = gameTimerComponent.getLastUpdateTime();
        var timeRemaining = gameTimerComponent.getCountdownRemaining();
        if (Math.abs(lastUpdateTime - timeRemaining) <= 1) {
            // has been less than a second since last update - defer until a full second has passed
            return;
        }
        gameTimerComponent.sync();
        for (var target : targets) {
            TimerDisplay.showBase(world, target.getTransform());
            TimerDisplay.showTime(world, target.getTransform(), (int) Math.floor(timeRemaining));
        }
    }

    @Override
    public GameQuery getGameQuery() {
        return GameQuery.of(GameTimerComponent.getComponentType());
    }
}
