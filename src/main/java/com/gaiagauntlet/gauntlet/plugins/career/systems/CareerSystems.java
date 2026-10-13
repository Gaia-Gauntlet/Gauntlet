package com.gaiagauntlet.gauntlet.plugins.career.systems;

import com.gaiagauntlet.gauntlet.plugins.career.components.CareerComponent;
import com.gaiagauntlet.gauntlet.plugins.career.components.CareerStats;
import com.gaiagauntlet.gauntlet.plugins.career.utils.CareerUtils;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;

import java.util.concurrent.TimeUnit;

public class CareerSystems {
	public static void onPlayerConnect(PlayerConnectEvent event) {
        var career = event.getHolder().ensureAndGetComponent(CareerComponent.getComponentType());
        career.increment(CareerStats.CONNECTIONS, 1L);
	}

    public static void onPlayerDisconnect(PlayerDisconnectEvent event) {
        CareerUtils.ensurePlayerCareer(event.getPlayerRef());
        var career = CareerUtils.careerFor(event.getPlayerRef());
        career.increment(CareerStats.DISCONNECTIONS, 1L);
    }
}
