package gaiagauntlet.plugins.settings;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import gaiagauntlet.plugins.settings.config.SettingsRegistry;
import gaiagauntlet.plugins.settings.config.SettingsValues;
import gaiagauntlet.plugins.settings.constants.Settings;

import java.util.logging.Level;

public class SettingsPlugin extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static SettingsPlugin instance;

    private final SettingsRegistry settings = Settings.create();
    // Configs must be declared before setup runs, so they are created with the
    // plugin.
    private final Config<SettingsValues> settingsConfig = withConfig("settings", settings.codec());

    public SettingsPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void start() {
        LOGGER.at(Level.INFO).log("Starting Gauntlet [SETTINGS]!");
    }

    @Override
    protected void setup() {
        LOGGER.at(Level.INFO).log("Setting up Gauntlet [SETTINGS]!");

        settings.load(settingsConfig);
    }

    @Override
    protected void shutdown() {
        LOGGER.at(Level.INFO).log("Shutting down Gauntlet [SETTINGS]!");
    }
}
