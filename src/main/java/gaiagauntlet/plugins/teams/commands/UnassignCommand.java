package gaiagauntlet.plugins.teams.commands;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import gaiagauntlet.plugins.announcer.utils.Chat;
import gaiagauntlet.plugins.settings.constants.Permissions;

import javax.annotation.Nonnull;

import static gaiagauntlet.plugins.teams.commands.TeamCommands.getRoster;

public final class UnassignCommand extends CommandBase {
    private final RequiredArg<String> player = withRequiredArg("player", "Username", ArgTypes.STRING);

    UnassignCommand() {
        super("unassign", "Remove a player from their team");
        requirePermission(Permissions.ADMIN);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        if (getRoster().unassign(player.get(context))) {
            Chat.ok(context, player.get(context) + " removed from their team");
        } else {
            Chat.warn(context, player.get(context) + " was not on any team");
        }
    }
}
