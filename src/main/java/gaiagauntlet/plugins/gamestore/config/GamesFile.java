package gaiagauntlet.plugins.gamestore.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.logger.HytaleLogger;
import gaiagauntlet.plugins.gamestore.components.GameComponent;
import gaiagauntlet.plugins.gamestore.components.GameComponentType;
import gaiagauntlet.plugins.gamestore.components.GameComponents;
import gaiagauntlet.plugins.gamestore.game.Game;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The on-disk shape of the game list. Each game stores its id, hub, lobby slots, and one document per
 * persistent component, keyed by component id, so any plugin's component rides along.
 */
public final class GamesFile {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final BuilderCodec<GamesFile> CODEC = BuilderCodec.builder(GamesFile.class, GamesFile::new)
            .append(new KeyedCodec<>("Games", new ArrayCodec<>(Entry.CODEC, Entry[]::new)),
                    (file, v) -> file.games = v == null ? new Entry[0] : v, file -> file.games)
            .documentation("Every game that can run, with each of its saved components.")
            .add()
            .build();

    private Entry[] games = new Entry[0];

    public GamesFile() {
    }

    @Nonnull
    public List<Entry> games() {
        return List.of(games);
    }

    public void setGames(@Nonnull List<Entry> entries) {
        games = entries.toArray(Entry[]::new);
    }

    /** Encodes every persistent component on the game, plus any data saved by components nobody registered. */
    @Nonnull
    public static Entry entryOf(@Nonnull Game game) {
        var entry = new Entry();
        entry.id = game.id();
        entry.hub = game.hubId();
        entry.type = game.typeId();
        entry.lobbies = game.lobbyNames().toArray(String[]::new);
        entry.open = game.isOpen();
        var data = new LinkedHashMap<String, BsonDocument>(game.unknownData());
        for (var type : game.componentTypes()) {
            encode(game, type, data);
        }
        entry.data = data;
        return entry;
    }

    @SuppressWarnings("unchecked")
    private static <T extends GameComponent> void encode(@Nonnull Game game, @Nonnull GameComponentType<T> type,
                                                         @Nonnull Map<String, BsonDocument> into) {
        var codec = type.codec();
        var component = game.get(type);
        if (codec == null || component == null) {
            return;
        }
        try {
            into.put(type.id(), codec.encode(component, new ExtraInfo()));
        } catch (RuntimeException e) {
            LOGGER.atWarning().withCause(e).log("Could not save component %s of game %s", type.id(), game.id());
        }
    }

    /** Restores every saved component onto the game; unknown ids are kept for the next save. */
    public static void restore(@Nonnull Entry entry, @Nonnull Game game) {
        for (var saved : entry.data.entrySet()) {
            var type = GameComponents.find(saved.getKey());
            if (type.isEmpty()) {
                game.unknownData().put(saved.getKey(), saved.getValue());
                continue;
            }
            decode(game, type.get(), saved.getValue());
        }
    }

    private static <T extends GameComponent> void decode(@Nonnull Game game, @Nonnull GameComponentType<T> type,
            @Nonnull BsonDocument document) {
        var codec = type.codec();
        if (codec == null) {
            return;
        }
        try {
            game.put(type, codec.decode(document, new ExtraInfo()));
        } catch (RuntimeException e) {
            LOGGER.atWarning().withCause(e).log("Could not load component %s of game %s; using defaults", type.id(), game.id());
        }
    }

    /** One game as stored. */
    public static final class Entry {

        public static final BuilderCodec<Entry> CODEC = BuilderCodec.builder(Entry.class, Entry::new)
                .append(new KeyedCodec<>("Id", Codec.STRING), (e, v) -> e.id = v, e -> e.id)
                .addValidator(Validators.nonEmptyString())
                .documentation("Game id used by commands.")
                .add()
                .append(new KeyedCodec<>("Hub", Codec.STRING), (e, v) -> e.hub = v == null ? "" : v, e -> e.hub)
                .documentation("Id of the hub this game belongs to.")
                .add()
                .append(new KeyedCodec<>("Type", Codec.STRING), (e, v) -> e.type = v == null ? "" : v, e -> e.type)
                .documentation("Id of the game type this game is an instance of.")
                .add()
                .append(new KeyedCodec<>("Lobbies", new ArrayCodec<>(Codec.STRING, String[]::new)),
                        (e, v) -> e.lobbies = v == null ? new String[0] : v, e -> e.lobbies)
                .documentation("Names of the lobby slots feeding this game's arena, primary first.")
                .add()
                .append(new KeyedCodec<>("Open", Codec.BOOLEAN), (e, v) -> e.open = v != null && v, e -> e.open)
                .documentation("Whether the game's lobby instances are spawned at boot.")
                .add()
                .append(new KeyedCodec<>("Components", new MapCodec<>(Codec.BSON_DOCUMENT, LinkedHashMap::new)),
                        (e, v) -> e.data = v == null ? new LinkedHashMap<>() : new LinkedHashMap<>(v), e -> e.data)
                .documentation("Component id to that component's saved data.")
                .add()
                .build();

        String id = "";
        String hub = "";
        String type = "";
        String[] lobbies = new String[0];
        boolean open;
        Map<String, BsonDocument> data = new LinkedHashMap<>();

        public Entry() {
        }

        @Nonnull
        public String id() {
            return id;
        }

        @Nonnull
        public String hub() {
            return hub;
        }

        @Nonnull
        public String type() {
            return type;
        }

        @Nonnull
        public List<String> lobbies() {
            return List.of(lobbies);
        }

        public boolean open() {
            return open;
        }
    }

    static List<Entry> unused() {
        return new ArrayList<>();
    }
}
