package com.gaiagauntlet.gauntlet.plugins.chat.commands;

import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.msg;

import javax.annotation.Nonnull;

import com.gaiagauntlet.gauntlet.plugins.chat.components.ChatChannel;
import com.gaiagauntlet.gauntlet.plugins.chat.resource.UniverseChatResource;
import com.gaiagauntlet.gauntlet.plugins.chat.utils.ChatUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// centralized channels (except whispers because of the playe arg) that lets you swap between channels 
public class ChannelCommand extends AbstractPlayerCommand {
    private final ChatChannel channel;

    public ChannelCommand(@Nonnull ChatChannel channel) {
        super(channel.id(), "server.gg.plugins.chat.commands." + channel.id() + ".desc");
        this.channel = channel;
        requireNoPermission();
        addUsageVariant(new Once(channel));
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        if (channel.recipients(playerRef) == null) {
            playerRef.sendMessage(msg("server.gg.plugins.chat.unavailable." + channel.id()));
            return;
        }
        UniverseChatResource.get().select(playerRef.getUuid(), channel);
        playerRef.sendMessage(msg("server.gg.plugins.chat.selected").param("channel", ChatUtils.label(channel)));
    }

    private static class Once extends AbstractPlayerCommand {
        private final ChatChannel channel;
        // Annoyingly, this cannot handle spaces. I didn't find anything that would let you do <rest of args> too :/  - melodic pls fix
        private final RequiredArg<String> messageArg;

        Once(@Nonnull ChatChannel channel) {
            super("server.gg.plugins.chat.commands.message.desc");
            this.channel = channel;
            this.messageArg = withRequiredArg("message", "server.gg.plugins.chat.commands.message.arg",
                    ArgTypes.GREEDY_STRING);
            requireNoPermission();
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            ChatUtils.send(playerRef, channel, context.get(messageArg));
        }
    }
}
