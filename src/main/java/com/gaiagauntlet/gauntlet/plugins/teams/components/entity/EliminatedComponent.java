package com.gaiagauntlet.gauntlet.plugins.teams.components.entity;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * Component to give a player when they have been eliminated from a game.
 */
public class EliminatedComponent implements Component<EntityStore> {
    @Getter @Setter private static ComponentType<EntityStore, EliminatedComponent> componentType;

    public static BuilderCodec<EliminatedComponent> CODEC = BuilderCodec
        .builder(EliminatedComponent.class, EliminatedComponent::new)
        .append(
            new KeyedCodec<>("EliminatedAt", Codec.LONG),
            EliminatedComponent::setEliminatedAt,
            EliminatedComponent::getEliminatedAt
        )
        .documentation("The time that this entity was eliminated.")
        .add()
        .build();

    @Getter @Setter private long eliminatedAt;

    public EliminatedComponent() {}

    public EliminatedComponent(long eliminatedAt) {
        this.eliminatedAt = eliminatedAt;
    }

    @Override
    public @Nullable Component<EntityStore> clone() {
        return new EliminatedComponent(eliminatedAt);
    }
}
