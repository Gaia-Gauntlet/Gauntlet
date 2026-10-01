package com.gaiagauntlet.gauntlet.plugins.teams.components.entity;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.Getter;
import lombok.Setter;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

/**
 * Component to give a player when they have been eliminated from a game.
 */
public class TeamPlayerComponent implements Component<EntityStore> {
    @Getter
    @Setter
    private static ComponentType<EntityStore, TeamPlayerComponent> componentType;

    public static BuilderCodec<@NotNull TeamPlayerComponent> CODEC = BuilderCodec
            .builder(TeamPlayerComponent.class, TeamPlayerComponent::new)
            .append(
                    new KeyedCodec<>("Team", Codec.STRING),
                    TeamPlayerComponent::setTeam,
                    TeamPlayerComponent::getTeam)
            .documentation("The time that this entity was eliminated.")
            .add()
            .append(
                    new KeyedCodec<>("Score", Codec.DOUBLE),
                    TeamPlayerComponent::setScore,
                    TeamPlayerComponent::getScore)
            .documentation("The time that this entity was eliminated.")
            .add()
            .build();

    @Getter @Setter private String team;
    /** Generic score the player holds. Shows up next to their username on refresh */
    @Getter @Setter private Double score;
    public TeamPlayerComponent() {
        score = 0.0d;
    }
    public TeamPlayerComponent(TeamPlayerComponent other) {
        score = other.score;
        team = other.team;
    }

    public TeamPlayerComponent(String team) {
        this.team = team;
        score = 0.0d;
    }

    @Override
    public @Nullable Component<EntityStore> clone() {
        return new TeamPlayerComponent(this);
    }
}
