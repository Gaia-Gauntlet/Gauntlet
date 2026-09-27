package gaiagauntlet.plugins.teams.commands;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.Universe;
import gaiagauntlet.plugins.announcer.utils.Chat;
import gaiagauntlet.plugins.settings.constants.Permissions;
import gaiagauntlet.plugins.teams.team.Team;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;

import static gaiagauntlet.plugins.teams.commands.TeamCommands.getRoster;
import static gaiagauntlet.plugins.teams.commands.TeamCommands.refuseDuringMatch;

/**
 * {@code /gg team shuffle}: every online player who is not on a watching team such as Camera Crew
 * is dealt at random onto participant teams picked at random, three to a team, until the players
 * run out.
 * The teams' previous rosters are replaced.
 */
public final class ShuffleCommand extends CommandBase {
    private static final int PER_TEAM = 3;

    ShuffleCommand() {
        super("shuffle", "Deal the online players onto random teams, three to a team");
        requirePermission(Permissions.ADMIN);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        Chat.error(context, "Command not implemented.");
        // TODO:
//        if (refuseDuringMatch(context, "shuffled")) return;
//
//        var store = GlobalStore.get();
//        var roster = getRoster();
//        var players = new ArrayList<String>();
//        for (var player : Universe.get().getPlayers()) {
//            var username = String.valueOf(player.getUsername());
//            if (roster.teamOf(username).map(Team::isParticipant).orElse(true)) {
//                players.add(username);
//            }
//        }
//        if (players.isEmpty()) {
//            Chat.warn(context, "No online players to shuffle");
//            return;
//        }
//        Collections.shuffle(players);
//        var leftOver = roster.deal(players, PER_TEAM);
//        for (var game : store.games()) {
//            var overrides = OverridesComponent.TYPE.of(game).memberOverrides();
//            for (var username : players) {
//                overrides.remove(Team.normalize(username));
//            }
//        }
//        int dealt = players.size() - leftOver.size();
//        Chat.ok(context, "Shuffled " + dealt + (dealt == 1 ? " player" : " players") + " onto teams of " + PER_TEAM);
//        for (var team : roster.participantTeams()) {
//            if (team.size() > 0) {
//                Chat.line(context, team.id() + ":", String.join(", ", team.members()));
//            }
//        }
//        if (!leftOver.isEmpty()) {
//            Chat.warn(context, "Every team is full. Left without a team: " + String.join(", ", leftOver));
//        }
    }
}
