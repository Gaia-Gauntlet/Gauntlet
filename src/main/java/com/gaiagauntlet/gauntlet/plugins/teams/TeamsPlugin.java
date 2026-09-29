package com.gaiagauntlet.gauntlet.plugins.teams;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.gaiagauntlet.gauntlet.plugins.announcer.AnnouncerPlugin;
import com.gaiagauntlet.gauntlet.plugins.teams.components.EliminatedComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.assets.TeamAsset;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import lombok.Getter;

import java.util.List;

public class TeamsPlugin extends GamePlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    @Getter public static final String ID = "TeamsPlugin";

    @Override
    public void install() {
        //
    }

    public void setup(JavaPlugin host) {
        host.getAssetRegistry().register(HytaleAssetStore.builder(TeamAsset.class, new DefaultAssetMap<>())
            .setPath("Gauntlet/"+ID)
            .setCodec(TeamAsset.CODEC)
            .setKeyFunction(TeamAsset::getId)
            .build());

        EliminatedComponent.setComponentType(host.getEntityStoreRegistry().registerComponent(
            EliminatedComponent.class,
            EliminatedComponent::new
        ));
    }

    @Override
    public List<String> getDependencies() {
        var deps = super.getDependencies();
        deps.add(AnnouncerPlugin.getID());
        return deps;
    }
}
