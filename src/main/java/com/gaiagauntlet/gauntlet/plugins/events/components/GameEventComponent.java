package com.gaiagauntlet.gauntlet.plugins.events.components;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponentType;
import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameComponentRegistry;

import lombok.Getter;

public class GameEventComponent implements GameComponent {
    public static final String ID = "GameEventComponent";
    @Getter
    private static final GameComponentType<@NotNull GameEventComponent> componentType = GameComponentRegistry.register(ID,
            GameEventComponent.class);

    
}
