package com.gaiagauntlet.gauntlet.plugins.teams.components;

import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.assets.TeamAsset;
import com.hypixel.hytale.logger.HytaleLogger;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

@AllArgsConstructor
@ToString
public abstract class TeamComponent {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Getter final String id;
    @Getter @Nonnull String name;
    @Getter @Nonnull TeamType teamType;
    @Getter @Nonnull UUID[] players;
    String icon;

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

    public TeamComponent(TeamAsset asset) {
        this(
            asset.getId(),
            asset.getName(),
            asset.getTeamType(),
            asset.getPlayers().length,
            asset.getIcon()
        );
        this.players = asset.getPlayers();
    }

    public TeamComponent() {
        id = "";
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

    public boolean isParticipant() {
        return teamType == TeamType.Participant;
    }

    public boolean isVisible() {
        return isParticipant();
    }

    synchronized void clearMembers() {
        players = new UUID[0];
    }

    public boolean isFull() {
        return Arrays.stream(players).noneMatch(Objects::isNull);
    }

    public int getSize() {
        return players.length;
    }

    public synchronized boolean contains(@Nonnull UUID uuid) {
        return Arrays.asList(players).contains(uuid);
    }

    public boolean add(@Nonnull UUID newUuid) {
        for (int i = 0; i < players.length; i++) {
            UUID uuid = players[i];
            if (Objects.isNull(uuid)) {
                players[i] = newUuid;
                return true;
            }
        }
        LOGGER.atSevere().log("Could not add new UUID " + newUuid + " to team " + id
            + "because there are no available spaces."
        );
        return false;
    }

    public boolean remove(@Nonnull UUID uuidToRemove) {
        for (int i = 0; i < players.length; i++) {
            UUID uuid = players[i];
            if (uuid.equals(uuidToRemove)) {
                players[i] = null;
                return true;
            }
        }
        LOGGER.atSevere().log("Could not add remove UUID " + uuidToRemove + " from team " + id
            + "because they are not in this team."
        );
        return false;
    }

    public void clear() {
        this.players = new UUID[this.players.length];
    }
}
