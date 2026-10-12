package com.gaiagauntlet.gauntlet.plugins.chat.config;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.utils.PlayerUtils;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.asset.util.ColorParseUtil;
import com.hypixel.hytale.server.core.codec.ProtocolCodecs;

import lombok.Getter;

public class ChatRank {
    @Nonnull
    public static final BuilderCodec<@NotNull ChatRank> CODEC = BuilderCodec
            .builder(ChatRank.class, ChatRank::new)
            .append(new KeyedCodec<>("Prefix", Codec.STRING),
                    (rank, v) -> rank.prefix = v,
                    rank -> rank.getPrefix())
            .documentation("The tag shown before the player's name, such as [DEV]")
            .add()
            .append(new KeyedCodec<>("Color", ProtocolCodecs.COLOR),
                    (rank, v) -> rank.color = v,
                    rank -> rank.getColor())
            .documentation("The color of the prefix and the player's name")
            .add()
            .append(new KeyedCodec<>("Priority", Codec.INTEGER),
                    (rank, v) -> rank.priority = v,
                    rank -> rank.getPriority())
            .documentation("The rank shown when a player holds several. The highest priority wins")
            .add()
            .append(new KeyedCodec<>("Players", new SetCodec<>(Codec.STRING, HashSet::new, false)),
                    (rank, v) -> rank.players = v.stream().map(PlayerUtils::normalize).collect(Collectors.toUnmodifiableSet()),
                    rank -> rank.getPlayers())
            .documentation("Lowercase usernames of the players who hold this rank")
            .add()
            .build();

    @Getter
    private String prefix = "";
    @Getter
    private Color color = ColorParseUtil.parseColor("#FFFFFF");
    @Getter
    private int priority;
    @Getter
    private Set<String> players = Set.of();

    public boolean includes(@Nonnull String username) {
        return players.contains(PlayerUtils.normalize(username));
    }
}
