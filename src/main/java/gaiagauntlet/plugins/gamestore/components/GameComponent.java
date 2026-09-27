package gaiagauntlet.plugins.gamestore.components;

import gaiagauntlet.plugins.gamestore.game.Game;

/**
 * Data a feature keeps on a {@link Game}. Features register a {@link GameComponentType} for their
 * component and read it back with {@code type.of(game)}. The core never needs to know it exists.
 */
public interface GameComponent {

    /** Called when the game returns to IDLE. Clear anything that belonged to the match that just ended. */
    default void resetMatch() {
    }
}
