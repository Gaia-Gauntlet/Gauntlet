package com.gaiagauntlet.gauntlet.plugins.chat.utils;

import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.msg;

import java.util.ArrayList;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.gaiagauntlet.gauntlet.plugins.chat.components.ChatChannel;
import com.gaiagauntlet.gauntlet.plugins.chat.config.ChatConfig;
import com.gaiagauntlet.gauntlet.plugins.chat.events.ChannelChatEvent;
import com.gaiagauntlet.gauntlet.plugins.chat.resource.UniverseChatResource;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.util.ColorParseUtil;
import com.hypixel.hytale.server.core.console.ConsoleModule;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.MessageUtil;

public final class ChatUtils {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private ChatUtils() {
    }

    public static void route(@Nonnull PlayerChatEvent event) {
        var sender = event.getSender();
        var channel = event instanceof ChannelChatEvent explicit
                ? explicit.getChannel()
                : UniverseChatResource.get().channelOf(sender.getUuid());

        var recipients = channel.recipients(sender);
        if (recipients == null) {
            event.setCancelled(true);
            sender.sendMessage(msg("server.gg.plugins.chat.unavailable." + channel.id()));
            return;
        }

        recipients.removeIf(recipient -> recipient.getHiddenPlayersManager().isPlayerHidden(sender.getUuid()));
        event.setTargets(recipients);
        event.setFormatter((player, content) -> format(player, channel, content));
    }

    public static void send(@Nonnull PlayerRef sender, @Nonnull ChatChannel channel, @Nonnull String content) {
        HytaleServer.get().getEventBus().dispatchForAsync(PlayerChatEvent.class)
                .dispatch(new ChannelChatEvent(sender, new ArrayList<>(), content, channel))
                .whenComplete((event, error) -> {
                    if (error != null) {
                        LOGGER.atSevere().withCause(error).log("Failed to send %s's %s message",
                                sender.getUsername(), channel.id());
                        return;
                    }
                    if (event.isCancelled())
                        return;

                    var message = event.getFormatter().format(sender, event.getContent());
                    HytaleLogger.getLogger().atInfo().log("%s",
                            MessageUtil.toAnsiString(message).toAnsi(ConsoleModule.get().getTerminal()));
                    for (var target : event.getTargets())
                        target.sendMessage(message);
                });
    }

    @Nonnull
    public static Message format(@Nonnull PlayerRef sender, @Nonnull ChatChannel channel, @Nonnull String content) {
        var config = ChatConfig.get();
        var color = ColorParseUtil.colorToHexString(channel.textColor(config));
        var rank = config.rankOf(sender.getUsername()).orElse(null);

        var parts = new ArrayList<Message>();
        if (!(channel instanceof ChatChannel.Global)) {
            parts.add(Message.translation("server.gg.plugins.chat.tag." + channel.id())
                    .color(ColorParseUtil.colorToHexString(channel.color(config))));
            parts.add(Message.raw(" "));
        }
        if (rank != null) {
            parts.add(Message.raw(rank.getPrefix()).color(ColorParseUtil.colorToHexString(rank.getColor())));
            parts.add(Message.raw(" "));
        }

        var line = channel instanceof ChatChannel.Whisper whisper
                ? Message.translation("server.gg.plugins.chat.line.whisper")
                        .param("target", name(Objects.requireNonNullElse(PlayerUtils.nameFor(whisper.target()),
                                whisper.target().toString()), color, config))
                : Message.translation("server.gg.plugins.chat.line");
        parts.add(line.color(color)
                .param("name", name(sender.getUsername(), color, config))
                .param("message", Message.raw(content).color(color)));

        return Message.join(parts.toArray(Message[]::new));
    }

    @Nonnull
    public static Message label(@Nonnull ChatChannel channel) {
        return Message.translation("server.gg.plugins.chat.channel." + channel.id());
    }

    @Nonnull
    private static Message name(@Nonnull String username, @Nonnull String fallbackColor, @Nonnull ChatConfig config) {
        var color = config.rankOf(username)
                .map(rank -> ColorParseUtil.colorToHexString(rank.getColor()))
                .orElse(fallbackColor);
        return Message.raw(username).color(color);
    }
}
