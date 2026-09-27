package gaiagauntlet.plugins.gamestore.constants;

import gaiagauntlet.plugins.gamestore.config.GameType;

import javax.annotation.Nonnull;
import java.util.*;

/** Every game type that can be created. This plugin registers the elimination match; mods register theirs. */
public final class GameTypes {

    public static final GameType DEFAULT = new GameType("default", "Default",
            "Fallback gametype.");

    private static final Map<String, GameType> TYPES = new LinkedHashMap<>();

    private GameTypes() {
    }

    public static synchronized void register(@Nonnull GameType type) {
        TYPES.put(type.id().toLowerCase(Locale.ROOT), type);
    }

    @Nonnull
    public static synchronized List<GameType> all() {
        return new ArrayList<>(TYPES.values());
    }

    @Nonnull
    public static synchronized Optional<GameType> find(@Nonnull String id) {
        return Optional.ofNullable(TYPES.get(id.trim().toLowerCase(Locale.ROOT)));
    }
}
