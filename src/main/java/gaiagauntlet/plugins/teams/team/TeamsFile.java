package gaiagauntlet.plugins.teams.team;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;
import gaiagauntlet.plugins.teams.constants.TeamType;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/** The on-disk shape of the roster: a list of teams, each with its members. */
public final class TeamsFile {

    public static final BuilderCodec<TeamsFile> CODEC = BuilderCodec.builder(TeamsFile.class, TeamsFile::new)
            .append(new KeyedCodec<>("Teams", new ArrayCodec<>(Entry.CODEC, Entry[]::new)),
                    (file, v) -> file.teams = v == null ? new Entry[0] : v, file -> file.teams)
            .documentation("Every team and its roster.")
            .add()
            .build();

    private Entry[] teams = new Entry[0];

    public TeamsFile() {
    }

    @Nonnull
    public List<Entry> teams() {
        return List.of(teams);
    }

    public void setTeams(@Nonnull List<Entry> entries) {
        teams = entries.toArray(Entry[]::new);
    }

    @Nonnull
    public static Entry entryOf(@Nonnull Team team) {
        var entry = new Entry();
        entry.id = team.id();
        entry.name = team.name();
        entry.type = team.type();
        entry.maxSize = team.maxSize();
        entry.icon = team.icon();
        entry.color = team.color();
        entry.members = team.members().toArray(String[]::new);
        return entry;
    }

    /** One team as stored. */
    public static final class Entry {

        public static final BuilderCodec<Entry> CODEC = BuilderCodec.builder(Entry.class, Entry::new)
                .append(new KeyedCodec<>("Id", Codec.STRING), (e, v) -> e.id = v, e -> e.id)
                .addValidator(Validators.nonEmptyString())
                .documentation("Team id used by commands. Letters, digits, and underscores.")
                .add()
                .append(new KeyedCodec<>("Name", Codec.STRING), (e, v) -> e.name = v, e -> e.name)
                .documentation("Display name shown to players.")
                .add()
                .append(new KeyedCodec<>("Type", new EnumCodec<>(TeamType.class)), (e, v) -> e.type = v == null ? TeamType.Participant : v, e -> e.type)
                .documentation("Participant teams compete; Camera teams only watch.")
                .add()
                .append(new KeyedCodec<>("MaxSize", Codec.INTEGER), (e, v) -> e.maxSize = v == null ? 3 : v, e -> e.maxSize)
                .addValidator(Validators.greaterThanOrEqual(1))
                .documentation("Most players the team can hold.")
                .add()
                .append(new KeyedCodec<>("Icon", Codec.STRING), (e, v) -> e.icon = v == null ? "" : v, e -> e.icon)
                .documentation("UI icon path, for example UI/Custom/GG/TeamIcons/Goblin.png.")
                .add()
                .append(new KeyedCodec<>("Color", Codec.STRING), (e, v) -> e.color = v == null ? "#FFFFFF" : v, e -> e.color)
                .documentation("Hex colour for the team.")
                .add()
                .append(new KeyedCodec<>("Members", new ArrayCodec<>(Codec.STRING, String[]::new)),
                        (e, v) -> e.members = v == null ? new String[0] : v, e -> e.members)
                .documentation("Usernames on the team, matched ignoring case.")
                .add()
                .build();

        String id = "";
        String name = "";
        TeamType type = TeamType.Participant;
        int maxSize = 3;
        String icon = "";
        String color = "#FFFFFF";
        String[] members = new String[0];

        public Entry() {
        }

        @Nonnull
        public Team toTeam() {
            var team = new Team(id, name.isEmpty() ? id : name, type, maxSize);
            team.setIcon(icon);
            team.setColor(color);
            for (var member : members) {
                team.add(member);
            }
            return team;
        }
    }

    static List<Entry> unused() {
        return new ArrayList<>();
    }
}
