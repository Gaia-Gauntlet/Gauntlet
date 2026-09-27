package gaiagauntlet.plugins.gamestore.config;

import javax.annotation.Nonnull;

/**
 * A kind of game a hub can run: the elimination match today, others later. A game is one instance
 * of a type. The type names what the instance is for; the features that make it play attach
 * themselves through components and events.
 */
public record GameType(@Nonnull String id, @Nonnull String name, @Nonnull String description) {
}
