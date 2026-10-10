package com.gaiagauntlet.gauntlet.plugins.gamestore.registry;


import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponentType;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;

/**
 * A Query abstraction
 */
public final class GameQuery {
    private final Set<String> required;

    private GameQuery(Set<String> required) {
        this.required = required;
    }

    public static GameQuery of(GameComponentType<?>... types) {
        return new GameQuery(Arrays.stream(types).map(GameComponentType::getIndex).collect(Collectors.toUnmodifiableSet()));
    }

    public boolean test(GameEcs game) {
        return game.getSessionComponents().keySet().containsAll(required);
    }
}
