package gaiagauntlet;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import gaiagauntlet.core.commands.GgCommand;
import lombok.Getter;

import java.util.logging.Level;

public class GauntletPlugin extends JavaPlugin {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static GauntletPlugin instance;

    /** Store reference to command to allow sub-plugins to add subcommands */
    @Getter private GgCommand ggCommand;

    public GauntletPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static GauntletPlugin get() {
        return instance;
    }

    @Override
    protected void start() {
        LOGGER.at(Level.INFO).log("Starting Gauntlet!");
    }

    @Override
    protected void setup() {
        LOGGER.at(Level.INFO).log("Setting up Gauntlet!");

        ggCommand = new GgCommand();
        getCommandRegistry().registerCommand(ggCommand);
    }

    @Override
    protected void shutdown() {
        LOGGER.at(Level.INFO).log("Shutting down Gauntlet!");
    }
}
