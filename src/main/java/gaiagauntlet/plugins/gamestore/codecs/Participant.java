package gaiagauntlet.plugins.gamestore.codecs;

import com.hypixel.hytale.math.vector.Transform;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/** One player's standing in one game. Created when the transfer is planned, kept until the game returns to IDLE. */
public final class Participant {

    private final UUID uuid;
    private final String username;
    @Nullable private final String teamId;
    @Getter private final boolean competitor;
    private final String sourceLobby;
    private volatile Transform spawn;
    @Getter
    private volatile boolean alive = true;
    @Setter @Getter private volatile boolean online = true;
    @Nullable private volatile UUID killer;
    private volatile long deathTimeMillis;
    private volatile double kills;

    public Participant(@Nonnull UUID uuid, @Nonnull String username, @Nullable String teamId, boolean competitor,
                       @Nonnull String sourceLobby, @Nonnull Transform spawn) {
        this.uuid = uuid;
        this.username = username;
        this.teamId = teamId;
        this.competitor = competitor;
        this.sourceLobby = sourceLobby;
        this.spawn = spawn;
        this.kills = 0;
    }

    /** The lobby world the player came from and returns to. */
    @Nonnull
    public String sourceLobby() {
        return sourceLobby;
    }

    @Nonnull
    public UUID uuid() {
        return uuid;
    }

    @Nonnull
    public String username() {
        return username;
    }

    @Nullable
    public String teamId() {
        return teamId;
    }

    @Nonnull
    public Transform spawn() {
        return spawn;
    }

    public void setSpawn(@Nonnull Transform spawn) {
        this.spawn = spawn;
    }

    @Nullable
    public UUID killer() {
        return killer;
    }

    public void addKill() {
        kills++;
    }

    public double kills() {
        return kills;
    }

    public long deathTimeMillis() {
        return deathTimeMillis;
    }

    public void markAlive() {
        alive = true;
        killer = null;
        deathTimeMillis = 0;
    }

    public void markDead(@Nullable UUID killer) {
        if (!alive) {
            return;
        }
        alive = false;
        this.killer = killer;
        deathTimeMillis = System.currentTimeMillis();
    }
}
