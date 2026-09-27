package gaiagauntlet.plugins.gamestore.game;

import com.hypixel.hytale.server.core.universe.world.World;
import gaiagauntlet.plugins.gamestore.components.GameComponent;
import gaiagauntlet.plugins.gamestore.components.GameComponentType;
import gaiagauntlet.plugins.gamestore.config.GameType;
import gaiagauntlet.plugins.gamestore.constants.GameTypes;
import lombok.Getter;
import lombok.Setter;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One game: an id, its type, the hub it belongs to, the names of its lobby slots, and a store of components.
 * Everything a feature knows about the game lives in the component it attached, so games grow by
 * adding components, from this plugin or any other, and the core stays unaware of them.
 */
public final class Game {

    public static final String DEFAULT_ID = "main";

    private final String id;
    private final String hubId;
    private final String typeId;
    private final List<String> lobbyNames = new java.util.concurrent.CopyOnWriteArrayList<>();
    /**
     * -- GETTER --
     * True while the game's lobby instances exist and players may join. Saved, so an open game reopens after a restart.
     */
    @Setter
    @Getter
    private volatile boolean open;
    private final Map<GameComponentType<?>, GameComponent> components = new ConcurrentHashMap<>();
    /** Saved data for component types no loaded plugin registered, kept so it is not lost on the next save. */
    private final Map<String, BsonDocument> unknownData = new ConcurrentHashMap<>();

    public Game(@Nonnull String id, @Nonnull String hubId, @Nonnull String typeId, @Nonnull List<String> lobbyNames) {
        this.id = id;
        this.hubId = hubId;
        this.typeId = typeId;
        this.lobbyNames.addAll(lobbyNames);
    }

    /** The kind of game this is, by {@link GameType} id. */
    @Nonnull
    public String typeId() {
        return typeId;
    }

    @Nonnull
    public GameType type() {
        return GameTypes.find(typeId).orElse(GameTypes.DEFAULT);
    }

    /** The hub this game belongs to and whose world its lobbies are spawned from. */
    @Nonnull
    public String hubId() {
        return hubId;
    }

    @Nonnull
    public static String normalizeId(@Nonnull String raw) {
        return raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
    }

    @Nonnull
    public String id() {
        return id;
    }

    /** Names of the lobby slots that feed this game's arena, in order. Each becomes an instance while the game is open. */
    @Nonnull
    public List<String> lobbyNames() {
        return List.copyOf(lobbyNames);
    }

    public void addLobbyName(@Nonnull String name) {
        if (!lobbyNames.contains(name)) {
            lobbyNames.add(name);
        }
    }

    public boolean removeLobbyName(@Nonnull String name) {
        return lobbyNames.size() > 1 && lobbyNames.remove(name);
    }

    // Components

    /**
     * The component, created on first use. A type registered after this game was loaded picks up
     * any data that was saved under its id.
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public <T extends GameComponent> T ensure(@Nonnull GameComponentType<T> type) {
        return (T) components.computeIfAbsent(type, t -> {
            var saved = unknownData.remove(type.id());
            var codec = type.codec();
            if (saved != null && codec != null) {
                try {
                    return codec.decode(saved, new com.hypixel.hytale.codec.ExtraInfo());
                } catch (RuntimeException e) {
                    unknownData.put(type.id(), saved);
                }
            }
            return type.create();
        });
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends GameComponent> T get(@Nonnull GameComponentType<T> type) {
        return (T) components.get(type);
    }

    public boolean has(@Nonnull GameComponentType<?> type) {
        return components.containsKey(type);
    }

    public <T extends GameComponent> void put(@Nonnull GameComponentType<T> type, @Nonnull T component) {
        components.put(type, component);
    }

    public void remove(@Nonnull GameComponentType<?> type) {
        components.remove(type);
    }

    @Nonnull
    public List<GameComponentType<?>> componentTypes() {
        return new ArrayList<>(components.keySet());
    }

    @Nonnull
    public Map<String, BsonDocument> unknownData() {
        return unknownData;
    }

    /** True when the world is one this game's components claim. */
    public boolean owns(@Nonnull World world) {
        for (var component : components.values()) {
            if (component instanceof WorldOwner owner && owner.ownsWorld(world)) {
                return true;
            }
        }
        return false;
    }

    /** Tells every component the match is over. */
    public void resetMatch() {
        for (var component : components.values()) {
            component.resetMatch();
        }
    }

    /** A component that holds one of the game's worlds answers here so {@link Game#owns} can find it. */
    public interface WorldOwner {
        boolean ownsWorld(@Nonnull World world);
    }

    @Override
    public String toString() {
        return id;
    }
}
