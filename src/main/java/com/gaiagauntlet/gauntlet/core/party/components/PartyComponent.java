package com.gaiagauntlet.gauntlet.core.party.components;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import lombok.Getter;

import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.jetbrains.annotations.NotNull;

import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.msg;

public class PartyComponent {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final BuilderCodec<@NotNull PartyComponent> CODEC = BuilderCodec
            .builder(PartyComponent.class, PartyComponent::new)
            .append(new KeyedCodec<>("Id", Codec.STRING),
                    (c, v) -> c.id = v,
                    c -> c.id)
            .add()
            .append(new KeyedCodec<>("Label", Codec.STRING),
                    (c, v) -> c.label = v,
                    c -> c.label)
            .add()
            .append(new KeyedCodec<>("Players", new SetCodec<>(Codec.UUID_STRING, HashSet::new, false)),
                    (c, v) -> {
                        c.players.clear();
                        c.players.addAll(v);
                    },
                    c -> c.players)
            .add()
            .append(new KeyedCodec<>("Owner", Codec.UUID_STRING),
                    (c, v) -> c.owner = v,
                    c -> c.owner)
            .add()
            .build();

    @Getter
    String id;
    @Getter
    String label;
    Set<UUID> players = new HashSet<>();
    @Getter
    UUID owner;
    // list of offline players with their cancellation token - change type of
    // 'string' once that cancellation token type is known
    Map<UUID, ScheduledFuture<?>> offlinePlayers = new ConcurrentHashMap<>();

    private PartyComponent() {
    }

    public PartyComponent(String id, String label, Set<UUID> players) {
        if (players.isEmpty()) {
            throw new InvalidParameterException("Party cannot be formed with no players");
        }
        this.id = id;
        this.label = label;
        this.players = players;
        this.owner = players.stream().findAny().get();
    }

    public PartyComponent(String id, String label, Set<UUID> players, UUID owner) {
        this.id = id;
        this.label = label;
        this.players = players;
        this.owner = owner;
    }

    public void addPlayer(UUID player) {
        var playerRef = PlayerUtils.get(player);
        if (Objects.nonNull(playerRef)) {
            sendMessage(msg("server.gg.commands.party.joined")
                    .param("player", playerRef.getUsername()));
        }
        players.add(player);
    }

    public boolean removePlayer(UUID player) {
        var playerRef = PlayerUtils.get(player);

        clearOffline(player);

        boolean isOwner = owner.equals(player);
        if (isOwner && players.size() <= 1) {
            GauntletUtils.withResource().removeParty(id);
            return true;
        }
        boolean removed = players.remove(player);
        if (removed) {
            if (Objects.nonNull(playerRef)) {
                sendMessage(msg("server.gg.commands.party.left")
                        .param("player", playerRef.getUsername()));
            }
        }
        if (isOwner)
            setOwner(players.stream().findAny().get());
        return removed;
    }

    public void setOwner(UUID owner) {
        if (owner == null) {
            LOGGER.atSevere().log("Parties must always have an owner");
            return;
        } else if (!players.contains(owner)) {
            LOGGER.atSevere().log("Player with UUID " + owner + " cannot be set as owner of" +
                    "party with ID " + id + "because they are not in the party.");
            return;
        }
        this.owner = owner;
    }

    public List<PlayerRef> getAllOnlinePlayers() {
        var players = new ArrayList<PlayerRef>();
        for (UUID uuid : this.players) {
            var player = PlayerUtils.get(uuid);
            if (Objects.nonNull(player)) {
                players.add(player);
            }
        }
        return players;
    }

    public Set<UUID> getAllPlayers() {
        return players;
    }

    public boolean includesPlayer(PlayerRef player) {
        if (!offlinePlayers.containsKey(player.getUuid()))
            return includesPlayer(player.getUuid());

        setOnline(player.getUuid());
        return true;
    }
    public boolean includesPlayer(UUID player) {
        if (players.contains(player))
            return true;
        
        return false;
    }

    public int size() {
        return players.size();
    }

    public void sendMessage(Message message) {
        for (PlayerRef partyMember : getAllOnlinePlayers()) {
            partyMember.sendMessage(message);
        }
    }

    // moves a player back to being connected
    public void setOffline(UUID playerId, ScheduledFuture<?> disconnectFuture) {
        if (!players.remove(playerId))
            return;
        offlinePlayers.put(playerId, disconnectFuture);
    }

    // clears all offline players
    public void clearOffline() {
        for (var offlinePlayer : offlinePlayers.entrySet()) {
            clearOffline(offlinePlayer.getKey());
        }
    }

    // clears an offline player
    public void clearOffline(UUID playerId) {
        if (offlinePlayers.remove(playerId) == null)
            return;
        players.remove(playerId);
    }

    /**
     * sets a player as online again - removing their offine token
     * 
     * @returns the cancel token
     */
    public ScheduledFuture<?> setOnline(UUID playerId) {
        var cancelToken = offlinePlayers.get(playerId);
        if (cancelToken == null)
            return null;
        offlinePlayers.remove(playerId); // remove from offline
        if (cancelToken.isCancelled())
            return cancelToken;
        cancelToken.cancel(false);
        return cancelToken;
    }

    public Set<UUID> getOffline() {
        return this.offlinePlayers.keySet();
    }
}
