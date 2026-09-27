package gaiagauntlet.plugins.scoring.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import gaiagauntlet.plugins.gamestate.components.ParticipantsComponent;
import gaiagauntlet.plugins.gamestore.game.Game;
import gaiagauntlet.plugins.scoring.components.StandingsComponent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/** The on-disk shape of one match's results. */
public final class MatchResultFile {

    public static final BuilderCodec<MatchResultFile> CODEC = BuilderCodec.builder(MatchResultFile.class, MatchResultFile::new)
            .append(new KeyedCodec<>("Game", Codec.STRING), (f, v) -> f.game = v, f -> f.game)
            .documentation("Game id.")
            .add()
            .append(new KeyedCodec<>("EndedAt", Codec.STRING), (f, v) -> f.endedAt = v, f -> f.endedAt)
            .documentation("When the match ended, ISO-8601 UTC.")
            .add()
            .append(new KeyedCodec<>("Standings", new ArrayCodec<>(StandingEntry.CODEC, StandingEntry[]::new)),
                    (f, v) -> f.standings = v == null ? new StandingEntry[0] : v, f -> f.standings)
            .documentation("Teams in placement order.")
            .add()
            .append(new KeyedCodec<>("Players", new ArrayCodec<>(PlayerEntry.CODEC, PlayerEntry[]::new)),
                    (f, v) -> f.players = v == null ? new PlayerEntry[0] : v, f -> f.players)
            .documentation("Every participant's fate.")
            .add()
            .build();

    String game = "";
    String endedAt = "";
    StandingEntry[] standings = new StandingEntry[0];
    PlayerEntry[] players = new PlayerEntry[0];

    public MatchResultFile() {
    }

    @Nonnull
    public static MatchResultFile of(@Nonnull Game game, @Nonnull String endedAt, @Nonnull List<StandingsComponent.Standing> standings) {
        var file = new MatchResultFile();
        file.game = game.id();
        file.endedAt = endedAt;
        var rows = new ArrayList<StandingEntry>();
        for (var s : standings) {
            var row = new StandingEntry();
            row.place = s.place();
            row.team = s.teamId();
            row.name = s.teamName();
            row.points = s.points();
            row.winner = s.winner();
            row.eliminatedAtMillis = s.eliminatedAtMillis();
            rows.add(row);
        }
        file.standings = rows.toArray(StandingEntry[]::new);
        var players = new ArrayList<PlayerEntry>();
        var participants = ParticipantsComponent.TYPE.of(game);
        for (var p : participants.all().values()) {
            var row = new PlayerEntry();
            row.name = p.username();
            row.team = p.teamId() == null ? "" : p.teamId();
            row.competitor = p.isCompetitor();
            row.alive = p.isAlive();
            row.online = p.isOnline();
            var killer = p.killer() == null ? null : participants.get(p.killer());
            row.kills = p.kills();
            row.killedBy = killer == null ? "" : killer.username();
            row.deathTimeMillis = p.deathTimeMillis();
            players.add(row);
        }
        file.players = players.toArray(PlayerEntry[]::new);
        return file;
    }

    public static final class StandingEntry {

        public static final BuilderCodec<StandingEntry> CODEC = BuilderCodec.builder(StandingEntry.class, StandingEntry::new)
                .append(new KeyedCodec<>("Place", Codec.INTEGER), (e, v) -> e.place = v == null ? 0 : v, e -> e.place).add()
                .append(new KeyedCodec<>("Team", Codec.STRING), (e, v) -> e.team = v, e -> e.team).add()
                .append(new KeyedCodec<>("Name", Codec.STRING), (e, v) -> e.name = v, e -> e.name).add()
                .append(new KeyedCodec<>("Points", Codec.INTEGER), (e, v) -> e.points = v == null ? 0 : v, e -> e.points).add()
                .append(new KeyedCodec<>("Winner", Codec.BOOLEAN), (e, v) -> e.winner = v != null && v, e -> e.winner).add()
                .append(new KeyedCodec<>("EliminatedAtMillis", Codec.LONG), (e, v) -> e.eliminatedAtMillis = v == null ? 0 : v, e -> e.eliminatedAtMillis).add()
                .build();

        int place;
        String team = "";
        String name = "";
        int points;
        boolean winner;
        long eliminatedAtMillis;

        public StandingEntry() {
        }
    }

    public static final class PlayerEntry {

        public static final BuilderCodec<PlayerEntry> CODEC = BuilderCodec.builder(PlayerEntry.class, PlayerEntry::new)
                .append(new KeyedCodec<>("Name", Codec.STRING), (e, v) -> e.name = v, e -> e.name).add()
                .append(new KeyedCodec<>("Team", Codec.STRING), (e, v) -> e.team = v, e -> e.team).add()
                .append(new KeyedCodec<>("Competitor", Codec.BOOLEAN), (e, v) -> e.competitor = v != null && v, e -> e.competitor).add()
                .append(new KeyedCodec<>("Alive", Codec.BOOLEAN), (e, v) -> e.alive = v != null && v, e -> e.alive).add()
                .append(new KeyedCodec<>("Online", Codec.BOOLEAN), (e, v) -> e.online = v != null && v, e -> e.online).add()
                .append(new KeyedCodec<>("KilledBy", Codec.STRING), (e, v) -> e.killedBy = v, e -> e.killedBy).add()
                .append(new KeyedCodec<>("Kills", Codec.DOUBLE), (e, v) -> e.kills = v, e -> e.kills).add()
                .append(new KeyedCodec<>("DeathTimeMillis", Codec.LONG), (e, v) -> e.deathTimeMillis = v == null ? 0 : v, e -> e.deathTimeMillis).add()
                .build();

        String name = "";
        String team = "";
        boolean competitor;
        boolean alive;
        boolean online;
        String killedBy = "";
        long deathTimeMillis;
        double kills;

        public PlayerEntry() {
        }
    }
}
