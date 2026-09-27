package gaiagauntlet.plugins.settings.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Objects;

/**
 * One adjustable value. The key carries everything a command or a UI needs to show, parse, and
 * validate it, so adding a setting anywhere in the plugin is a single declaration.
 */
public final class SettingKey<T> {

    public enum Kind { INTEGER, DECIMAL, BOOLEAN, TEXT }

    private final String id;
    private final String group;
    private final String description;
    private final Kind kind;
    private final T defaultValue;
    @Nullable private final Double min;
    @Nullable private final Double max;

    private SettingKey(@Nonnull String id, @Nonnull String description, @Nonnull Kind kind, @Nonnull T defaultValue,
                       @Nullable Double min, @Nullable Double max) {
        this.id = id;
        this.group = id.contains(".") ? id.substring(0, id.indexOf('.')) : "general";
        this.description = description;
        this.kind = kind;
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
    }

    @Nonnull
    public static SettingKey<Integer> integer(@Nonnull String id, @Nonnull String description, int defaultValue,
            int min, int max) {
        return new SettingKey<>(id, description, Kind.INTEGER, defaultValue, (double) min, (double) max);
    }

    @Nonnull
    public static SettingKey<Double> decimal(@Nonnull String id, @Nonnull String description, double defaultValue,
            double min, double max) {
        return new SettingKey<>(id, description, Kind.DECIMAL, defaultValue, min, max);
    }

    @Nonnull
    public static SettingKey<Boolean> bool(@Nonnull String id, @Nonnull String description, boolean defaultValue) {
        return new SettingKey<>(id, description, Kind.BOOLEAN, defaultValue, null, null);
    }

    @Nonnull
    public static SettingKey<String> text(@Nonnull String id, @Nonnull String description, @Nonnull String defaultValue) {
        return new SettingKey<>(id, description, Kind.TEXT, defaultValue, null, null);
    }

    @Nonnull
    public String id() {
        return id;
    }

    @Nonnull
    public String group() {
        return group;
    }

    /** The key used in the settings file: the id in PascalCase without dots, as the server's codecs require. */
    @Nonnull
    public String fileKey() {
        var out = new StringBuilder();
        for (var part : id.split("\\.")) {
            if (!part.isEmpty()) {
                out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return out.toString();
    }

    @Nonnull
    public String description() {
        return description;
    }

    @Nonnull
    public Kind kind() {
        return kind;
    }

    @Nonnull
    public T defaultValue() {
        return defaultValue;
    }

    @Nullable
    public Double min() {
        return min;
    }

    @Nullable
    public Double max() {
        return max;
    }

    /** Parses user input into a value of this key's type. Throws with a human readable reason on bad input. */
    @Nonnull
    public T parse(@Nonnull String input) {
        var text = input.trim();
        Object value = switch (kind) {
            case INTEGER -> {
                try {
                    yield Integer.parseInt(text);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("'" + text + "' is not a whole number");
                }
            }
            case DECIMAL -> {
                try {
                    yield Double.parseDouble(text);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("'" + text + "' is not a number");
                }
            }
            case BOOLEAN -> switch (text.toLowerCase(Locale.ROOT)) {
                case "true", "on", "yes", "1" -> Boolean.TRUE;
                case "false", "off", "no", "0" -> Boolean.FALSE;
                default -> throw new IllegalArgumentException("'" + text + "' is not true or false");
            };
            case TEXT -> text;
        };
        return validate(cast(value));
    }

    /** Checks range constraints. Returns the value so callers can chain it. */
    @Nonnull
    public T validate(@Nonnull T value) {
        if (value instanceof Number number) {
            double d = number.doubleValue();
            if (min != null && d < min) {
                throw new IllegalArgumentException(id + " must be at least " + format(cast(min)));
            }
            if (max != null && d > max) {
                throw new IllegalArgumentException(id + " must be at most " + format(cast(max)));
            }
        }
        return value;
    }

    @Nonnull
    public String format(@Nonnull T value) {
        if (kind == Kind.DECIMAL) {
            double d = ((Number) value).doubleValue();
            return d == Math.rint(d) ? Long.toString((long) d) : Double.toString(d);
        }
        if (kind == Kind.INTEGER) {
            return Integer.toString(((Number) value).intValue());
        }
        return String.valueOf(value);
    }

    /** Human readable range, or empty when the key is unbounded. */
    @Nonnull
    public String rangeText() {
        if (min == null && max == null) {
            return "";
        }
        return format(cast(min)) + " to " + format(cast(max));
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    T cast(@Nonnull Object value) {
        return switch (kind) {
            case INTEGER -> (T) Integer.valueOf(((Number) value).intValue());
            case DECIMAL -> (T) Double.valueOf(((Number) value).doubleValue());
            case BOOLEAN -> (T) Boolean.valueOf((Boolean) value);
            case TEXT -> (T) String.valueOf(value);
        };
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SettingKey<?> other && other.id.equals(id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id;
    }
}
