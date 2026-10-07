package com.gaiagauntlet.gauntlet.plugins.spectator;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.gaiagauntlet.gauntlet.plugins.spectator.components.SpectatorComponent;
import com.gaiagauntlet.gauntlet.plugins.spectator.interactions.TeamSpectateControlInteraction;
import com.gaiagauntlet.gauntlet.plugins.spectator.systems.SpectatingSystems;
import com.gaiagauntlet.gauntlet.plugins.teams.TeamsPlugin;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import java.util.List;

public class SpectatorPlugin implements GamePlugin {
    @Override
    public String getId() {
        return "SpectatorPlugin";
    }

    @Override
    public List<String> getDependencies() {
        return List.of(
            TeamsPlugin.ID
        );
    }

    @Override
    public void init(JavaPlugin plugin) {
        plugin.getCodecRegistry(Interaction.CODEC).register(TeamSpectateControlInteraction.ID,
            TeamSpectateControlInteraction.class,
            TeamSpectateControlInteraction.CODEC);

        var registry = plugin.getEntityStoreRegistry();
        SpectatorComponent.setType(registry.registerComponent(SpectatorComponent.class, SpectatorComponent::new));

        registry.registerSystem(new SpectatingSystems.OnSpectatorChange());
        registry.registerSystem(new SpectatingSystems.HideSpectators());
        registry.registerSystem(new SpectatingSystems.SpectatorControls());
        registry.registerSystem(new SpectatingSystems.FollowTarget());
    }
}
