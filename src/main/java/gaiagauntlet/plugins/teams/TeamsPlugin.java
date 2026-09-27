package gaiagauntlet.plugins.teams;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import gaiagauntlet.GauntletPlugin;
import gaiagauntlet.plugins.teams.commands.TeamCommands;
import gaiagauntlet.plugins.teams.playerids.PlayerIds;
import gaiagauntlet.plugins.teams.playerids.PlayerIdsFile;
import gaiagauntlet.plugins.teams.team.TeamsFile;
import lombok.Getter;

import java.util.logging.Level;

public class TeamsPlugin extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static TeamsPlugin instance;

    // Configs must be declared before setup runs, so they are created with the
    // plugin.
    private final Config<TeamsFile> teamsConfig = withConfig("teams", TeamsFile.CODEC);
    private final Config<PlayerIdsFile> playerIdsConfig = withConfig("players", PlayerIdsFile.CODEC);

    // TODO: Should this be stored as a universe level resource, instead of storing state here?
    @Getter private Roster roster;

    public TeamsPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static TeamsPlugin get() {
        return instance;
    }

    @Override
    protected void start() {
        LOGGER.at(Level.INFO).log("Starting Gauntlet [TEAMS]!");
    }

    @Override
    protected void setup() {
        LOGGER.at(Level.INFO).log("Setting up Gauntlet [TEAMS]!");

        // TODO: Original code loads settings first. Not sure how to set dependencies for subplugins
        //  so there may be issues caused by load order here...
        var dataDirectory = getDataDirectory();
        roster = new Roster(dataDirectory, teamsConfig);
        roster.load();
        new PlayerIds(playerIdsConfig).load();
    }

    @Override
    protected void shutdown() {
        LOGGER.at(Level.INFO).log("Shutting down Gauntlet [TEAMS]!");
    }
}
