package com.gaiagauntlet.gauntlet.plugins.proxychat;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.hypixel.hytale.logger.HytaleLogger;

public class ProxyChatPlugin extends GamePlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

     public static final String ID = "ProxyChatPlugin";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void install() {
        //
    }
}
