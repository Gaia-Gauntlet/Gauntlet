package com.gaiagauntlet.gauntlet.plugins.teams.components;

import com.gaiagauntlet.gauntlet.plugins.teams.components.entity.EliminatedComponent;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIDisplayMode;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.common.CommonAssetValidator;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import lombok.Getter;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** The single component for an entire team. Holds all team-specific data */
public class TeamComponent {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static BuilderCodec<@NotNull TeamComponent> CODEC = BuilderCodec
            .builder(TeamComponent.class, TeamComponent::new)
            .append(new KeyedCodec<>("Name", Codec.STRING),
                    (team, v) -> team.name = v == null ? "" : v,
                    team -> team.name)
            .documentation("The display name shown to players. Defaults to the asset id when blank.")
            .add()
            .append(new KeyedCodec<>("TeamType", new EnumCodec<>(TeamType.class)),
                    (team, v) -> team.teamType = v,
                    team -> team.teamType)
            .documentation("The display name shown to players. Defaults to the asset id when blank.")
            .add()
            .append(new KeyedCodec<>("Icon", Codec.STRING),
                    (team, v) -> team.icon = v == null ? "" : v,
                    team -> team.icon)
            .addValidator(new CommonAssetValidator("png", "UI/Custom/"))
            .documentation("UI asset team icon, for example GG/TeamIcons/Tricky_Trorks.png. Optional.")
            .add()
            .append(new KeyedCodec<>("Players", new ArrayCodec<>(Codec.STRING, String[]::new)),
                    (team, v) -> team.rawPlayerNames = v,
                    team -> team.rawPlayerNames)
            .documentation("The team roster")
            .add()
            .append(new KeyedCodec<>("PlayerUUIDs", new ArrayCodec<>(Codec.UUID_STRING, UUID[]::new)),
                    (team, v) -> team.players = v,
                    team -> team.players)
            .documentation("The team roster")
            .metadata(UIDisplayMode.HIDDEN)
            .add()
            .afterDecode(team -> {
                // converts the player names into valid UUIDs in the event that the button was
                // not pressed

                // this also genuinely is a very sphaghetti way to handle this. I would like to clean this up a bit later
                var entries = team.rawPlayerNames;
                if (entries == null || entries.length == 0) return;
                var lookups = Arrays.stream(entries)
                        .map(entry -> {
                            if (entry == null) return null;
                            var uuid = parseUuid(entry);
                            return uuid != null
                                    ? CompletableFuture.completedFuture(uuid)
                                    : PlayerUtils.uuidOf(entry)
                                            .exceptionally(err -> null);
                        })
                        .toList();

                CompletableFuture.allOf(lookups.toArray(CompletableFuture[]::new)).join();

                var players = new ArrayList<UUID>(entries.length);
                for (var i = 0; i < entries.length; i++) {
                    var uuid = lookups.get(i).join();
                    if (uuid == null) {
                        LOGGER.atWarning().log("Team roster entry '%s' could not be resolved to a player", entries[i]);
                        continue;
                    }
                    players.add(uuid);
                }
                team.players = players.toArray(UUID[]::new);
            })
            .build();

    public static UUID parseUuid(@Nonnull String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Getter final String id;
    @Getter @Nonnull String name = "";
    @Getter @Nonnull TeamType teamType = TeamType.Participant;
    @Getter @Nonnull UUID[] players = new UUID[0];
    /**
     * List of player names - this is ONLY intended to be added via the asset
     * editor. Values normalized into the player's UUIDs after decoding. Ideally, this is never accessed anywhere
     * <br /><br />
     * Again, do NOT use this anywhere. Only use the `players` list
     */
    @Nonnull @Getter String[] rawPlayerNames = new String[0];
    String icon;

    public TeamComponent(
            String id,
            @Nullable String name,
            @Nullable TeamType teamType,
            int maxSize,
            @Nullable String icon) {
        this.id = id;
        this.name = name != null ? name : "";
        this.teamType = teamType != null ? teamType : TeamType.Participant;
        this.players = new UUID[maxSize];
        this.icon = icon;
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
                + "because there are no available spaces.");
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
                + "because they are not in this team.");
        return false;
    }

    public void clear() {
        this.players = new UUID[this.players.length];
    }

    public TeamComponent clone() {
        var team = new TeamComponent(
            this.id,
            this.name,
            this.teamType,
            this.players.length,
            this.icon
        );
        for (UUID player : this.players) {
            team.add(player);
        }
        return team;
    }
}
