package net.mexish.libs.configuration.type.appendable;

import lombok.NonNull;
import lombok.val;
import net.mexish.libs.configuration.exception.ConfigurationException;
import net.mexish.libs.configuration.type.Configuration;

/**
 * @author mexish
 * @version 09/11/2025
 */
public interface AppendableConfiguration {

    /**
     * Appends a new {@link Configuration} (section/sector/whatever) to the current {@link Configuration}
     * @param name The name (key) of the new underlying {@link Configuration}
     * @param other The new underlying {@link Configuration}
     */
    default void append(final @NonNull String name, final @NonNull Configuration other) {
        val clazz = getClass();
        val otherClazz = other.getClass();

        if (!clazz.equals(otherClazz)) {
            throw ConfigurationException.withReason("Tried to append object of type " + otherClazz.getSimpleName() + " to " + clazz.getSimpleName() + ".");
        }

        appendInternal(name, other);
    }

    void appendInternal(final @NonNull String name, final @NonNull Configuration other);

}
