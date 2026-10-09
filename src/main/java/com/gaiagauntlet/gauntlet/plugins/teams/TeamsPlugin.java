package com.gaiagauntlet.gauntlet.plugins.teams;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.games.interfaces.UiGamePlugin;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.session.registry.SessionRegistry;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.HudElement;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfig;
import com.gaiagauntlet.gauntlet.plugins.announcer.AnnouncerPlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.interfaces.PersistentGamePlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestore.interfaces.SessionWriter;
import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameComponentRegistry;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamListComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.assets.TeamListAsset;
import com.gaiagauntlet.gauntlet.plugins.teams.components.entity.EliminatedComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.entity.TeamPlayerComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.editor.UsernameTransformButton;
import com.gaiagauntlet.gauntlet.plugins.teams.ui.PartyHud;
import com.gaiagauntlet.gauntlet.plugins.teams.ui.TeamsHud;
import com.gaiagauntlet.gauntlet.plugins.teams.ui.TeamsTab;
import com.gaiagauntlet.gauntlet.plugins.teams.utils.TeamUtils;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.builtin.asseteditor.AssetEditorPlugin;
import com.hypixel.hytale.builtin.asseteditor.event.AssetEditorActivateButtonEvent;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;

public class TeamsPlugin implements PersistentGamePlugin, UiGamePlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "TeamsPlugin";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void init(JavaPlugin host) {
        host.getAssetRegistry().register(HytaleAssetStore.builder(TeamListAsset.class, new DefaultAssetMap<>())
                .setPath("Gauntlet/Plugins/" + ID + "/Teams")
                .setCodec(TeamListAsset.CODEC)
                .setKeyFunction(TeamListAsset::getId)
                .build());

        var entityStore = host.getEntityStoreRegistry();

        EliminatedComponent.setComponentType(entityStore.registerComponent(
                EliminatedComponent.class,
                EliminatedComponent::new));
        TeamPlayerComponent.setComponentType(entityStore.registerComponent(
                TeamPlayerComponent.class,
                "TeamPlayer",
                TeamPlayerComponent.CODEC));

        TeamListComponent.setSessionComponentType(
                SessionRegistry.register(TeamListComponent.ID, TeamListComponent.class, TeamListComponent.CODEC));
        TeamListComponent.setGameComponentType(
                GameComponentRegistry.register(TeamListComponent.ID, TeamListComponent.class, TeamListComponent.CODEC));

        // asset editor button
        var editor = AssetEditorPlugin.get();
        if (editor != null) {
            editor.getEventRegistry().register(
                    AssetEditorActivateButtonEvent.class, UsernameTransformButton.BUTTON_ID,
                    UsernameTransformButton::activate);
        }

        AssetEditorPlugin assetEditor = AssetEditorPlugin.get();
        if (assetEditor == null) return;
    }

    @Override
    public List<String> getDependencies() {
        return List.of(AnnouncerPlugin.ID);
    }

    @Override
    public void setup(ComponentAccessor<EntityStore> arenaAccessor, GameSession sessionObject, GameEcs gameStore, String gameId) {
        // sets up the game with the team stuff
        var teamList = new TeamListComponent();
        gameStore.put(TeamListComponent.getGameComponentType(), teamList);
        TeamUtils.initialiseTeams(teamList, EZGameConfig.get(gameStore).getTeamsPreset());
        TeamUtils.distributePlayers(GauntletUtils.playersFor(sessionObject), teamList);
    }

    @Override
    public SessionWriter capture(World world, GameEcs store, String sessionId) {
        return null;
    }

    @Override
    public List<AdminTab> getAdminTabs() {
        return List.of(new TeamsTab());
    }

    @Override
    public List<HudElement> getHudElements() {
        return List.of(new PartyHud(), new TeamsHud());
    }
}
