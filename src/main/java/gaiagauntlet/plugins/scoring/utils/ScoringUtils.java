package gaiagauntlet.plugins.scoring.utils;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.util.Config;
import gaiagauntlet.plugins.gamestate.components.ParticipantsComponent;
import gaiagauntlet.plugins.gamestore.components.OverridesComponent;
import gaiagauntlet.plugins.gamestore.game.Game;
import gaiagauntlet.plugins.gamestore.store.GlobalStore;
import gaiagauntlet.plugins.scoring.components.StandingsComponent;
import gaiagauntlet.plugins.scoring.config.MatchResultFile;
import gaiagauntlet.plugins.settings.constants.Settings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ScoringUtils {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    /** A team earns one bonus point for every this many kills its members made. */
    private static final int KILLS_PER_POINT = 2; // TODO: Move to config

    private ScoringUtils() {}

    /**
     * Ranks every competing team: the survivors first, then by how late each was eliminated. Each team
     * scores its placement's points plus a bonus for its kills. Safe for any team count.
     */
    @Nonnull
    public static List<StandingsComponent.Standing> rank(@Nonnull Game game, @Nullable String winningTeamId) {
        var store = GlobalStore.get();
        var table = parseTable(store.settingsOf(game).get(Settings.MATCH_SCORE_TABLE));
        record Entry(String teamId, String name, boolean alive, long eliminatedAt, double kills) {
        }
        var entries = new ArrayList<Entry>();
        var participants = ParticipantsComponent.TYPE.of(game);
        for (var team : OverridesComponent.TYPE.of(game).participantTeams(store)) {
            long lastDeath = 0;
            double kills = 0;
            boolean anyMember = false;
            for (var p : participants.all().values()) {
                if (team.id().equals(p.teamId()) && p.isCompetitor()) {
                    anyMember = true;
                    lastDeath = Math.max(lastDeath, p.deathTimeMillis());
                    kills += p.kills();
                }
            }
            if (!anyMember) {
                continue;
            }
            var alive = team.id().equals(winningTeamId) || participants.isTeamAlive(team.id());
            entries.add(new Entry(team.id(), team.name(), alive, alive ? Long.MAX_VALUE : lastDeath, kills));
        }
        entries.sort(Comparator.comparing((Entry e) -> e.alive).reversed().thenComparing(e -> -e.eliminatedAt));
        var standings = new ArrayList<StandingsComponent.Standing>();
        for (int i = 0; i < entries.size(); i++) {
            var e = entries.get(i);
            int placement = table.isEmpty() ? 0 : table.get(Math.min(i, table.size() - 1));
            int points = placement + (int) (e.kills / KILLS_PER_POINT);
            standings.add(new StandingsComponent.Standing(i + 1, e.teamId, e.name, e.teamId.equals(winningTeamId),
                points, e.alive ? 0 : e.eliminatedAt));
        }
        return standings;
    }

    /** Writes the standings and every participant's fate to results/{time}-{game}.json through the result codec. */
    public static void writeResults(@Nonnull Path resultsDirectory, @Nonnull Game game, @Nonnull List<StandingsComponent.Standing> standings) {
        var now = Instant.now();
        var stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneOffset.UTC).format(now);
        var name = stamp + "-" + game.id();
        try {
            java.nio.file.Files.createDirectories(resultsDirectory);
        } catch (java.io.IOException e) {
            LOGGER.atSevere().withCause(e).log("[%s] Could not create %s", game.id(), resultsDirectory);
            return;
        }
        var file = MatchResultFile.of(game, now.toString(), standings);
        Config.preloadedConfig(resultsDirectory, name, MatchResultFile.CODEC, file).save().whenComplete((v, e) -> {
            if (e != null) {
                LOGGER.atSevere().withCause(e).log("[%s] Could not write results", game.id());
            } else {
                LOGGER.atInfo().log("[%s] Results written to %s", game.id(), resultsDirectory.resolve(name + ".json"));
            }
        });
    }

    @Nonnull
    static List<Integer> parseTable(@Nonnull String text) {
        var table = new ArrayList<Integer>();
        for (var part : text.split(",")) {
            try {
                table.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {
                // A malformed entry is skipped so a typo in the table never breaks the end of a match.
            }
        }
        return table;
    }
}
