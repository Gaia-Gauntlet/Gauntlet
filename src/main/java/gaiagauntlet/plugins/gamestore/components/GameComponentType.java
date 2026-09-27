package gaiagauntlet.plugins.gamestore.components;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import gaiagauntlet.plugins.gamestore.game.Game;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Identifies one kind of component on a game. Components with a codec are saved with the game and
 * restored on boot; the rest are rebuilt from a fresh instance.
 */
public final class GameComponentType<T extends GameComponent> {

    private final String id;
    private final Class<T> type;
    private final Supplier<T> factory;
    @Nullable private final BuilderCodec<T> codec;

    GameComponentType(@Nonnull String id, @Nonnull Class<T> type, @Nonnull Supplier<T> factory, @Nullable BuilderCodec<T> codec) {
        this.id = id;
        this.type = type;
        this.factory = factory;
        this.codec = codec;
    }

    @Nonnull
    public String id() {
        return id;
    }

    @Nonnull
    public Class<T> type() {
        return type;
    }

    @Nonnull
    public T create() {
        return factory.get();
    }

    @Nullable
    public BuilderCodec<T> codec() {
        return codec;
    }

    public boolean isPersistent() {
        return codec != null;
    }

    /** The component on the game, created on first use. */
    @Nonnull
    public T of(@Nonnull Game game) {
        return game.ensure(this);
    }

    @Override
    public String toString() {
        return id;
    }
}
