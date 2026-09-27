package gaiagauntlet.plugins.gamestate.components;

import gaiagauntlet.plugins.gamestate.constants.MatchState;
import gaiagauntlet.plugins.gamestore.components.GameComponent;
import gaiagauntlet.plugins.gamestore.components.GameComponentType;
import gaiagauntlet.plugins.gamestore.components.GameComponents;

import javax.annotation.Nonnull;

/** Where a game is in its life cycle. Owned by the orchestrator. */
public final class MatchStateComponent implements GameComponent {

    public static final GameComponentType<MatchStateComponent> TYPE = GameComponents.register(
            "MatchState", MatchStateComponent.class, MatchStateComponent::new);

    private volatile MatchState state = MatchState.IDLE;

    @Nonnull
    public MatchState state() {
        return state;
    }

    public void setState(@Nonnull MatchState state) {
        this.state = state;
    }

    public boolean isLive() {
        return state == MatchState.ACTIVE || state == MatchState.SUDDEN_DEATH;
    }

    public boolean isInArena() {
        return switch (state) {
            case STAGING, ACTIVE, SUDDEN_DEATH, ENDED -> true;
            default -> false;
        };
    }
}
