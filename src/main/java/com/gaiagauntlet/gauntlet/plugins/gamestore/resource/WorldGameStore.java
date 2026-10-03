package com.gaiagauntlet.gauntlet.plugins.gamestore.resource;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import lombok.Getter;
import lombok.Setter;

public class WorldGameStore implements Resource<EntityStore> {
    public static final String ID = "WorldGameStore";
    @Getter
    @Setter
    private static ResourceType<EntityStore, WorldGameStore> resourceType;
    public static final BuilderCodec<@NotNull WorldGameStore> CODEC = BuilderCodec
            .builder(WorldGameStore.class, WorldGameStore::new)
            .append(new KeyedCodec<>("Games",
                    new MapCodec<>(GameEcs.CODEC, ConcurrentHashMap::new, false)),
                    (resource, v) -> resource.games = v,
                    resource -> resource.games)
            .add()
            .build();

    // gameId is just the current session
    private Map<String, GameEcs> games = new ConcurrentHashMap<>();

    public Optional<GameEcs> get(String sessionId) {
        return Optional.ofNullable(games.get(sessionId));
    }

    public GameEcs create(String sessionId) {
        var existing = get(sessionId);
        if (existing.isPresent()) return existing.orElseThrow();
        var game = new GameEcs();
        games.put(sessionId, game);
        return game;
    }

    public void clearGame(String sessionId) {
        var game = games.get(sessionId);
        game.clear();
    }

    @Override
    public WorldGameStore clone() {
        return new WorldGameStore();
    }

}
