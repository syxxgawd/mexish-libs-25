package net.mexish.libs.command.exception;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class CommandMethodCallException extends CommandException {
    public CommandMethodCallException(final Throwable t) {
        super("Failed to call command's MethodHandle", t);
    }

    @Contract("_ -> new")
    public static @NotNull CommandMethodCallException with(final Throwable t) {
        return new CommandMethodCallException(t);
    }
}
