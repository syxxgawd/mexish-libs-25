package net.mexish.libs.configuration.type;

import lombok.NonNull;
import lombok.val;
import net.mexish.libs.configuration.type.appendable.AppendableConfiguration;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author mexish
 * @version 10/11/2025
 */
@SuppressWarnings("all")
public interface Configuration extends AppendableConfiguration {

    /**
     * Get an underlying configuration from the map that's accessible in the current provider
     * @return the underlying configuration or {@code null} if it doesn't exist
     */
    @Nullable
    default <T extends Configuration> T getConfiguration(final @NonNull String name) {
        val obj = get(name);

        if (!(obj instanceof Configuration)) {
            return null;
        }

        return (T) obj;
    }

    @Nullable
    default <T extends Configuration> T getConfiguration(final @NonNull String name,
                                                         final @NonNull Class<T> configurationClazz) {
        val obj = get(name);

        if (!obj.getClass().isInstance(configurationClazz)) {
            return null;
        }

        return (T) obj;
    }

    @Nullable <T extends Configuration> T getParent();

    <T extends Configuration> void setParent(final @NonNull T parent);

    //getters
    @Nullable Object get(final @NonNull String name);

    default boolean isPresent(final @NonNull String name) {
        return get(name) != null;
    }

    default int getInt(final @NonNull String name) {
        return Integer.parseInt(getString(name));
    }

    default String getString(final @NonNull String name) {
        val obj = get(name);

        if (obj == null) {
            return null;
        }

        return String.valueOf(obj);
    }

    default String getOrDefault(final @NonNull String name, final @NonNull String def) {
        val obj = getString(name);
        return obj == null ? def : obj;
    }

    default long getLong(final @NonNull String name) {
        return Long.parseLong(getString(name));
    }

    default boolean getBoolean(final @NonNull String name) {
        return Boolean.parseBoolean(getString(name));
    }

    default double getDouble(final @NonNull String name) {
        return Double.parseDouble(getString(name));
    }

    default <T> List<T> getGenericList(final @NonNull String name) {
        val obj = get(name);

        if (obj == null) {
            return null;
        }

        return (List<T>) obj;
    }

    default List<?> getOrDefault(final @NonNull String name, final @NonNull List<?> def) {
        val obj = getGenericList(name);
        return obj == null ? def : obj;
    }

    default List<String> getStringList(final @NonNull String name) {
        val obj = get(name);

        if (obj == null) {
            return null;
        }

        return (List<String>) obj;
    }

    default <T> T getTyped(final @NonNull String name) {
        val obj = get(name);

        if (obj == null) {
            return null;
        }

        return (T) obj;
    }

    default <T> T getOrDefault(final @NonNull String name, final @NonNull T def) {
        val obj = get(name);
        return obj == null ? def : (T) obj;
    }
    // getters end

    // setters
    void set(final @NonNull String name, final @Nullable Object value);

    default void setInt(final @NonNull String name, final int value) {
        set(name, value);
    }

    default void setString(final @NonNull String name, final String value) {
        set(name, value == null ? "" : value);
    }

    default void setLong(final @NonNull String name, final long value) {
        set(name, value);
    }

    default void setBoolean(final @NonNull String name, final boolean value) {
        set(name, value);
    }

    default void setDouble(final @NonNull String name, final double value) {
        set(name, value);
    }

    default void setGenericList(final @NonNull String name, final @NonNull List<?> value) {
        set(name, value);
    }

    default void setStringList(final @NonNull String name, final @NonNull List<String> value) {
        set(name, value);
    }
    // setters end

}
