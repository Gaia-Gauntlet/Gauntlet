package gaiagauntlet.plugins.teams.commands;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import gaiagauntlet.plugins.settings.constants.Permissions;
import gaiagauntlet.plugins.teams.Roster;
import gaiagauntlet.plugins.teams.TeamsPlugin;

import javax.annotation.Nonnull;

/** {@code /gg team list|create|delete|assign|unassign|rename|pull|shuffle|reset}. */
public final class TeamCommands extends AbstractCommandCollection {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public TeamCommands() {
        super("team", "Manage teams and rosters");
        requirePermission(Permissions.ADMIN);
        addAliases("teams");
        addSubCommand(new ListCommand());
        addSubCommand(new CreateCommand());
        addSubCommand(new DeleteCommand());
        addSubCommand(new AssignCommand());
        addSubCommand(new UnassignCommand());
        addSubCommand(new RenameCommand());
        addSubCommand(new PullCommand());
        addSubCommand(new ShuffleCommand());
        addSubCommand(new ResetCommand());
    }

    static Roster getRoster() {
        return TeamsPlugin.get().getRoster();
    }

    /** Tells the sender and returns true when any game has a match running, since teams must not change then. */
    static boolean refuseDuringMatch(@Nonnull CommandContext context, @Nonnull String action) {
        LOGGER.atWarning().log("refuseDuringMatch not implemented, returning false by default.");
        // TODO:
//        for (var game : GlobalStore.get().games()) {
//            if (MatchStateComponent.TYPE.of(game).state() != MatchState.IDLE) {
//                Chat.error(context, "Teams cannot be " + action + " while " + game.id() + " has a match running");
//                return true;
//            }
//        }
        return false;
    }
}
