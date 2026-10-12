package com.gaiagauntlet.gauntlet.plugins.career.systems;

import com.gaiagauntlet.gauntlet.plugins.career.components.CareerComponent;
import com.gaiagauntlet.gauntlet.plugins.career.components.CareerStats;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;

public class CareerSystems {
	public static void onPlayerConnect(PlayerConnectEvent event) {
		if (event.getPlayerRef() == null) {
			return;
		}

		var career = event.getHolder().ensureAndGetComponent(CareerComponent.getComponentType());
		career.increment(CareerStats.CONNECTIONS, 1L);
	}
}
