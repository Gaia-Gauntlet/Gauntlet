package com.gaiagauntlet.gauntlet.core.components;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import lombok.Getter;
import lombok.Setter;

/** Simple player component for persistently storing per-player stats */
public class PlayerComponent implements Component<EntityStore> {
    @Setter
    @Getter
    private static ComponentType<EntityStore, PlayerComponent> componentType;
    public static final String ID = "GamePlayerComponent";
    public static final BuilderCodec<@NotNull PlayerComponent> CODEC = BuilderCodec
            .builder(PlayerComponent.class, PlayerComponent::new)
            .append(new KeyedCodec<>("CurrentSession", Codec.STRING),
                    (p, v) -> p.currentGame = v,
                    p -> p.getCurrentGame())
            .documentation("The cached current session for the player. Used for disconnect logic")
            .add()
            .build();

    @Getter
    @Setter 
    @Nullable
    private String currentGame;

    public PlayerComponent() {
    }

    public PlayerComponent(PlayerComponent other) {
        this.currentGame = other.currentGame;
    };

    public PlayerComponent clone() {
        return new PlayerComponent(this);
    }
}
