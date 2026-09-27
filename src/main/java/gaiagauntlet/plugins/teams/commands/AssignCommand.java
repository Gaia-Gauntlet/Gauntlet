package gaiagauntlet.plugins.teams.commands;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import gaiagauntlet.plugins.announcer.utils.Chat;
import gaiagauntlet.plugins.settings.constants.Permissions;

import javax.annotation.Nonnull;

import static gaiagauntlet.plugins.teams.commands.TeamCommands.getRoster;

public final class AssignCommand extends CommandBase {
    private final RequiredArg<String> player = withRequiredArg("player", "Username", ArgTypes.STRING);
    private final RequiredArg<String> team = withRequiredArg("team", "Team id", ArgTypes.STRING);

    AssignCommand() {
        super("assign", "Put a player on a team, moving them off any other team");
        requirePermission(Permissions.ADMIN);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        try {
            getRoster().assign(player.get(context), team.get(context));
            Chat.ok(context, player.get(context) + " is now on " + team.get(context));
        } catch (IllegalArgumentException e) {
            Chat.error(context, e.getMessage());
        }
    }
}
