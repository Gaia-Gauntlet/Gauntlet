package com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Tags a spawned boss entity with the game and boss it belongs to. Saved with the entity, so a boss
 * whose chunk unloads is still recognised when it loads again.
 */
public final class BossMarkerComponent implements Component<EntityStore> {

    public static final BuilderCodec<BossMarkerComponent> CODEC = BuilderCodec.builder(BossMarkerComponent.class, BossMarkerComponent::new)
            .append(new KeyedCodec<>("GameId", Codec.STRING), (m, v) -> m.gameId = v == null ? "" : v, m -> m.gameId)
            .add()
            .append(new KeyedCodec<>("BossId", Codec.STRING), (m, v) -> m.bossId = v == null ? "" : v, m -> m.bossId)
            .add()
            .build();

    private static ComponentType<EntityStore, BossMarkerComponent> type;

    private String gameId = "";
    private String bossId = "";

    public BossMarkerComponent() {
    }

    public BossMarkerComponent(@Nonnull String gameId, @Nonnull String bossId) {
        this.gameId = gameId;
        this.bossId = bossId;
    }

    public static void setType(@Nonnull ComponentType<EntityStore, BossMarkerComponent> componentType) {
        type = componentType;
    }

    @Nonnull
    public static ComponentType<EntityStore, BossMarkerComponent> getComponentType() {
        return type;
    }

    @Nonnull
    public String gameId() {
        return gameId;
    }

    @Nonnull
    public String bossId() {
        return bossId;
    }

    @Override
    public BossMarkerComponent clone() {
        return new BossMarkerComponent(gameId, bossId);
    }
}
