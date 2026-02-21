package net.mexish.libs.configuration.type;

import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author mexish
 * @version 09/11/2025
 */
public interface SerializableConfiguration extends Configuration {

    default Map<?, ?> serialize() {
        return toSerializableMap();
    }

    default Map<?, ?> serialize(final @NonNull Class<?> typeClazz) {
        preSerialization(typeClazz);
        return toSerializableMap();
    }

    /**
     * Turns the current {@link Configuration} object's map's underlying {@link Configuration} objects
     * into proper underlying maps for serialization
     * @return The map representation of this {@link Configuration}
     */
    @NotNull Map<?, ?> toSerializableMap();

    default void preSerialization(final @NonNull Class<?> typeClazz) {
        // no-op
    }

}
