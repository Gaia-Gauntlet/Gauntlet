package gaiagauntlet.plugins.teams.commands;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import gaiagauntlet.plugins.announcer.utils.Chat;
import gaiagauntlet.plugins.settings.constants.Permissions;

import javax.annotation.Nonnull;

import static gaiagauntlet.plugins.teams.commands.TeamCommands.getRoster;
import static gaiagauntlet.plugins.teams.commands.TeamCommands.refuseDuringMatch;

/** {@code /gg team reset}: every team goes back to the defaults bundled with the plugin. */
public final class ResetCommand extends CommandBase {
    ResetCommand() {
        super("reset", "Put every team back to the plugin's default teams");
        requirePermission(Permissions.ADMIN);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        if (refuseDuringMatch(context, "reset")) return;

        try {
            getRoster().resetToDefaults();
            Chat.ok(context, "Teams reset to the defaults: " + getRoster().teams().size() + " teams");
        } catch (IllegalStateException e) {
            Chat.error(context, e.getMessage());
        }
    }
}
