package gaiagauntlet.plugins.teams.commands;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import gaiagauntlet.plugins.announcer.utils.Chat;
import gaiagauntlet.plugins.settings.constants.Permissions;

import javax.annotation.Nonnull;

import static gaiagauntlet.plugins.teams.commands.TeamCommands.getRoster;

public final class DeleteCommand extends CommandBase {
    private final RequiredArg<String> id = withRequiredArg("id", "Team id", ArgTypes.STRING);

    DeleteCommand() {
        super("delete", "Delete a team and unassign its members");
        requirePermission(Permissions.ADMIN);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        try {
            getRoster().delete(id.get(context));
            Chat.ok(context, "Deleted team " + id.get(context));
        } catch (IllegalArgumentException e) {
            Chat.error(context, e.getMessage());
        }
    }
}
