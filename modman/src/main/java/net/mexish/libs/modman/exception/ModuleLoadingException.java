package net.mexish.libs.modman.exception;

import lombok.NonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * @author mexish
 */
public final class ModuleLoadingException extends RuntimeException {

    public ModuleLoadingException(final @NotNull String message) {
        super(message);
    }

    public ModuleLoadingException(final @NotNull Throwable cause) {
        super(cause);
    }

    public ModuleLoadingException(final @NotNull String message,
                                  final @NotNull Throwable cause) {
        super(message, cause);
    }

    @Contract("_ -> new")
    public static @NotNull ModuleLoadingException withReason(final @NonNull String message) {
        return new ModuleLoadingException(message);
    }

    @Contract("_ -> new")
    public static @NotNull ModuleLoadingException withCause(final @NonNull Throwable cause) {
        return new ModuleLoadingException(cause);
    }

    @Contract("_, _ -> new")
    public static @NotNull ModuleLoadingException withCause(final @NonNull String message,
                                                            final @NonNull Throwable cause) {
        return new ModuleLoadingException(message, cause);
    }

}
