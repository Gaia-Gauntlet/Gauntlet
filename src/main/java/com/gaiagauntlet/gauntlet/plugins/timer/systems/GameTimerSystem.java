package com.gaiagauntlet.gauntlet.plugins.timer.systems;

import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.TimerDisplay;
import com.gaiagauntlet.gauntlet.plugins.events.events.MatchEventRegistry;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.interfaces.GameTickingSystem;
import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameQuery;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.timer.components.GameTimerComponent;
import com.gaiagauntlet.gauntlet.plugins.timer.events.TimerCompleteEvent;
import com.gaiagauntlet.gauntlet.plugins.timer.ui.TimerHud;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class GameTimerSystem extends GameTickingSystem {

    public GameTimerSystem() {
    }

    @Override
    public void tick(float dt, String sessionId, GameEcs gameStore, Store<EntityStore> store) {
        var gameTimerComponent = gameStore.get(GameTimerComponent.getComponentType()).orElse(null);
        if (gameTimerComponent == null || !gameTimerComponent.isRunning())
            return;
        var world = store.getExternalData().getWorld();

        var continueTimer = gameTimerComponent.progressTimer(dt);
        var targets = gameTimerComponent.getPois();

        if (!continueTimer) {
            // do end of timer stuff
            for (var target : targets) {
                TimerDisplay.clear(world, target.getTransform());
            }

            var ctx = GameStore.getContext(gameStore);

            MatchEventRegistry.dispatch(TimerCompleteEvent.of(gameStore, gameTimerComponent.getTimerId()), ctx.getGameId());
            // remove the timer
            gameStore.remove(GameTimerComponent.getComponentType());
            TimerHud.update(gameStore);
            return;
        }
        var lastUpdateTime = gameTimerComponent.getLastUpdateTime();
        var timeRemaining = gameTimerComponent.getCountdownRemaining();
        if (Math.abs(lastUpdateTime - timeRemaining) <= 1) {
            // has been less than a second since last update - defer until a full second has
            // passed
            return;
        }
        gameTimerComponent.sync();
        for (var target : targets) {
            TimerDisplay.showBase(world, target.getTransform());
            TimerDisplay.showTime(world, target.getTransform(), (int) Math.floor(timeRemaining));
        }
        TimerHud.update(gameStore);
    }

    @Override
    public GameQuery getGameQuery() {
        return GameQuery.of(GameTimerComponent.getComponentType());
    }
}
