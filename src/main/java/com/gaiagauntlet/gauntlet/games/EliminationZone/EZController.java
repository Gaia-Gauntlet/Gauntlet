package com.gaiagauntlet.gauntlet.games.EliminationZone;

import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.HudElement;
import com.gaiagauntlet.gauntlet.games.EliminationZone.ui.BossesTab;
import com.gaiagauntlet.gauntlet.games.EliminationZone.ui.EventsTab;
import com.gaiagauntlet.gauntlet.games.EliminationZone.ui.ScoreboardHud;
import com.gaiagauntlet.gauntlet.games.EliminationZone.ui.TimerHud;
import com.gaiagauntlet.gauntlet.games.EliminationZone.ui.WinnerHud;
import com.gaiagauntlet.gauntlet.games.EliminationZone.ui.ZonesHud;
import com.gaiagauntlet.gauntlet.games.EliminationZone.ui.ZonesTab;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.arena.EZArenaManager;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.lobby.EZLobbyManager;
import com.gaiagauntlet.gauntlet.plugins.announcer.AnnouncerPlugin;
import com.gaiagauntlet.gauntlet.plugins.config.ConfigPlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestate.GameStatePlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestore.GameStorePlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.LobbyController;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.LobbyControllerPlugin;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.interfaces.ArenaManager;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.interfaces.LobbyManager;
import com.gaiagauntlet.gauntlet.plugins.proxychat.ProxyChatPlugin;
import com.gaiagauntlet.gauntlet.plugins.spectator.SpectatorPlugin;
import com.gaiagauntlet.gauntlet.plugins.teams.TeamsPlugin;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EZController extends LobbyController {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "EZGameController";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Elimination Zone";
    }

    private static final List<String> requiredPlugins = List.of(
        AnnouncerPlugin.ID,
        GameStatePlugin.ID,
        GameStorePlugin.ID,
        LobbyControllerPlugin.ID,
        ProxyChatPlugin.ID,
        TeamsPlugin.ID,
        ConfigPlugin.ID,
        SpectatorPlugin.ID
    );

    private final EZArenaManager arena = new EZArenaManager();
    private final EZLobbyManager lobby = new EZLobbyManager();

    @NotNull
    public static EZController get() {
        var gameController = GameRegistry.getGame(ID).orElse(null);
        if (gameController != null) {
            if (gameController instanceof EZController ezController) {
                return ezController;
            }
        }
        throw new IllegalStateException("Unable to get " + ID + "! The game has not been registered");
    }

    @Override
    public ArenaManager getArenaManager() {
        return arena;
    }

    @Override
    public LobbyManager getLobbyManager() {
        return lobby;
    }

    @Override
    public List<AdminTab> getAdminTabs() {
        return List.of(new ZonesTab(), new BossesTab(), new EventsTab());
    }

    @Override
    public List<HudElement> getHudElements() {
        return List.of(new ZonesHud(), new TimerHud(), new ScoreboardHud(), new WinnerHud());
    }

    @Override // temp, just messing around here
    public List<String> getRequiredPlugins() {
        return requiredPlugins;
    }

    @Override
    public void setupGame(World world, GameEcs gameStore, String sessionId) {}
}
