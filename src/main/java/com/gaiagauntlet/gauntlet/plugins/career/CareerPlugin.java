package com.gaiagauntlet.gauntlet.plugins.career;

import java.util.List;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.gaiagauntlet.gauntlet.plugins.career.components.CareerComponent;
import com.gaiagauntlet.gauntlet.plugins.career.systems.CareerSystems;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

public class CareerPlugin implements GamePlugin {
    public static final String ID = "CareerPlugin";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<String> getDependencies() {
        return List.of();
    }

    @Override
    public void init(JavaPlugin plugin) {
        // inits listeners
        var registry = plugin.getEntityStoreRegistry();
        CareerComponent.setComponentType(
                registry.registerComponent(CareerComponent.class, CareerComponent.ID, CareerComponent.CODEC));
        plugin.getEventRegistry().register(PlayerConnectEvent.class, CareerSystems::onPlayerConnect);
    }

}
