package com.gaiagauntlet.gauntlet.core.commands;

import com.gaiagauntlet.gauntlet.core.games.interfaces.CommandGamePlugin;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

// import gauntlet.plugins.settings.constants.Permissions;
// import gauntlet.plugins.teams.commands.TeamCommands;

public class GauntletCommand extends AbstractCommandCollection {
    public GauntletCommand() {
        super("gauntlet", "Gaia Gauntlet controls");
        // requirePermission(Permissions.ADMIN);
        addAliases("gg");

        for (CommandGamePlugin plugin : GameRegistry.getPlugins(CommandGamePlugin.class)) {
            for (AbstractCommand command : plugin.getCommands()) {
                addSubCommand(command);
            }
        }
    }
}
