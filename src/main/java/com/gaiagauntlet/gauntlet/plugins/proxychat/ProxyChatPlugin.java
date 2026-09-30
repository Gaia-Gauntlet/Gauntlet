package com.gaiagauntlet.gauntlet.plugins.proxychat;

import java.util.List;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

public class ProxyChatPlugin implements GamePlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "ProxyChatPlugin";

    @Override
    public void init(JavaPlugin host) {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<String> getDependencies() {
        return List.of();
    }
}
