package net.mexish.libs.command.exception;

import lombok.NonNull;
import net.mexish.libs.commons.util.Strings;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class CommandCallException extends CommandException {
    public CommandCallException(final String message) {
        super("Command call exception: " + message);
    }

    public CommandCallException(final CommandException e) {
        super(e);
    }

    @Contract("_ -> new")
    public static @NotNull CommandCallException of(final @NonNull String message) {
        return new CommandCallException(message);
    }

    @Contract("_, _ -> new")
    public static @NotNull CommandCallException of(final @NonNull String message, final Object... args) {
        return of(Strings.fastFormatText(message, args));
    }

    @Contract("_ -> new")
    public static @NotNull CommandCallException with(final CommandException e) {
        return new CommandCallException(e);
    }
}
