package com.gaiagauntlet.gauntlet.plugins.teams.components.entity;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.Getter;
import lombok.Setter;

import org.jetbrains.annotations.NotNull;

/**
 * Component to give a player when they have been eliminated from a game.
 */
public class EliminatedComponent implements Component<EntityStore> {
    @Getter @Setter private static ComponentType<EntityStore, EliminatedComponent> componentType;

    public enum EliminationReason {
        KILLED,
        DISCONNECT
    }

    public static BuilderCodec<@NotNull EliminatedComponent> CODEC = BuilderCodec
        .builder(EliminatedComponent.class, EliminatedComponent::new)
        .append(
            new KeyedCodec<>("EliminatedAt", Codec.LONG),
            EliminatedComponent::setEliminatedAt,
            EliminatedComponent::getEliminatedAt
        )
        .documentation("The time that this entity was eliminated.")
        .add()
        .append(
            new KeyedCodec<>("Reason", new EnumCodec<>(EliminationReason.class)),
            EliminatedComponent::setReason,
            EliminatedComponent::getReason
        )
        .documentation("The reason the player was eliminated (ui rendering).")
        .add()
        .build();

    @Getter @Setter private long eliminatedAt;
    @Getter @Setter private EliminationReason reason;

    public EliminatedComponent(EliminationReason reason) {
        this.eliminatedAt = System.currentTimeMillis();
        this.reason = reason;
    }
    public EliminatedComponent() {
        this.eliminatedAt = System.currentTimeMillis();
        this.reason = EliminationReason.KILLED;
    }

    public EliminatedComponent(EliminatedComponent other) {
        this.eliminatedAt = other.eliminatedAt;
        this.reason = other.reason;
    }

    @Override
    public Component<EntityStore> clone() {
        return new EliminatedComponent(this);
    }
}
