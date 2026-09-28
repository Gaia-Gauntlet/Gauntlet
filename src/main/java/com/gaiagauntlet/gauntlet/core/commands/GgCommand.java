package com.gaiagauntlet.gauntlet.core.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

// import gauntlet.plugins.settings.constants.Permissions;
// import gauntlet.plugins.teams.commands.TeamCommands;

public class GgCommand extends AbstractCommandCollection {
    public GgCommand() {
        super("gg", "Gaia Gauntlet controls");
        // requirePermission(Permissions.ADMIN);
        addAliases("gaiagauntlet");

        // addSubCommand(new TeamCommands());
    }
}
