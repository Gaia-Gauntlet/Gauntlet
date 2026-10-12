package com.gaiagauntlet.gauntlet.plugins.chat.commands;

import com.gaiagauntlet.gauntlet.plugins.chat.components.ChatChannel;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class ChatCommand extends AbstractCommandCollection {
    public ChatCommand() {
        super("chat", "server.gg.plugins.chat.commands.chat.desc");
        requireNoPermission();

        addSubCommand(new ChannelCommand(ChatChannel.GLOBAL));
        addSubCommand(new ChannelCommand(ChatChannel.PARTY));
        addSubCommand(new ChannelCommand(ChatChannel.TEAM));
        addSubCommand(new ChannelCommand(ChatChannel.SESSION));
        addSubCommand(new ChannelCommand(ChatChannel.ANNOUNCEMENT));
        addSubCommand(new ChannelCommand(ChatChannel.SPECTATOR));
        addSubCommand(new WhisperCommand());
    }
}
