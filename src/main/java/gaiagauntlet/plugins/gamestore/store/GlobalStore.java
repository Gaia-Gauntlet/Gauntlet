package gaiagauntlet.plugins.gamestore.store;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.resources.UniverseResourceType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.util.Config;
import gaiagauntlet.plugins.gamestore.config.GameType;
import gaiagauntlet.plugins.gamestore.config.GamesFile;
import gaiagauntlet.plugins.gamestore.config.HubsFile;
import gaiagauntlet.plugins.gamestore.constants.GameTypes;
import gaiagauntlet.plugins.gamestore.game.Game;
import gaiagauntlet.plugins.gamestore.game.Hub;
import gaiagauntlet.plugins.gamestore.settings.GameSettings;
import gaiagauntlet.plugins.settings.SettingsPlugin;
import gaiagauntlet.plugins.settings.config.SettingsRegistry;
import gaiagauntlet.plugins.settings.constants.Settings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The one place global event state lives. Registered as a universe resource, so it exists exactly
 * once per server. It holds every {@link Hub} and every {@link Game}; their definitions persist
 * through the server's {@link Config} with the {@link HubsFile} and {@link GamesFile} codecs, so a
 * restart brings the same hubs and games back, games closed and IDLE.
 */
public final class GlobalStore {

    public static final String ID = "GG_GlobalStore";
    public static final BuilderCodec<GlobalStore> CODEC = BuilderCodec.builder(GlobalStore.class, GlobalStore::new).build();

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static UniverseResourceType<GlobalStore> type;
    private static Config<GamesFile> gamesConfig;
    private static Config<HubsFile> hubsConfig;

    private final Map<String, Hub> hubs = new ConcurrentHashMap<>();
    private final Map<String, Game> games = new ConcurrentHashMap<>();
    private volatile boolean loaded;

    public GlobalStore() {
    }

    public static void register(@Nonnull Config<HubsFile> hubs, @Nonnull Config<GamesFile> games) {
        hubsConfig = hubs;
        gamesConfig = games;
        type = Universe.registerResource(GlobalStore.class, ID, CODEC);
    }

    /** The store, or null before the universe has finished loading. */
    @Nullable
    public static GlobalStore find() {
        try {
            var store = Universe.get().getResource(type);
            store.ensureLoaded();
            return store;
        } catch (RuntimeException e) {
            LOGGER.atFine().withCause(e).log("Global store not available yet");
            return null;
        }
    }

    @Nonnull
    public static GlobalStore get() {
        var store = find();
        if (store == null) {
            throw new IllegalStateException("The universe is not ready; the global store does not exist yet");
        }
        return store;
    }

    @Nonnull
    public SettingsRegistry settings() {
        return SettingsPlugin.get().getSettings();
    }

    // Hubs

    @Nonnull
    public List<Hub> hubs() {
        return new ArrayList<>(hubs.values());
    }

    @Nonnull
    public Optional<Hub> hub(@Nonnull String id) {
        return Optional.ofNullable(hubs.get(Game.normalizeId(id)));
    }

    /** The default hub, created on first use with the configured hub world. */
    @Nonnull
    public Hub mainHub() {
        var hub = hubs.get(Hub.DEFAULT_ID);
        if (hub == null) {
            hub = new Hub(Hub.DEFAULT_ID, Settings.get().get(Settings.HUB_WORLD_NAME));
            hubs.put(Hub.DEFAULT_ID, hub);
            saveHubs();
        }
        return hub;
    }

    @Nonnull
    public Optional<Hub> hubFor(@Nonnull World world) {
        for (var hub : hubs.values()) {
            if (hub.ownsWorld(world)) {
                return Optional.of(hub);
            }
        }
        return Optional.empty();
    }

    /** Creates a hub with its own permanent world named after it. */
    @Nonnull
    public Hub createHub(@Nonnull String rawId) {
        var id = Game.normalizeId(rawId);
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Hub id must contain letters or digits");
        }
        if (hubs.containsKey(id)) {
            throw new IllegalArgumentException("Hub '" + id + "' already exists");
        }
        var worldName = id.equals(Hub.DEFAULT_ID) ? Settings.get().get(Settings.HUB_WORLD_NAME)
                : Settings.get().get(Settings.HUB_WORLD_NAME) + "-" + id;
        var hub = new Hub(id, worldName);
        hubs.put(id, hub);
        saveHubs();
        return hub;
    }

    // Games

    @Nonnull
    public List<Game> games() {
        return new ArrayList<>(games.values());
    }

    @Nonnull
    public List<Game> gamesOf(@Nonnull Hub hub) {
        return games.values().stream().filter(g -> g.hubId().equals(hub.id())).toList();
    }

    @Nonnull
    public Optional<Game> game(@Nonnull String id) {
        return Optional.ofNullable(games.get(Game.normalizeId(id)));
    }

    /** The default game on the main hub, created on first use. */
    @Nonnull
    public Game mainGame() {
        var game = games.get(Game.DEFAULT_ID);
        if (game == null) {
            game = new Game(Game.DEFAULT_ID, mainHub().id(), GameTypes.DEFAULT.id(), defaultLobbySlots());
            games.put(Game.DEFAULT_ID, game);
            saveGames();
        }
        return game;
    }

    /** The game that owns the world, if any. */
    @Nonnull
    public Optional<Game> gameFor(@Nonnull World world) {
        for (var game : games.values()) {
            if (game.owns(world)) {
                return Optional.of(game);
            }
        }
        return Optional.empty();
    }

    @Nonnull
    public GameSettings settingsOf(@Nonnull Game game) {
        return new GameSettings(game);
    }

    /** Creates a game of the type on the hub. An empty id becomes the type's id plus the next free number. */
    @Nonnull
    public Game createGame(@Nonnull Hub hub, @Nonnull GameType type, @Nonnull String rawId) {
        var id = rawId.isBlank() ? nextId(type) : Game.normalizeId(rawId);
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Game id must contain letters or digits");
        }
        if (games.containsKey(id)) {
            throw new IllegalArgumentException("Game '" + id + "' already exists");
        }
        var game = new Game(id, hub.id(), type.id(), defaultLobbySlots());
        games.put(id, game);
        saveGames();
        return game;
    }

    @Nonnull
    private String nextId(@Nonnull GameType type) {
        for (int n = 1; ; n++) {
            var candidate = Game.normalizeId(type.id() + "-" + n);
            if (!games.containsKey(candidate)) {
                return candidate;
            }
        }
    }

    /** Adds a lobby slot to the game. Slot names become part of the instance world name. */
    public void addLobby(@Nonnull Game game, @Nonnull String slotName) {
        var name = slotName.trim().toLowerCase(java.util.Locale.ROOT);
        if (!name.matches("[a-z0-9_-]+")) {
            throw new IllegalArgumentException("Lobby name must be letters, digits, '-' or '_'");
        }
        game.addLobbyName(name);
        saveGames();
    }

    /** Removes a lobby slot from the game. The last one cannot be removed. */
    public void removeLobby(@Nonnull Game game, @Nonnull String slotName) {
        if (!game.removeLobbyName(slotName.trim())) {
            throw new IllegalArgumentException("Cannot remove '" + slotName + "': not a lobby of " + game.id() + ", or the last one");
        }
        saveGames();
    }

    /** Forgets a game. The caller must have closed it first. */
    public void removeGame(@Nonnull String rawId) {
        var id = Game.normalizeId(rawId);
        if (id.equals(Game.DEFAULT_ID)) {
            throw new IllegalArgumentException("The main game cannot be removed");
        }
        if (games.remove(id) == null) {
            throw new IllegalArgumentException("No game '" + id + "'");
        }
        saveGames();
    }

    @Nonnull
    private static List<String> defaultLobbySlots() {
        var slots = new ArrayList<String>();
        int count = Settings.get().get(Settings.LOBBY_COUNT);
        for (int i = 1; i <= count; i++) {
            slots.add("lobby-" + i);
        }
        return slots;
    }

    // Persistence of game definitions and overrides

    private synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        for (var entry : hubsConfig.load().join().hubs()) {
            var id = Game.normalizeId(entry.id());
            var worldName = entry.world().isEmpty() ? Settings.get().get(Settings.HUB_WORLD_NAME) : entry.world();
            hubs.put(id, new Hub(id, worldName));
        }
        for (var entry : gamesConfig.load().join().games()) {
            try {
                var id = Game.normalizeId(entry.id());
                var hubId = entry.hub().isEmpty() ? Hub.DEFAULT_ID : Game.normalizeId(entry.hub());
                if (!hubs.containsKey(hubId)) {
                    hubs.put(hubId, new Hub(hubId, Settings.get().get(Settings.HUB_WORLD_NAME)
                            + (hubId.equals(Hub.DEFAULT_ID) ? "" : "-" + hubId)));
                }
                var lobbies = entry.lobbies().isEmpty() ? defaultLobbySlots() : entry.lobbies();
                var typeId = entry.type().isEmpty() ? GameTypes.DEFAULT.id() : entry.type();
                var game = new Game(id, hubId, typeId, lobbies);
                game.setOpen(entry.open());
                GamesFile.restore(entry, game);
                games.put(id, game);
            } catch (RuntimeException e) {
                LOGGER.atWarning().withCause(e).log("Skipping malformed game entry %s", entry.id());
            }
        }
        LOGGER.atInfo().log("Loaded %d hubs and %d games", hubs.size(), games.size());
    }

    public void saveHubs() {
        var entries = new ArrayList<HubsFile.Entry>();
        for (var hub : hubs.values()) {
            entries.add(HubsFile.entryOf(hub));
        }
        hubsConfig.get().setHubs(entries);
        hubsConfig.save().exceptionally(e -> {
            LOGGER.atSevere().withCause(e).log("Could not save hubs");
            return null;
        });
    }

    public void saveGames() {
        var entries = new ArrayList<GamesFile.Entry>();
        for (var game : games.values()) {
            entries.add(GamesFile.entryOf(game));
        }
        gamesConfig.get().setGames(entries);
        gamesConfig.save().exceptionally(e -> {
            LOGGER.atSevere().withCause(e).log("Could not save games");
            return null;
        });
    }
}
