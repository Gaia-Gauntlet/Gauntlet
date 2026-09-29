package com.gaiagauntlet.gauntlet.plugins.teams.components;

import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

@AllArgsConstructor
public class TeamComponent implements GameComponent {
    @Getter private final String id;
    @Getter @Nonnull private String name;
    @Getter @Nonnull private TeamType teamType;
    @Getter @Nonnull private UUID[] players;
    private String icon;

    public TeamComponent(
        String id,
        @Nullable String name,
        @Nullable TeamType teamType,
        int maxSize,
        @Nullable String icon
    ) {
        this.id = id;
        this.name = name != null ? name : "";
        this.teamType = teamType != null ? teamType : TeamType.Participant;
        this.players = new UUID[maxSize];
        this.icon = icon;
    }

    /**
     * The icon path relative to {@code UI/Custom/}, or an empty string when the
     * team has no icon.
     */
    @Nonnull
    public String getUiIcon() {
        String prefix = "UI/Custom/";
        return icon.startsWith(prefix) ? icon.substring(prefix.length()) : icon;
    }

    public boolean isFull() {
        return Arrays.stream(players).noneMatch(Objects::isNull);
    }

    public boolean isVisible() {
        return teamType.equals(TeamType.Participant);
    }

    synchronized void clearMembers() {
        players = new UUID[0];
    }
}
