package gaiagauntlet.plugins.scoring.components;

import gaiagauntlet.plugins.gamestore.components.GameComponent;
import gaiagauntlet.plugins.gamestore.components.GameComponentType;
import gaiagauntlet.plugins.gamestore.components.GameComponents;

import javax.annotation.Nonnull;
import java.util.List;

/** The final placements of the current or last match. */
public final class StandingsComponent implements GameComponent {

    public static final GameComponentType<StandingsComponent> TYPE = GameComponents.register(
            "Standings", StandingsComponent.class, StandingsComponent::new);

    /** A team's final placement and points. */
    public record Standing(int place, @Nonnull String teamId, @Nonnull String teamName, boolean winner, int points,
            long eliminatedAtMillis) {
    }

    private volatile List<Standing> standings = List.of();

    @Nonnull
    public List<Standing> standings() {
        return standings;
    }

    public void setStandings(@Nonnull List<Standing> standings) {
        this.standings = List.copyOf(standings);
    }

    @Override
    public void resetMatch() {
        standings = List.of();
    }
}
