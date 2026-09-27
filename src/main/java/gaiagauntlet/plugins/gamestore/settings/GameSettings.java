package gaiagauntlet.plugins.gamestore.settings;

import gaiagauntlet.plugins.gamestore.components.OverridesComponent;
import gaiagauntlet.plugins.gamestore.game.Game;
import gaiagauntlet.plugins.gamestore.store.GlobalStore;
import gaiagauntlet.plugins.settings.config.SettingKey;
import gaiagauntlet.plugins.settings.constants.Settings;

import javax.annotation.Nonnull;

/** The effective settings of one game: its overrides on top of the global registry. */
public final class GameSettings {

    private final Game game;

    public GameSettings(@Nonnull Game game) {
        this.game = game;
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    public <T> T get(@Nonnull SettingKey<T> key) {
        var override = OverridesComponent.TYPE.of(game).settingOverrides().get(key.id());
        return override == null ? Settings.get().get(key) : (T) override;
    }

    public int seconds(@Nonnull SettingKey<Double> key) {
        return (int) Math.round(get(key));
    }

    public boolean isOverridden(@Nonnull SettingKey<?> key) {
        return OverridesComponent.TYPE.of(game).settingOverrides().containsKey(key.id());
    }

    /** Parses and stores an override for this game only. */
    @Nonnull
    public <T> String override(@Nonnull SettingKey<T> key, @Nonnull String input) {
        var value = key.parse(input);
        OverridesComponent.TYPE.of(game).settingOverrides().put(key.id(), value);
        GlobalStore.get().saveGames();
        return key.format(value);
    }

    public void clearOverride(@Nonnull SettingKey<?> key) {
        OverridesComponent.TYPE.of(game).settingOverrides().remove(key.id());
        GlobalStore.get().saveGames();
    }

    @Nonnull
    public <T> String format(@Nonnull SettingKey<T> key) {
        return key.format(get(key));
    }
}
