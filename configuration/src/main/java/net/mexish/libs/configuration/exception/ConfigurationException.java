package net.mexish.libs.configuration.exception;

import lombok.NonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * @author mexish
 */
public final class ConfigurationException extends RuntimeException {

    public ConfigurationException(final @NonNull String reason) {
        super("Configuration exception: " + reason);
    }

    public ConfigurationException(final @NonNull Throwable t) {
        super(t);
    }

    @Contract("_ -> new")
    public static @NotNull ConfigurationException with(final @NonNull Throwable t) {
        return new ConfigurationException(t);
    }

    @Contract("_ -> new")
    public static @NotNull ConfigurationException withReason(final @NonNull String reason) {
        return new ConfigurationException(reason);
    }

}
