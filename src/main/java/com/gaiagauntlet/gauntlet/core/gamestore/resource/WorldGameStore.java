package com.gaiagauntlet.gauntlet.core.gamestore.resource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameStore;
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
                    new MapCodec<>(GameStore.CODEC, ConcurrentHashMap::new, false)),
                    (resource, v) -> resource.games = v,
                    resource -> resource.games)
            .add()
            .build();

    // we need to verify if you can have more than one game in a store at a time. Because otherwise, this should just be a single game 
    private Map<String, GameStore> games;

    @Override
    public WorldGameStore clone() {
        return new WorldGameStore();
    }

}
