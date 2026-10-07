package com.gaiagauntlet.gauntlet.games.EliminationZone.spectator;

import com.gaiagauntlet.gauntlet.games.EliminationZone.spectator.interactions.TeamSpectateControlInteraction;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

public class EZSpectator {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void setup(JavaPlugin plugin) {
        LOGGER.atInfo().log("Setting up EZGame [Spectator]!");
        plugin.getCodecRegistry(Interaction.CODEC).register(TeamSpectateControlInteraction.ID,
            TeamSpectateControlInteraction.class,
            TeamSpectateControlInteraction.CODEC);
    }
}
