package gaiagauntlet.plugins.teams.commands;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import gaiagauntlet.plugins.announcer.utils.Chat;
import gaiagauntlet.plugins.settings.constants.Permissions;
import gaiagauntlet.plugins.teams.constants.TeamType;

import javax.annotation.Nonnull;

import static gaiagauntlet.plugins.teams.commands.TeamCommands.getRoster;

public final class CreateCommand extends CommandBase {
    private final RequiredArg<String> id = withRequiredArg("id", "Team id, no spaces", ArgTypes.STRING);
    private final OptionalArg<Integer> size = withOptionalArg("size", "Max players (default 3)", ArgTypes.INTEGER);
    private final OptionalArg<String> name = withOptionalArg("name", "Display name", ArgTypes.GREEDY_STRING);

    CreateCommand() {
        super("create", "Create a participant team");
        requirePermission(Permissions.ADMIN);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        try {
            var teamId = id.get(context);
            var maxSize = size.get(context) == null ? 3 : size.get(context);
            var display = name.get(context) == null ? teamId.replace('_', ' ') : name.get(context);
            getRoster().create(teamId, display, TeamType.Participant, maxSize);
            Chat.ok(context, "Created team " + teamId + " (" + display + ", max " + maxSize + ")");
        } catch (IllegalArgumentException e) {
            Chat.error(context, e.getMessage());
        }
    }
}
