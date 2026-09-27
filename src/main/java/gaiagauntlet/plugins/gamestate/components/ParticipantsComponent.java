package gaiagauntlet.plugins.gamestate.components;

import gaiagauntlet.plugins.gamestore.codecs.Participant;
import gaiagauntlet.plugins.gamestore.components.GameComponent;
import gaiagauntlet.plugins.gamestore.components.GameComponentType;
import gaiagauntlet.plugins.gamestore.components.GameComponents;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Everyone in the current match and their standing. Filled when the transfer is planned. */
public final class ParticipantsComponent implements GameComponent {

    public static final GameComponentType<ParticipantsComponent> TYPE = GameComponents.register(
            "Participants", ParticipantsComponent.class, ParticipantsComponent::new);

    private final Map<UUID, Participant> participants = new ConcurrentHashMap<>();

    @Nonnull
    public Map<UUID, Participant> all() {
        return participants;
    }

    @Nullable
    public Participant get(@Nonnull UUID uuid) {
        return participants.get(uuid);
    }

    @Nullable
    public Participant byName(@Nonnull String username) {
        for (var p : participants.values()) {
            if (p.username().equalsIgnoreCase(username)) {
                return p;
            }
        }
        return null;
    }

    public boolean isEmpty() {
        return participants.isEmpty();
    }

    /** Competitors on the team who are alive and online. */
    public boolean isTeamAlive(@Nonnull String teamId) {
        for (var p : participants.values()) {
            if (teamId.equals(p.teamId()) && p.isCompetitor() && p.isAlive() && p.isOnline()) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    public List<String> aliveTeamIds() {
        var alive = new LinkedHashSet<String>();
        for (var p : participants.values()) {
            if (p.teamId() != null && p.isCompetitor() && p.isAlive() && p.isOnline()) {
                alive.add(p.teamId());
            }
        }
        return new ArrayList<>(alive);
    }

    public int aliveCompetitors() {
        int count = 0;
        for (var p : participants.values()) {
            if (p.isCompetitor() && p.isAlive() && p.isOnline()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void resetMatch() {
        participants.clear();
    }
}
