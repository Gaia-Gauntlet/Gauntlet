package gaiagauntlet.plugins.teams;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.util.Config;
import gaiagauntlet.plugins.teams.playerids.PlayerIds;
import gaiagauntlet.plugins.teams.team.Team;
import gaiagauntlet.plugins.teams.team.TeamsFile;
import gaiagauntlet.plugins.teams.constants.TeamType;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

/**
 * All teams and who is on them. Edited at runtime by commands and the dashboard; every change is
 * saved through the server's {@link Config} with the {@link TeamsFile} codec. Seeded from the bundled
 * defaults on first boot.
 */
public final class Roster {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String DEFAULTS_RESOURCE = "Server/GG/Defaults/teams.json";

    private final Config<TeamsFile> config;
    private final Path file;
    private final Map<String, Team> teams = new LinkedHashMap<>();

    public Roster(@Nonnull Path dataDirectory, @Nonnull Config<TeamsFile> config) {
        this.config = config;
        this.file = dataDirectory.resolve("teams.json");
    }

    // Queries

    @Nonnull
    public synchronized List<Team> teams() {
        return new ArrayList<>(teams.values());
    }

    @Nonnull
    public synchronized List<Team> participantTeams() {
        return teams.values().stream().filter(Team::isParticipant).toList();
    }

    @Nonnull
    public synchronized Optional<Team> team(@Nonnull String id) {
        return Optional.ofNullable(teams.get(id));
    }

    /** The team a player is rostered on, matched by username ignoring case. */
    @Nonnull
    public synchronized Optional<Team> teamOf(@Nonnull String username) {
        for (var team : teams.values()) {
            if (team.contains(username)) {
                return Optional.of(team);
            }
        }
        return Optional.empty();
    }

    public synchronized int participantCapacity() {
        return teams.values().stream().filter(Team::isParticipant).mapToInt(Team::maxSize).sum();
    }

    // Edits

    @Nonnull
    public synchronized Team create(@Nonnull String id, @Nonnull String name, @Nonnull TeamType type, int maxSize) {
        if (teams.containsKey(id)) {
            throw new IllegalArgumentException("Team '" + id + "' already exists");
        }
        var team = new Team(id, name, type, maxSize);
        teams.put(id, team);
        save();
        return team;
    }

    public synchronized void delete(@Nonnull String id) {
        if (teams.remove(id) == null) {
            throw new IllegalArgumentException("No team '" + id + "'");
        }
        save();
    }

    /** Puts the player on the team, removing them from any other team first. */
    public synchronized void assign(@Nonnull String username, @Nonnull String teamId) {
        var team = teams.get(teamId);
        if (team == null) {
            throw new IllegalArgumentException("No team '" + teamId + "'");
        }
        if (!team.contains(username) && team.isFull()) {
            throw new IllegalArgumentException(team.name() + " is full (" + team.maxSize() + ")");
        }
        for (var other : teams.values()) {
            other.remove(username);
        }
        team.add(username);
        save();
        PlayerIds.get().resolve(List.of(username));
    }

    /**
     * Empties every participant team, then deals the players out in the given order, filling the
     * participant teams in a random order, each to the given size or to its own max size when that is
     * smaller, before moving to the next. Returns the players left over once every team is full.
     */
    @Nonnull
    public synchronized List<String> deal(@Nonnull List<String> usernames, int perTeam) {
        var participants = new ArrayList<>(teams.values().stream().filter(Team::isParticipant).toList());
        Collections.shuffle(participants);
        for (var team : participants) {
            team.clearMembers();
        }
        var next = 0;
        for (var team : participants) {
            int size = Math.min(perTeam, team.maxSize());
            for (int i = 0; i < size && next < usernames.size(); i++) {
                team.add(usernames.get(next++));
            }
        }
        save();
        return new ArrayList<>(usernames.subList(next, usernames.size()));
    }

    /** Removes the player from whatever team they are on. Returns false when they were on none. */
    public synchronized boolean unassign(@Nonnull String username) {
        var removed = false;
        for (var team : teams.values()) {
            removed |= team.remove(username);
        }
        if (removed) {
            save();
        }
        return removed;
    }

    /** Applies any edit to a team and saves. */
    public synchronized void update(@Nonnull String teamId, @Nonnull Consumer<Team> edit) {
        var team = teams.get(teamId);
        if (team == null) {
            throw new IllegalArgumentException("No team '" + teamId + "'");
        }
        edit.accept(team);
        save();
    }

    // Persistence

    /** Replaces every team with the bundled defaults, the same as a first boot with no teams file. */
    public synchronized void resetToDefaults() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new IllegalStateException("Could not remove " + file.getFileName() + ": " + e.getMessage(), e);
        }
        load();
    }

    /** Copies the bundled defaults into place on first boot, then loads the file through its codec. */
    public synchronized void load() {
        if (!Files.exists(file)) {
            seedDefaults();
        }
        var loaded = config.load().join();
        teams.clear();
        for (var entry : loaded.teams()) {
            var team = entry.toTeam();
            teams.put(team.id(), team);
        }
        LOGGER.atInfo().log("Loaded %d teams", teams.size());
        save();
    }

    private void seedDefaults() {
        try (var stream = Roster.class.getClassLoader().getResourceAsStream(DEFAULTS_RESOURCE)) {
            if (stream == null) {
                LOGGER.atWarning().log("No bundled team defaults at %s; starting with no teams", DEFAULTS_RESOURCE);
                return;
            }
            Files.createDirectories(file.getParent());
            Files.copy(stream, file);
            LOGGER.atInfo().log("Seeded teams from bundled defaults");
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Could not seed team defaults");
        }
    }

    public synchronized void save() {
        var entries = new ArrayList<TeamsFile.Entry>();
        for (var team : teams.values()) {
            entries.add(TeamsFile.entryOf(team));
        }
        config.get().setTeams(entries);
        config.save().exceptionally(e -> {
            LOGGER.atSevere().withCause(e).log("Could not save teams");
            return null;
        });
    }
}
