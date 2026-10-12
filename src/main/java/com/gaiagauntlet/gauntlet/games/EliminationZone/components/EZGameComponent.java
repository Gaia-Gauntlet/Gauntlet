package com.gaiagauntlet.gauntlet.games.EliminationZone.components;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.plugins.auto.components.Standing;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponentType;

import lombok.Getter;
import lombok.Setter;

/** Logic and holds for the current game */
public class EZGameComponent implements GameComponent {
    @Getter @Setter private static GameComponentType<@NotNull EZGameComponent> componentType;
    public static final String ID = "EZGameComponent";
    @Getter @Setter private List<Standing> standings = List.of();
    private final Map<UUID, Long> eliminations = new ConcurrentHashMap<>();

    public EZGameComponent() {

    }

    public void eliminate(@Nonnull UUID player) {
        eliminations.putIfAbsent(player, System.currentTimeMillis());
    }

    public boolean isEliminated(@Nonnull UUID player) {
        return eliminations.containsKey(player);
    }

    public long eliminatedAt(@Nonnull UUID player) {
        return eliminations.getOrDefault(player, 0L);
    }
}