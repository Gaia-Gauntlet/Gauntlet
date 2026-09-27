package gaiagauntlet.plugins.teams.commands;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import gaiagauntlet.plugins.announcer.utils.Chat;
import gaiagauntlet.plugins.settings.constants.Permissions;

import javax.annotation.Nonnull;

import static gaiagauntlet.plugins.teams.commands.TeamCommands.getRoster;

public final class RenameCommand extends CommandBase {
    private final RequiredArg<String> id = withRequiredArg("id", "Team id", ArgTypes.STRING);
    private final RequiredArg<String> name = withRequiredArg("name", "New display name", ArgTypes.GREEDY_STRING);

    RenameCommand() {
        super("rename", "Change a team's display name");
        requirePermission(Permissions.ADMIN);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        try {
            getRoster().update(id.get(context), team -> team.setName(name.get(context)));
            Chat.ok(context, id.get(context) + " is now called " + name.get(context));
        } catch (IllegalArgumentException e) {
            Chat.error(context, e.getMessage());
        }
    }
}
