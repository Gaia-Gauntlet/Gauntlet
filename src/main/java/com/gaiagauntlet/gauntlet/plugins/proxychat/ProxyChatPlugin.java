package com.gaiagauntlet.gauntlet.plugins.proxychat;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import java.util.logging.Level;

public class ProxyChatPlugin extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public ProxyChatPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Starting Gauntlet [PROXY CHAT]!");
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up Gauntlet [PROXY CHAT]!");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down Gauntlet [PROXY CHAT]!");
    }
}
