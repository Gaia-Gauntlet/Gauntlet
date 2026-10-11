package com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;

/**
 * Tags a spawned boss entity with the game and boss it belongs to. Saved with the entity, so a boss
 * whose chunk unloads is still recognised when it loads again.
 */
public final class BossMarkerComponent implements Component<EntityStore> {
    public static final String ID = "BossMarker";

    public static final BuilderCodec<BossMarkerComponent> CODEC = BuilderCodec.builder(BossMarkerComponent.class, BossMarkerComponent::new)
            .append(new KeyedCodec<>("GameSession", Codec.STRING), (m, v) -> m.sessionId = v == null ? "" : v, m -> m.sessionId)
            .add()
            .append(new KeyedCodec<>("BossId", Codec.STRING), (m, v) -> m.bossId = v == null ? "" : v, m -> m.bossId)
            .add()
            .build();

    @Getter @Setter
    private static ComponentType<EntityStore, BossMarkerComponent> componentType;

    @Getter private String sessionId = "";
    @Getter private String bossId = "";

    private BossMarkerComponent() {
    }

    public BossMarkerComponent(@Nonnull String sessionId, @Nonnull String bossId) {
        this.sessionId = sessionId;
        this.bossId = bossId;
    }

    @Override
    public BossMarkerComponent clone() {
        return new BossMarkerComponent(sessionId, bossId);
    }
}
