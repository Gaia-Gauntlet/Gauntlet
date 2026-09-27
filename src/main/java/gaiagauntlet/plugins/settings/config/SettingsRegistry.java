package gaiagauntlet.plugins.settings.config;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.util.Config;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * Holds the live value of every {@link SettingKey}. Reads are lock free and safe from any thread.
 * Values persist through the server's own {@link Config} and a codec generated from the keys, so
 * every successful write is saved immediately and announced to listeners.
 */
public final class SettingsRegistry {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Map<String, SettingKey<?>> keys = Collections.synchronizedMap(new LinkedHashMap<>());
    private final List<BiConsumer<SettingKey<?>, Object>> listeners = new CopyOnWriteArrayList<>();
    private Config<SettingsValues> config;
    private SettingsValues values = new SettingsValues();

    public <T> SettingKey<T> register(@Nonnull SettingKey<T> key) {
        if (keys.putIfAbsent(key.id(), key) != null) {
            throw new IllegalStateException("Setting " + key.id() + " registered twice");
        }
        values.values().put(key.id(), key.defaultValue());
        return key;
    }

    /** The codec for the settings file, built from the registered keys. */
    @Nonnull
    public BuilderCodec<SettingsValues> codec() {
        return SettingsValues.codecFor(keys());
    }

    /** Loads saved values through the given config, which must have been created with {@link #codec()}. */
    public void load(@Nonnull Config<SettingsValues> settingsConfig) {
        config = settingsConfig;
        var loaded = config.load().join();
        for (var key : keys()) {
            loaded.values().putIfAbsent(key.id(), key.defaultValue());
        }
        values = loaded;
        save();
    }

    @Nonnull
    public List<SettingKey<?>> keys() {
        synchronized (keys) {
            return new ArrayList<>(keys.values());
        }
    }

    @Nonnull
    public Optional<SettingKey<?>> find(@Nonnull String id) {
        return Optional.ofNullable(keys.get(id));
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    public <T> T get(@Nonnull SettingKey<T> key) {
        return (T) values.values().getOrDefault(key.id(), key.defaultValue());
    }

    /** Validates, stores, saves, and notifies. Throws IllegalArgumentException for out of range values. */
    public <T> void set(@Nonnull SettingKey<T> key, @Nonnull T value) {
        var validated = key.validate(value);
        values.values().put(key.id(), validated);
        save();
        for (var listener : listeners) {
            listener.accept(key, validated);
        }
    }

    /** Parses and stores text input for the key with the given id. Returns the stored value formatted. */
    @Nonnull
    public String setFromText(@Nonnull String id, @Nonnull String input) {
        var key = find(id).orElseThrow(() -> new IllegalArgumentException("Unknown setting '" + id + "'"));
        return setFromText(key, input);
    }

    @Nonnull
    public <T> String setFromText(@Nonnull SettingKey<T> key, @Nonnull String input) {
        var value = key.parse(input);
        set(key, value);
        return key.format(value);
    }

    public <T> void reset(@Nonnull SettingKey<T> key) {
        set(key, key.defaultValue());
    }

    @Nonnull
    public <T> String format(@Nonnull SettingKey<T> key) {
        return key.format(get(key));
    }

    public void addListener(@Nonnull BiConsumer<SettingKey<?>, Object> listener) {
        listeners.add(listener);
    }

    public void save() {
        if (config == null) {
            return;
        }
        config.save().exceptionally(e -> {
            LOGGER.atSevere().withCause(e).log("Could not save settings");
            return null;
        });
    }
}
