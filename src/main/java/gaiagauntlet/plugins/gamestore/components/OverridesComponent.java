package gaiagauntlet.plugins.gamestore.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import gaiagauntlet.plugins.gamestore.store.GlobalStore;
import gaiagauntlet.plugins.settings.constants.Settings;
import gaiagauntlet.plugins.teams.TeamsPlugin;
import gaiagauntlet.plugins.teams.team.Team;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** A game's overrides on the global settings and roster. Saved with the game. */
public final class OverridesComponent implements GameComponent {

    public static final BuilderCodec<OverridesComponent> CODEC = BuilderCodec.builder(OverridesComponent.class, OverridesComponent::new)
            .append(new KeyedCodec<>("Settings", new MapCodec<>(Codec.STRING, LinkedHashMap::new)),
                    (c, v) -> c.readSettings(v), OverridesComponent::writeSettings)
            .documentation("Setting id to value, overriding the global settings for this game only.")
            .add()
            .append(new KeyedCodec<>("Teams", new ArrayCodec<>(Codec.STRING, String[]::new)),
                    (c, v) -> { c.teams.clear(); if (v != null) { c.teams.addAll(List.of(v)); } },
                    c -> c.teams.toArray(String[]::new))
            .documentation("Team ids taking part. Empty means every team.")
            .add()
            .append(new KeyedCodec<>("Members", new MapCodec<>(Codec.STRING, LinkedHashMap::new)),
                    (c, v) -> { c.memberOverrides.clear(); if (v != null) { c.memberOverrides.putAll(v); } },
                    c -> new LinkedHashMap<>(c.memberOverrides))
            .documentation("Lower-cased username to team id, overriding the roster for this game only.")
            .add()
            .build();

    public static final GameComponentType<OverridesComponent> TYPE = GameComponents.register(
            "Overrides", OverridesComponent.class, OverridesComponent::new, CODEC);

    private final Map<String, Object> settingOverrides = new ConcurrentHashMap<>();
    private final Set<String> teams = ConcurrentHashMap.newKeySet();
    private final Map<String, String> memberOverrides = new ConcurrentHashMap<>();

    public OverridesComponent() {
    }

    @Nonnull
    public Map<String, Object> settingOverrides() {
        return settingOverrides;
    }

    /** Team ids taking part; empty means every team. */
    @Nonnull
    public Set<String> teams() {
        return teams;
    }

    /** Lower-cased username to team id for this game only. */
    @Nonnull
    public Map<String, String> memberOverrides() {
        return memberOverrides;
    }

    /** The team a player is on in this game: the override first, then the global roster, filtered to this game's teams. */
    @Nonnull
    public Optional<Team> teamOf(@Nonnull GlobalStore store, @Nonnull String username) {
        var override = memberOverrides.get(Team.normalize(username));
        var roster = TeamsPlugin.get().getRoster();
        if (override != null) {
            return roster.team(override);
        }
        return roster.teamOf(username).filter(team -> teams.isEmpty() || teams.contains(team.id()));
    }

    @Nonnull
    public List<Team> participantTeams(@Nonnull GlobalStore store) {
        var all = TeamsPlugin.get().getRoster().participantTeams();
        return teams.isEmpty() ? all : all.stream().filter(team -> teams.contains(team.id())).toList();
    }

    private void readSettings(Map<String, String> raw) {
        settingOverrides.clear();
        if (raw == null) {
            return;
        }
        for (var entry : raw.entrySet()) {
            Settings.get().find(entry.getKey()).ifPresent(key -> {
                try {
                    settingOverrides.put(key.id(), key.parse(entry.getValue()));
                } catch (IllegalArgumentException ignored) {
                    // A stale override for a changed range is dropped rather than failing the whole file.
                }
            });
        }
    }

    @Nonnull
    private LinkedHashMap<String, String> writeSettings() {
        var out = new LinkedHashMap<String, String>();
        settingOverrides.forEach((k, v) -> out.put(k, String.valueOf(v)));
        return out;
    }
}
