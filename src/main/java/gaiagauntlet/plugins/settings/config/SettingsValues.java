package gaiagauntlet.plugins.settings.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The on-disk shape of the settings file. Its codec is built from the registered keys, so every
 * setting is a documented, validated field in the same format the rest of the server uses.
 */
public final class SettingsValues {

    private final Map<String, Object> values = new ConcurrentHashMap<>();

    public SettingsValues() {
    }

    @Nonnull
    public Map<String, Object> values() {
        return values;
    }

    /** One field per key, with the key's description as documentation and its range as validators. */
    @Nonnull
    public static BuilderCodec<SettingsValues> codecFor(@Nonnull List<SettingKey<?>> keys) {
        var builder = BuilderCodec.builder(SettingsValues.class, SettingsValues::new);
        for (var key : keys) {
            appendField(builder, key);
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static <T> void appendField(@Nonnull BuilderCodec.Builder<SettingsValues> builder, @Nonnull SettingKey<T> key) {
        var codec = (Codec<T>) codecFor(key.kind());
        var field = builder.append(new KeyedCodec<>(key.fileKey(), codec),
                (holder, value) -> {
                    if (value != null) {
                        holder.values.put(key.id(), key.cast(value));
                    }
                },
                holder -> (T) holder.values.getOrDefault(key.id(), key.defaultValue()))
                .documentation(key.description());
        if (key.min() != null && key.max() != null) {
            switch (key.kind()) {
                case INTEGER -> field
                        .addValidator((com.hypixel.hytale.codec.validation.Validator<? super T>) Validators.min(key.min().intValue()))
                        .addValidator((com.hypixel.hytale.codec.validation.Validator<? super T>) Validators.max(key.max().intValue()));
                case DECIMAL -> field
                        .addValidator((com.hypixel.hytale.codec.validation.Validator<? super T>) Validators.min(key.min()))
                        .addValidator((com.hypixel.hytale.codec.validation.Validator<? super T>) Validators.max(key.max()));
                default -> { }
            }
        }
        field.add();
    }

    @Nonnull
    private static Codec<?> codecFor(@Nonnull SettingKey.Kind kind) {
        return switch (kind) {
            case INTEGER -> Codec.INTEGER;
            case DECIMAL -> Codec.DOUBLE;
            case BOOLEAN -> Codec.BOOLEAN;
            case TEXT -> Codec.STRING;
        };
    }
}
