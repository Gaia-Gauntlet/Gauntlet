package com.gaiagauntlet.gauntlet.games.EliminationZone.combat.components;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponentType;

import lombok.Getter;
import lombok.Setter;

public class EZCombatComponent implements GameComponent {
    @Getter @Setter private static GameComponentType<@NotNull EZCombatComponent> componentType;
    public static final String ID = "EZCombatComponent";

    private record Hit(UUID attacker, long atMillis) {
    }

    private final Map<UUID, Hit> lastHit = new ConcurrentHashMap<>();

    public void recordDamage(@Nonnull UUID victim, @Nonnull UUID attacker) {
        if (!victim.equals(attacker)) {
            lastHit.put(victim, new Hit(attacker, System.currentTimeMillis()));
        }
    }

    @Nullable
    public UUID recentAttacker(@Nonnull UUID victim, double windowSeconds) {
        var hit = lastHit.remove(victim);
        if (hit == null || System.currentTimeMillis() - hit.atMillis() > windowSeconds * 1000) {
            return null;
        }
        return hit.attacker();
    }
}
