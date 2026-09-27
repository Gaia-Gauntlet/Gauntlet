package gaiagauntlet.plugins.gamestore;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import gaiagauntlet.plugins.gamestore.config.GamesFile;
import gaiagauntlet.plugins.gamestore.config.HubsFile;
import gaiagauntlet.plugins.gamestore.constants.GameTypes;
import gaiagauntlet.plugins.gamestore.store.GlobalStore;

import java.util.logging.Level;

public class GameStorePlugin extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static GameStorePlugin instance;

    // Configs must be declared before setup runs, so they are created with the plugin.
    private final Config<HubsFile> hubsConfig = withConfig("hubs", HubsFile.CODEC);
    private final Config<GamesFile> gamesConfig = withConfig("games", GamesFile.CODEC);

    public GameStorePlugin(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void start() {
        LOGGER.at(Level.INFO).log("Starting Gauntlet [GAME STORE]!");
    }

    @Override
    protected void setup() {
        LOGGER.at(Level.INFO).log("Setting up Gauntlet [GAME STORE]!");

        GameTypes.register(GameTypes.DEFAULT);
        GlobalStore.register(hubsConfig, gamesConfig);
    }

    @Override
    protected void shutdown() {
        LOGGER.at(Level.INFO).log("Shutting down Gauntlet [GAME STORE]!");
    }
}
