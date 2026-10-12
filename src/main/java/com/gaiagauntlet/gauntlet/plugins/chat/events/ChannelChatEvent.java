package com.gaiagauntlet.gauntlet.plugins.chat.events;

import java.util.List;

import javax.annotation.Nonnull;

import com.gaiagauntlet.gauntlet.plugins.chat.components.ChatChannel;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import lombok.Getter;

public class ChannelChatEvent extends PlayerChatEvent {
    @Getter
    private final ChatChannel channel;

    public ChannelChatEvent(@Nonnull PlayerRef sender, @Nonnull List<PlayerRef> targets, @Nonnull String content,
            @Nonnull ChatChannel channel) {
        super(sender, targets, content);
        this.channel = channel;
    }
}
