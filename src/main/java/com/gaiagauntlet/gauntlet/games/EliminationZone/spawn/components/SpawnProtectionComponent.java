package com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.components;

import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponentType;
import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameComponentRegistry;
import lombok.Getter;
import lombok.Setter;

public class SpawnProtectionComponent implements GameComponent {
    public static final String ID = "SpawnProtectionComponent";
    @Getter @Setter private static GameComponentType<SpawnProtectionComponent> componentType =
        GameComponentRegistry.register(ID, SpawnProtectionComponent.class);

    public SpawnProtectionComponent() {}

    @Override
    public SpawnProtectionComponent clone() {
        return new SpawnProtectionComponent();
    }
}
