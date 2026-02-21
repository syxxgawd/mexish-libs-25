package net.mexish.libs.configuration.exception;

import lombok.NonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * @author mexish
 */
public final class LoaderException extends RuntimeException {

    public LoaderException(final @NonNull Throwable t) {
        super(t);
    }

    public LoaderException(final @NonNull String reason) {
        super(reason);
    }

    @Contract("_ -> new")
    public static @NotNull LoaderException with(final @NonNull Throwable t) {
        return new LoaderException(t);
    }

    @Contract("_ -> new")
    public static @NotNull LoaderException withReason(final @NonNull String reason) {
        return new LoaderException(reason);
    }

}
