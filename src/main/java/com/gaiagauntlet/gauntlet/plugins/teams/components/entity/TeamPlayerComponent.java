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
    @Getter @Setter private static ComponentType<EntityStore, TeamPlayerComponent> componentType;

    public static BuilderCodec<@NotNull TeamPlayerComponent> CODEC = BuilderCodec
            .builder(TeamPlayerComponent.class, TeamPlayerComponent::new)
            .append(
                    new KeyedCodec<>("Team", Codec.STRING),
                    TeamPlayerComponent::setTeam,
                    TeamPlayerComponent::getTeam)
            .documentation("The team that this entity is on.")
            .add()
            .append(
                    new KeyedCodec<>("Kills", Codec.INTEGER),
                    TeamPlayerComponent::setKills,
                    TeamPlayerComponent::getKills)
            .documentation("The number of players killed by this player.")
            .add()
            .build();

    @Getter @Setter private String team;
    /** The number of players killed by this player. */
    @Getter @Setter private int kills;
    public TeamPlayerComponent() {
        kills = 0;
    }
    public TeamPlayerComponent(TeamPlayerComponent other) {
        kills = other.kills;
        team = other.team;
    }

    public TeamPlayerComponent(String team) {
        this.team = team;
        kills = 0;
    }

    public int incrementKills() {
        return ++kills;
    }

    public void reset() {
        kills = 0;
    }

    @Override
    public @Nullable Component<EntityStore> clone() {
        return new TeamPlayerComponent(this);
    }
}
