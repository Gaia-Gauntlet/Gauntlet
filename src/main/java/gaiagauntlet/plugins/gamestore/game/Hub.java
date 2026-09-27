package gaiagauntlet.plugins.gamestore.game;

import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A hub: a permanent world players gather in, from which games are created and joined. Each game
 * belongs to one hub, and a game's lobby instances are spawned from the hub's world.
 */
public final class Hub {

    public static final String DEFAULT_ID = "main";

    private final String id;
    private final String worldName;
    @Nullable private volatile World world;

    public Hub(@Nonnull String id, @Nonnull String worldName) {
        this.id = id;
        this.worldName = worldName;
    }

    @Nonnull
    public String id() {
        return id;
    }

    @Nonnull
    public String worldName() {
        return worldName;
    }

    /** The hub world when it is loaded and alive. */
    @Nullable
    public World world() {
        var w = world;
        return w != null && w.isAlive() ? w : null;
    }

    public void setWorld(@Nullable World world) {
        this.world = world;
    }

    public boolean ownsWorld(@Nonnull World candidate) {
        return candidate.equals(world);
    }

    @Override
    public String toString() {
        return id;
    }
}
