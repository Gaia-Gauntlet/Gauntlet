package gaiagauntlet.core.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import gaiagauntlet.plugins.settings.constants.Permissions;
import gaiagauntlet.plugins.teams.commands.TeamCommands;

public class GgCommand extends AbstractCommandCollection {
    public GgCommand() {
        super("gg", "Gaia Gauntlet controls");
        requirePermission(Permissions.ADMIN);
        addAliases("gaiagauntlet");

        addSubCommand(new TeamCommands());
    }
}
