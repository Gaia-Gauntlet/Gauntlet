package com.gaiagauntlet.gauntlet.core.commands;

import java.util.Collection;
import java.util.Locale;

import javax.annotation.Nonnull;

import com.gaiagauntlet.gauntlet.core.GauntletOrchestrator;
import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.SingleArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand;
import com.hypixel.hytale.server.core.command.system.suggestion.SuggestionProvider;
import com.hypixel.hytale.server.core.command.system.suggestion.SuggestionResult;
import com.hypixel.hytale.server.core.command.system.suggestion.SuggestionUtil;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * All of these are, currently, debug and a stop-gap until the full eventing
 * pipeline is finished
 * 
 * 
 * Once the eventing pipeline is done, the orchestrator will be managing all of
 * this stuff and these commands will just
 * emit intents
 */
public class SessionCommands extends AbstractCommandCollection {

    private static final SingleArgumentType<String> SESSION_ID = new SingleArgumentType<>(
            "server.commands.parsing.argtype.string.name", "server.commands.parsing.argtype.string.usage") {
        @Override
        public String parse(String input, ParseResult parseResult) {
            return input;
        }

        @Override
        public void suggest(@Nonnull CommandSender sender, @Nonnull String textAlreadyEntered,
                int numParametersTyped, @Nonnull SuggestionResult result) {
            SuggestionUtil.suggestFiltered(GauntletUtils.withResource().getSessions().keySet(), textAlreadyEntered, result);

        }

        @Override
        public int getSuggestionValueCount() {
            return 1;
        }
    };
    private static final SingleArgumentType<String> GAME_ID = new SingleArgumentType<>(
            "server.commands.parsing.argtype.string.name", "server.commands.parsing.argtype.string.usage") {
        @Override
        public String parse(String input, ParseResult parseResult) {
            return input;
        }

        @Override
        public void suggest(@Nonnull CommandSender sender, @Nonnull String textAlreadyEntered,
                int numParametersTyped, @Nonnull SuggestionResult result) {
            SuggestionUtil.suggestFiltered(GameRegistry.getGameIds(), textAlreadyEntered, result);
        }

        @Override
        public int getSuggestionValueCount() {
            return 1;
        }
    };

    public SessionCommands() {
        super("session", "Gaia Gauntlet controls");
        // requirePermission(Permissions.ADMIN);
        addAliases("s");
        addSubCommand(new CreateSession());
        addSubCommand(new ListSessions());
        addSubCommand(new DestroySession());
        addSubCommand(new AddGameToSession());
        addSubCommand(new RemoveGameFromSession());
        addSubCommand(new SetupSession());
        addSubCommand(new StartSession());
        addSubCommand(new StopSession());
        addSubCommand(new NextGameSession());
    }

    @Nonnull
    private static Message msg(String key) {
        var message = Message.translation(key);
        message.getFormattedMessage().markupEnabled = true;
        return message;
    }

    @Nonnull
    private static Message markup(@Nonnull Message message) {
        message.getFormattedMessage().markupEnabled = true;
        return message;
    }

    private static Message error(@Nonnull String error) {
        var message = Message.translation("server.gg.commands.error").param("message", error);
        message.getFormattedMessage().markupEnabled = true;
        return message;
    }

    private class CreateSession extends AbstractWorldCommand {
        private RequiredArg<String> sessionId;
        private OptionalArg<String> games;

        public CreateSession() {
            super("create", "Create a new session");
            addAliases("c");
            sessionId = withRequiredArg("sessionId", "The id of the session", ArgTypes.STRING);
            games = withOptionalArg("games", "the list of games to play as a csv", ArgTypes.STRING);
        }

        @Override
        protected void execute(CommandContext ctx, World arg1, Store<EntityStore> arg2) {
            var selectedGamesList = games.provided(ctx) ? games.get(ctx) : "";
            var session = sessionId.get(ctx);
            var games = selectedGamesList.split(",");

            var gameSession = new GameSession(session);
            if (games.length >= 1) {
                for (var game : games) {
                    if (!GameRegistry.hasGame(game)) {
                        ctx.sendMessage(error("Game " + game + " is not registered!"));
                        continue;
                    }
                    gameSession.addGame(game);
                }
            }

            var resource = GauntletUtils.withResource();
            var success = resource.addSession(gameSession);
            if (success) {
                ctx.sendMessage(msg("session.create.success").param("sessionId", session));
            } else {
                ctx.sendMessage(error("Unable to add session! It already exists"));
            }
        }
    }

    private class DestroySession extends AbstractWorldCommand {
        private RequiredArg<String> sessionId;

        public DestroySession() {
            super("destroy", "Destroys a session");
            addAliases("d");
            sessionId = withRequiredArg("sessionId", "The session to destroy", ArgTypes.STRING);
        }

        @Override
        protected void execute(CommandContext ctx, World arg1, Store<EntityStore> arg2) {
            var targetSession = sessionId.get(ctx);
            var resource = GauntletUtils.withResource();
            resource.getSession(targetSession).ifPresentOrElse(session -> {

                resource.getSessions().remove(session.getId());

                ctx.sendMessage(msg("server.gg.commands.session.destroy.success")
                        .param("sessionId", session.getId()));
            }, () -> {
                ctx.sendMessage(error("Unable to remove session: " + sessionId + " because it isn't registered!"));
            });

        }
    }

    private class ListSessions extends AbstractWorldCommand {
        public ListSessions() {
            super("list", "List all sessions");
            addAliases("ls");
        }

        @Override
        protected void execute(CommandContext ctx, World arg1, Store<EntityStore> arg2) {
            var sessions = GauntletUtils.withResource().getSessions();

            if (sessions.size() == 0) {
                ctx.sendMessage(error("No active sessions"));
                return;
            }

            for (var session : sessions.entrySet()) {
                var games = session.getValue().getGameSequence();
                ctx.sendMessage(
                        markup(Message.translation("server.gg.commands.session.list.line")
                                .param("sessionId", session.getKey())
                                .param("game", session.getValue().getCurrentGame())
                                .param("gamesList",
                                        games != null && games.length >= 1
                                                ? String.join(", ", session.getValue().getGameSequence())
                                                : "No games queued")
                                .param("state", session.getValue().getSessionState().toString())));
            }
        }
    }

    private class AddGameToSession extends AbstractWorldCommand {
        private RequiredArg<String> sessionId;
        private RequiredArg<String> gameId;

        public AddGameToSession() {
            super("add", "Add a game to a session");
            addAliases("a");
            sessionId = withRequiredArg("sessionId", "The id of the session", SESSION_ID);
            gameId = withRequiredArg("game", "The game to add", GAME_ID);
        }

        @Override
        protected void execute(CommandContext ctx, World arg1, Store<EntityStore> arg2) {
            var game = gameId.get(ctx);
            var session = sessionId.get(ctx);
            GauntletUtils.sessionFor(session).ifPresentOrElse(ses -> {
                ses.addGame(game);
                ctx.sendMessage(msg("server.gg.commands.session.add.success")
                        .param("sessionId", session)
                        .param("gameId", game));
            }, () -> {
                ctx.sendMessage(error("Unable to find session " + session));
            });

        }
    }

    private class RemoveGameFromSession extends AbstractWorldCommand {
        private RequiredArg<String> sessionId;
        private RequiredArg<String> gameId;

        public RemoveGameFromSession() {
            super("remove", "Remove a game from the session");
            addAliases("r");
            sessionId = withRequiredArg("sessionId", "The id of the session", SESSION_ID);
            gameId = withRequiredArg("game", "the game to remove", GAME_ID);
        }

        @Override
        protected void execute(CommandContext ctx, World arg1, Store<EntityStore> arg2) {
            var game = gameId.get(ctx);
            var session = sessionId.get(ctx);
            GauntletUtils.sessionFor(session).ifPresentOrElse(ses -> {
                var success = ses.removeGameIfPresent(game);
                ctx.sendMessage(msg("server.gg.commands.session.remove.success")
                        .param("sessionId", session)
                        .param("gameId", game)
                        .param("status", success ? "successfully" : "unsuccessfully"));
            }, () -> {
                ctx.sendMessage(error("Unable to remove game from session: " + session));
            });

        }
    }

    private class SetupSession extends AbstractWorldCommand {
        private RequiredArg<String> sessionId;

        public SetupSession() {
            super("setup", "Sets up the next game for a session");
            sessionId = withRequiredArg("sessionId", "The session", SESSION_ID);
        }

        @Override
        protected void execute(CommandContext ctx, World arg1, Store<EntityStore> accessor) {
            var targetSession = sessionId.get(ctx);
            var future = GauntletOrchestrator.setupGame(accessor, targetSession);
            ctx.sendMessage(msg("server.gg.commands.session.setup.pending")
                    .param("sessionId", targetSession));

            future.whenComplete((ctrl, error) -> {
                if (error != null) {
                    ctx.sendMessage(error("Unable to setup! " + error.getLocalizedMessage()));
                    return;
                }
                ctx.sendMessage(msg("server.gg.commands.session.setup.success")
                        .param("sessionId", targetSession)
                        .param("gameId", ctrl.getId()));
            });
        }
    }

    private class StartSession extends AbstractWorldCommand {
        private RequiredArg<String> sessionId;

        public StartSession() {
            super("start", "Starts the current game for a session");
            sessionId = withRequiredArg("sessionId", "The session", SESSION_ID);
        }

        @Override
        protected void execute(CommandContext ctx, World arg1, Store<EntityStore> accessor) {
            var targetSession = sessionId.get(ctx);
            ctx.sendMessage(error("Starting games not yet supported!"));

            // var future = GauntletOrchestrator.setupGame(accessor, targetSession);
            // ctx.sendMessage(msg("server.gg.commands.session.start.pending")
            // .param("sessionId", targetSession));

            // future.whenComplete((ctrl, error) -> {
            // if (error != null) {
            // ctx.sendMessage(error("Unable to start! " + error.getLocalizedMessage()));
            // return;
            // }

            // ctx.sendMessage(msg("server.gg.commands.session.start.success")
            // .param("sessionId", targetSession)
            // .param("gameId", ctrl.getId()));
            // });
        }
    }

    private class StopSession extends AbstractWorldCommand {
        private RequiredArg<String> sessionId;

        public StopSession() {
            super("stop", "Starts the next game for a session");
            sessionId = withRequiredArg("sessionId", "The session", SESSION_ID);
        }

        @Override
        protected void execute(CommandContext ctx, World arg1, Store<EntityStore> accessor) {
            var targetSession = sessionId.get(ctx);
            ctx.sendMessage(error("Stopping games not yet supported!"));

            // var future = GauntletOrchestrator.stopGame(accessor, targetSession);
            // ctx.sendMessage(msg("server.gg.commands.session.stop.pending")
            // .param("sessionId", targetSession));

            // future.whenComplete((ctrl, error) -> {
            // if (error != null) {
            // ctx.sendMessage(error("Unable to stop! " + error.getLocalizedMessage()));
            // return;
            // }
            // ctx.sendMessage(msg("server.gg.commands.session.stop.success")
            // .param("sessionId", targetSession)
            // .param("gameId", ctrl.getId()));
            // });
        }
    }

    private class NextGameSession extends AbstractWorldCommand {
        private RequiredArg<String> sessionId;

        public NextGameSession() {
            super("next", "Sets up the next game for a session");
            sessionId = withRequiredArg("sessionId", "The session", SESSION_ID);
        }

        @Override
        protected void execute(CommandContext ctx, World arg1, Store<EntityStore> accessor) {
            var targetSession = sessionId.get(ctx);
            ctx.sendMessage(error("Transitioning to the next game not yet supported!"));

            // var future = GauntletOrchestrator.setupGame(accessor, targetSession);
            // ctx.sendMessage(msg("server.gg.commands.session.setup.pending")
            // .param("sessionId", targetSession));

            // future.whenComplete((ctrl, error) -> {
            // if (error != null) {
            // ctx.sendMessage(error("Unable to setup! " + error.getLocalizedMessage()));
            // return;
            // }
            // ctx.sendMessage(msg("server.gg.commands.session.setup.success")
            // .param("sessionId", targetSession)
            // .param("gameId", ctrl.getId()));
            // });
        }
    }
}
