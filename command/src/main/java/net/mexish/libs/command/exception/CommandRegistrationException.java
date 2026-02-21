package net.mexish.libs.command.exception;

import lombok.NonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class CommandRegistrationException extends CommandException {
    public CommandRegistrationException(final @NonNull Class<?> clazz, final String reason) {
        super("Failed to register command: `" + clazz.getSimpleName() + "`" + reason);
    }

    @Contract("_, _ -> new")
    public static @NotNull CommandRegistrationException withReason(final @NonNull Class<?> clazz, final String reason) {
        return new CommandRegistrationException(clazz, reason);
    }
}
