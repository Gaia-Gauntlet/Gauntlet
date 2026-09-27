package gaiagauntlet.plugins.teams.commands;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import gaiagauntlet.plugins.announcer.utils.Chat;
import gaiagauntlet.plugins.settings.constants.Permissions;

import javax.annotation.Nonnull;

import static gaiagauntlet.plugins.teams.commands.TeamCommands.getRoster;

public final class ListCommand extends CommandBase {
    ListCommand() {
        super("list", "List every team and its members");
        requirePermission(Permissions.ADMIN);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        var roster = getRoster();
        Chat.header(context, "Teams (" + roster.participantCapacity() + " participant slots)");
        for (var team : roster.teams()) {
            var members = team.members().isEmpty() ? "(empty)" : String.join(", ", team.members());
            var tag = team.isParticipant() ? "" : " [" + team.type().name().toLowerCase() + "]";
            Chat.line(context, team.id() + tag + " " + team.size() + "/" + team.maxSize() + ":", members);
        }
    }
}
