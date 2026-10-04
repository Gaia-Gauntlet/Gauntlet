package com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.components;

import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponentType;
import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameComponentRegistry;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The bosses out in the game's arena and the ones waiting for their spawn chunk. A boss stays out until
 * it is defeated or the match resets, including while its chunk is unloaded. Arena thread only.
 */
public final class BossesComponent implements GameComponent {

    public static final GameComponentType<BossesComponent> TYPE = GameComponentRegistry.register(
            "Bosses", BossesComponent.class, BossesComponent.CODEC);

    // TODO: Does this need populating?
    //  If the server shuts down the game is dead anyways so it doesn't need to persist bosses I think?
    public static final BuilderCodec<BossesComponent> CODEC = BuilderCodec
        .builder(BossesComponent.class, BossesComponent::new)
        .build();

    private final Map<String, Active> active = new LinkedHashMap<>();
    private final List<String> pending = new ArrayList<>();
    /** Bosses already drawn at random since the bag last emptied. Kept across matches, so bosses take turns. */
    private final Set<String> drawn = new HashSet<>();

    /** One boss that is out, and its entity, which is invalid while unloaded. */
    public record Active(@Nonnull String bossId, @Nonnull Ref<EntityStore> ref, @Nonnull String zoneId) {
    }

    @Nonnull
    public List<Active> active() {
        return List.copyOf(active.values());
    }

    public boolean isActive(@Nonnull String bossId) {
        return active.containsKey(bossId);
    }

    /** Points a boss that is out at its entity again after the entity loads back in. */
    void rebind(@Nonnull String bossId, @Nonnull Ref<EntityStore> ref) {
        var boss = active.get(bossId);
        if (boss != null) {
            active.put(bossId, new Active(boss.bossId(), ref, boss.zoneId()));
        }
    }

    void add(@Nonnull Active boss) {
        active.put(boss.bossId(), boss);
    }

    void remove(@Nonnull String bossId) {
        active.remove(bossId);
    }

    @Nonnull
    public List<String> getPending() {
        return pending;
    }

    /**
     * Picks one of the candidates, skipping any already drawn until every candidate has had a turn, then
     * starting a fresh round.
     */
    @Nonnull
    public String drawFrom(@Nonnull List<String> candidates) {
        var fresh = candidates.stream().filter(id -> !drawn.contains(id)).toList();
        if (fresh.isEmpty()) {
            candidates.forEach(drawn::remove);
            fresh = candidates;
        }
        var pick = fresh.get(ThreadLocalRandom.current().nextInt(fresh.size()));
        drawn.add(pick);
        return pick;
    }

    /** Living plus waiting bosses. */
    public int count() {
        return active().size() + pending.size();
    }

    public void reset() {
        active.clear();
        pending.clear();
    }
}
