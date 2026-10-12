package com.gaiagauntlet.gauntlet.plugins.chat.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.asset.util.ColorParseUtil;
import com.hypixel.hytale.server.core.codec.ProtocolCodecs;
import com.riprod.configly.Config;
import com.riprod.configly.Configly;

import lombok.Getter;

public class ChatConfig extends Config {
    @Nonnull
    public static final String TYPE = "GaiaChat";

    @Nonnull
    public static final BuilderCodec<@NotNull ChatConfig> CODEC = BuilderCodec
            .builder(ChatConfig.class, ChatConfig::new)
            .append(new KeyedCodec<>("Ranks", new MapCodec<>(ChatRank.CODEC, HashMap::new)),
                    (config, v) -> config.ranks = v,
                    config -> config.getRanks())
            .documentation("Ranks shown before a player's name in chat, keyed by rank id")
            .add()
            .append(new KeyedCodec<>("MessageColor", ProtocolCodecs.COLOR),
                    (config, v) -> config.messageColor = v,
                    config -> config.getMessageColor())
            .documentation("The color of message text in global, party, team, session and spectator chat")
            .add()
            .append(new KeyedCodec<>("PartyColor", ProtocolCodecs.COLOR),
                    (config, v) -> config.partyColor = v,
                    config -> config.getPartyColor())
            .documentation("The color of the party chat tag")
            .add()
            .append(new KeyedCodec<>("TeamColor", ProtocolCodecs.COLOR),
                    (config, v) -> config.teamColor = v,
                    config -> config.getTeamColor())
            .documentation("The color of the team chat tag")
            .add()
            .append(new KeyedCodec<>("SessionColor", ProtocolCodecs.COLOR),
                    (config, v) -> config.sessionColor = v,
                    config -> config.getSessionColor())
            .documentation("The color of the session chat tag")
            .add()
            .append(new KeyedCodec<>("AnnouncementColor", ProtocolCodecs.COLOR),
                    (config, v) -> config.announcementColor = v,
                    config -> config.getAnnouncementColor())
            .documentation("The color of the announcement tag and the whole announced line")
            .add()
            .append(new KeyedCodec<>("SpectatorColor", ProtocolCodecs.COLOR),
                    (config, v) -> config.spectatorColor = v,
                    config -> config.getSpectatorColor())
            .documentation("The color of the spectator chat tag")
            .add()
            .append(new KeyedCodec<>("WhisperColor", ProtocolCodecs.COLOR),
                    (config, v) -> config.whisperColor = v,
                    config -> config.getWhisperColor())
            .documentation("The color of the whisper tag and the whole whispered line")
            .add()
            .build();

    private static final ChatConfig DEFAULTS = new ChatConfig();

    @Nonnull
    public static ChatConfig get() {
        return Configly.getOrElse(TYPE, ChatConfig.class, DEFAULTS);
    }

    @Getter
    private Map<String, ChatRank> ranks = Map.of();
    @Getter
    private Color messageColor = ColorParseUtil.parseColor("#FFFFFF");
    @Getter
    private Color partyColor = ColorParseUtil.parseColor("#6CB4FF");
    @Getter
    private Color teamColor = ColorParseUtil.parseColor("#6EE07F");
    @Getter
    private Color sessionColor = ColorParseUtil.parseColor("#C9A0FF");
    @Getter
    private Color announcementColor = ColorParseUtil.parseColor("#FFD54A");
    @Getter
    private Color spectatorColor = ColorParseUtil.parseColor("#8C8C8C");
    @Getter
    private Color whisperColor = ColorParseUtil.parseColor("#A8A8A8");

    @Nonnull
    public Optional<ChatRank> rankOf(@Nonnull String username) {
        ChatRank best = null;
        for (var rank : ranks.values()) {
            if (rank.includes(username) && (best == null || rank.getPriority() > best.getPriority()))
                best = rank;
        }
        return Optional.ofNullable(best);
    }
}
