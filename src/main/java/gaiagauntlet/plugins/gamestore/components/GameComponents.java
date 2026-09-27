package gaiagauntlet.plugins.gamestore.components;

import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** The registry of component types any plugin may attach to games. Register during plugin setup. */
public final class GameComponents {

    private static final Map<String, GameComponentType<?>> TYPES = new ConcurrentHashMap<>();

    private GameComponents() {
    }

    /** A component rebuilt fresh for every game and never saved. */
    @Nonnull
    public static <T extends GameComponent> GameComponentType<T> register(@Nonnull String id, @Nonnull Class<T> type,
            @Nonnull Supplier<T> factory) {
        return register(id, type, factory, null);
    }

    /** A component saved with the game through its codec and restored on boot. */
    @Nonnull
    public static <T extends GameComponent> GameComponentType<T> register(@Nonnull String id, @Nonnull Class<T> type,
            @Nonnull Supplier<T> factory, @Nullable BuilderCodec<T> codec) {
        var created = new GameComponentType<>(id, type, factory, codec);
        if (TYPES.putIfAbsent(id, created) != null) {
            throw new IllegalStateException("Game component '" + id + "' registered twice");
        }
        return created;
    }

    /** Loads the core component classes so their types are registered before any game is read from disk. */
    public static void registerCore() {
        // TODO: This method is weird and doesn't seem to actually do anything of meaning.
        //  I'm likely missing something but hey, it's just commented out for now :P
//        var core = List.of(
//                com.gaiagauntlet.gg.match.MatchStateComponent.TYPE,
//                com.gaiagauntlet.gg.match.ParticipantsComponent.TYPE,
//                com.gaiagauntlet.gg.lobby.LobbiesComponent.TYPE,
//                com.gaiagauntlet.gg.arena.ArenaComponent.TYPE,
//                com.gaiagauntlet.gg.transfer.TransferComponent.TYPE,
//                com.gaiagauntlet.gg.combat.CombatComponent.TYPE,
//                com.gaiagauntlet.gg.scoring.StandingsComponent.TYPE,
//                com.gaiagauntlet.gg.scoring.EventScoresComponent.TYPE,
//                OverridesComponent.TYPE);
//        if (core.size() != 9) {
//            throw new IllegalStateException("Core game components did not register");
//        }
    }

    @Nonnull
    public static Optional<GameComponentType<?>> find(@Nonnull String id) {
        return Optional.ofNullable(TYPES.get(id));
    }

    @Nonnull
    public static List<GameComponentType<?>> all() {
        return new ArrayList<>(TYPES.values());
    }
}
