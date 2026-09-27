package gaiagauntlet.plugins.teams.team;

import gaiagauntlet.plugins.teams.constants.TeamType;

import javax.annotation.Nonnull;
import java.util.*;

/** A team and its roster. Member names are matched case-insensitively but displayed as authored. */
public final class Team {

    private final String id;
    private String name;
    private String icon;
    private String color;
    private TeamType type;
    private int maxSize;
    /** Lower-cased username to authored username, in roster order. */
    private final Map<String, String> members = new LinkedHashMap<>();

    public Team(@Nonnull String id, @Nonnull String name, @Nonnull TeamType type, int maxSize) {
        this.id = id;
        this.name = name;
        this.icon = "";
        this.color = "#FFFFFF";
        this.type = type;
        this.maxSize = maxSize;
    }

    @Nonnull
    public static String normalize(@Nonnull String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    @Nonnull
    public String id() {
        return id;
    }

    @Nonnull
    public String name() {
        return name;
    }

    public void setName(@Nonnull String name) {
        this.name = name;
    }

    @Nonnull
    public String icon() {
        return icon;
    }

    public void setIcon(@Nonnull String icon) {
        this.icon = icon;
    }

    @Nonnull
    public String color() {
        return color;
    }

    public void setColor(@Nonnull String color) {
        this.color = color;
    }

    @Nonnull
    public TeamType type() {
        return type;
    }

    public void setType(@Nonnull TeamType type) {
        this.type = type;
    }

    public int maxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public boolean isParticipant() {
        return type == TeamType.Participant;
    }

    @Nonnull
    public synchronized List<String> members() {
        return Collections.unmodifiableList(new ArrayList<>(members.values()));
    }

    public synchronized int size() {
        return members.size();
    }

    public synchronized boolean isFull() {
        return members.size() >= maxSize;
    }

    public synchronized boolean contains(@Nonnull String username) {
        return members.containsKey(normalize(username));
    }

    public synchronized void add(@Nonnull String username) {
        members.put(normalize(username), username.trim());
    }

    public synchronized boolean remove(@Nonnull String username) {
        return members.remove(normalize(username)) != null;
    }

    public synchronized void clearMembers() {
        members.clear();
    }
}
