package net.mexish.libs.modman.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import net.mexish.libs.modman.ModuleLifecycleEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * @author mexish
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public final class ModuleLifecycleHandlerException extends RuntimeException {

    /**
     * nullable
     */
    String reason;

    final ModuleLifecycleEvent event;

    public ModuleLifecycleHandlerException(final @NotNull String reason,
                                           final @NotNull ModuleLifecycleEvent event) {
        super(reason);
        this.reason = reason;
        this.event = event;
    }

    public ModuleLifecycleHandlerException(final @NotNull Throwable cause,
                                           final @NotNull ModuleLifecycleEvent event) {
        super(cause);
        this.event = event;
    }

    @Contract("_, _ -> new")
    public static @NotNull ModuleLifecycleHandlerException withReason(final @NonNull ModuleLifecycleEvent event,
                                                                      final @NonNull String reason) {
        return new ModuleLifecycleHandlerException(reason, event);
    }

    @Contract("_, _ -> new")
    public static @NotNull ModuleLifecycleHandlerException withCause(final @NonNull ModuleLifecycleEvent event,
                                                                     final @NonNull Throwable cause) {
        return new ModuleLifecycleHandlerException(cause, event);
    }

}
