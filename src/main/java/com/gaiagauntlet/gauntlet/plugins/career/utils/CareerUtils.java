package com.gaiagauntlet.gauntlet.plugins.career.utils;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.plugins.career.components.CareerComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class CareerUtils {
    public static void ensurePlayerCareer(PlayerRef player) {
        var ref = player.getReference();
        var store = ref.getStore();
        var world = store.getExternalData().getWorld();
        GauntletUtils.run(world, () -> {
            store.ensureComponent(ref, CareerComponent.getComponentType());
        });
    }

    public static CareerComponent careerFor(PlayerRef player) {
        return player.getComponentConcurrent(CareerComponent.getComponentType());
    }

    public static void add(PlayerRef player, String gameId, String statId, Long amount) {
        careerFor(player).increment(gameId, statId, amount)
    }
    public static void add(PlayerRef player, CareerStats statId, Long amount) {
        careerFor(player).increment(statId, amount)
    }

    public static Long get(PlayerRef player, String gameId, String statId) {
        return careerFor(player).get(gameId, statId);
    }
    public static Long get(PlayerRef player, CareerStats statId) {
        return careerFor(player).get(statId);
    }
}
