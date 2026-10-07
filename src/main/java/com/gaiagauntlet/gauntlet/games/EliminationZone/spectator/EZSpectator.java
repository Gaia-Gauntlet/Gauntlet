package com.gaiagauntlet.gauntlet.games.EliminationZone.spectator;

import com.gaiagauntlet.gauntlet.games.EliminationZone.spectator.components.SpectatorComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spectator.interactions.TeamSpectateControlInteraction;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spectator.systems.SpectatingSystems;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

public class EZSpectator {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void setup(JavaPlugin plugin) {
        LOGGER.atInfo().log("Setting up EZGame [SpectatorComponent]!");
        plugin.getCodecRegistry(Interaction.CODEC).register(TeamSpectateControlInteraction.ID,
            TeamSpectateControlInteraction.class,
            TeamSpectateControlInteraction.CODEC);

        var registry = plugin.getEntityStoreRegistry();
        SpectatorComponent.setType(registry.registerComponent(SpectatorComponent.class, SpectatorComponent::new));
    }

    public static void start(JavaPlugin plugin) {
        var registry = plugin.getEntityStoreRegistry();
        registry.registerSystem(new SpectatingSystems.OnSpectatorChange());
        registry.registerSystem(new SpectatingSystems.HideSpectators());
        registry.registerSystem(new SpectatingSystems.SpectatorControls());
        registry.registerSystem(new SpectatingSystems.FollowTarget());
    }
}
