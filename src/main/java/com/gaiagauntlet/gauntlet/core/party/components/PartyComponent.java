package com.gaiagauntlet.gauntlet.core.party.components;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import lombok.Getter;

import java.security.InvalidParameterException;
import java.util.*;

import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.msg;

public class PartyComponent {
    private static HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final BuilderCodec<PartyComponent> CODEC = BuilderCodec
        .builder(PartyComponent.class, PartyComponent::new)
        .append(new KeyedCodec<>("Id", Codec.STRING),
            (c, v) -> c.id = v,
            c -> c.id
        ).add()
        .append(new KeyedCodec<>("Players", new ArrayCodec<>(Codec.UUID_STRING, UUID[]::new)),
            (c, v) -> c.players = v,
            c -> c.players
        ).add()
        .append(new KeyedCodec<>("Owner", Codec.UUID_STRING),
            (c, v) -> c.owner = v,
            c -> c.owner
        ).add()
        .build();

    @Getter String id;
    UUID[] players = new UUID[0];
    @Getter UUID owner;

    private PartyComponent() {}

    public PartyComponent(String id, UUID[] players) {
        if (players.length == 0) {
            throw new InvalidParameterException("Party cannot be formed with no players");
        }
        this.id = id;
        this.players = players;
        this.owner = players[0];
    }
    public PartyComponent(String id, UUID[] players, UUID owner) {
        this.id = id;
        this.players = players;
        this.owner = owner;
    }

    public void addPlayer(UUID player) {
        var playerRef = PlayerUtils.get(player);
        if (Objects.nonNull(playerRef)) {
            sendMessage(msg("server.gg.commands.party.joined")
                .param("player", playerRef.getUsername())
            );
        }

        var playerList = new ArrayList<>(Arrays.stream(players).toList());
        playerList.add(player);
        players = playerList.toArray(playerList.toArray(new UUID[0]));
    }

    public boolean removePlayer(UUID player) {
        var playerRef = PlayerUtils.get(player);

        boolean isOwner = owner.equals(player);
        if (isOwner && players.length <= 1) {
            GauntletUtils.withResource().removeParty(id);
            return true;
        }
        var playerList = new ArrayList<>(Arrays.stream(players).toList());
        boolean removed = playerList.remove(player);
        if (removed) {
            players = playerList.toArray(playerList.toArray(new UUID[0]));
            if (Objects.nonNull(playerRef)) {
                sendMessage(msg("server.gg.commands.party.left")
                    .param("player", playerRef.getUsername())
                );
            }
        }
        if (isOwner) setOwner(playerList.getFirst());
        return removed;
    }

    public void setOwner(UUID owner) {
        if (owner == null) {
            LOGGER.atSevere().log("Parties must always have an owner");
            return;
        } else if (Arrays.stream(players).noneMatch(p -> p.equals(owner))) {
            LOGGER.atSevere().log("Player with UUID " + owner + " cannot be set as owner of" +
                "party with ID " + id + "because they are not in the team.");
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

    public boolean includesPlayer(UUID player) {
        return Arrays.asList(players).contains(player);
    }

    public int size() {
        return players.length;
    }

    public void sendMessage(Message message) {
        for (PlayerRef partyMember : getAllOnlinePlayers()) {
            partyMember.sendMessage(message);
        }
    }
}
