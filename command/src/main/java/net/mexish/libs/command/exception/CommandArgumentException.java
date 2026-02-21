package net.mexish.libs.command.exception;

import net.mexish.libs.commons.util.Strings;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class CommandArgumentException extends CommandException {
    public CommandArgumentException(final String commandName, final String message) {
        super("Failed to process arguments for command `" + commandName + "`: " + message);
    }

    @Contract("_, _ -> new")
    public static @NotNull CommandArgumentException of(final String commandName, final String message) {
        return new CommandArgumentException(commandName, message);
    }

    @Contract("_, _, _ -> new")
    public static @NotNull CommandArgumentException of(final String commandName,
                                                       final String message,
                                                       final Object... args) {
        return of(commandName, Strings.fastFormatText(message, args));
    }

}
