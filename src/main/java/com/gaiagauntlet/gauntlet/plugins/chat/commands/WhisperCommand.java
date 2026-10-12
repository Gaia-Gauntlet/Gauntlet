package com.gaiagauntlet.gauntlet.plugins.chat.commands;

import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.msg;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

public class WhisperCommand extends AbstractPlayerCommand {
    private final RequiredArg<PlayerRef> playerArg;

    public WhisperCommand() {
        super("whisper", "server.gg.plugins.chat.commands.whisper.desc");
        playerArg = withRequiredArg("player", "server.gg.plugins.chat.commands.whisper.arg", ArgTypes.PLAYER_REF);
        requireNoPermission();
        addUsageVariant(new Once());
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        var channel = channelTo(playerRef, context.get(playerArg));
        if (channel == null)
            return;
        UniverseChatResource.get().select(playerRef.getUuid(), channel);
        playerRef.sendMessage(msg("server.gg.plugins.chat.selected.whisper")
                .param("player", context.get(playerArg).getUsername()));
    }

    @Nullable
    private static ChatChannel channelTo(@Nonnull PlayerRef sender, @Nonnull PlayerRef target) {
        if (target.getUuid().equals(sender.getUuid())) {
            sender.sendMessage(msg("server.gg.plugins.chat.whisper.self"));
            return null;
        }
        return new ChatChannel.Whisper(target.getUuid());
    }

    private static class Once extends AbstractPlayerCommand {
        private final RequiredArg<PlayerRef> playerArg;
        private final RequiredArg<String> messageArg;

        Once() {
            super("server.gg.plugins.chat.commands.whisper.message.desc");
            playerArg = withRequiredArg("player", "server.gg.plugins.chat.commands.whisper.arg", ArgTypes.PLAYER_REF);
            messageArg = withRequiredArg("message", "server.gg.plugins.chat.commands.message.arg",
                    ArgTypes.GREEDY_STRING);
            requireNoPermission();
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            var channel = channelTo(playerRef, context.get(playerArg));
            if (channel != null)
                ChatUtils.send(playerRef, channel, context.get(messageArg));
        }
    }
}
