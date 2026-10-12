package com.gaiagauntlet.gauntlet.plugins.chat;

import java.util.List;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.gaiagauntlet.gauntlet.plugins.chat.commands.ChatCommand;
import com.gaiagauntlet.gauntlet.plugins.chat.config.ChatConfig;
import com.gaiagauntlet.gauntlet.plugins.chat.resource.UniverseChatResource;
import com.gaiagauntlet.gauntlet.plugins.chat.utils.ChatUtils;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.Universe;
import com.riprod.configly.Configly;

public class ChatPlugin implements GamePlugin {
    public static final String ID = "ChatPlugin";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Chat";
    }

    @Override
    public void init(JavaPlugin host) {
        Configly.register(ChatConfig.TYPE, ChatConfig.class, ChatConfig.CODEC);
        UniverseChatResource.setResourceType(
                Universe.registerResource(UniverseChatResource.class, UniverseChatResource.ID,
                        UniverseChatResource.CODEC));

        host.getEventRegistry().registerGlobal(EventPriority.EARLY, PlayerChatEvent.class, ChatUtils::route);
        host.getEventRegistry().register(PlayerDisconnectEvent.class, event -> {
            var player = event.getPlayerRef();
            if (player != null)
                UniverseChatResource.get().clear(player.getUuid());
        });
        host.getCommandRegistry().registerCommand(new ChatCommand());
    }

    @Override
    public List<String> getDependencies() {
        return List.of();
    }
}
