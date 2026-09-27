package gaiagauntlet.plugins.teams.commands;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import gaiagauntlet.plugins.announcer.utils.Chat;
import gaiagauntlet.plugins.settings.constants.Permissions;

import javax.annotation.Nonnull;

/** {@code /gg teams pull <game>}: every online member of the game's teams is moved into its lobbies. */
public final class PullCommand extends CommandBase {
    private final RequiredArg<String> game = withRequiredArg("game", "Game id", ArgTypes.STRING);

    PullCommand() {
        super("pull", "Send every online member of a game's teams into its lobbies");
        requirePermission(Permissions.ADMIN);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        Chat.error(context, "Command not implemented.");
        // TODO:
//        var target = GameArg.resolve(context, game.get(context));
//        if (target == null) return;
//        try {
//            var moved = GaiaGauntletPlugin.get().orchestrator().pullTeams(target);
//            Chat.ok(context, "Pulling " + moved + " players into " + target.id());
//        } catch (IllegalStateException e) {
//            Chat.error(context, e.getMessage());
//        }
    }
}
