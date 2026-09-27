package gaiagauntlet.plugins.scoring;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import java.util.logging.Level;

public class ScoringPlugin extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static ScoringPlugin instance;

    public ScoringPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void start() {
        LOGGER.at(Level.INFO).log("Starting Gauntlet [SCORING]!");
    }

    @Override
    protected void setup() {
        LOGGER.at(Level.INFO).log("Setting up Gauntlet [SCORING]!");
    }

    @Override
    protected void shutdown() {
        LOGGER.at(Level.INFO).log("Shutting down Gauntlet [SCORING]!");
    }
}
