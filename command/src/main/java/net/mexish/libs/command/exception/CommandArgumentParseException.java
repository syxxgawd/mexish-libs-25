package net.mexish.libs.command.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import net.mexish.libs.command.argument.TypedArgument;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public final class CommandArgumentParseException extends CommandArgumentException {
    int idx;
    String argumentName;
    Class<? extends TypedArgument<?>> validType;

    public CommandArgumentParseException(final String commandName,
                                         final String message,
                                         final int idx,
                                         final String argumentName,
                                         final Class<? extends TypedArgument<?>> validType) {
        super(commandName, message);
        this.idx = idx;
        this.argumentName = argumentName;
        this.validType = validType;
    }

    @Contract("_, _, _, _, _ -> new")
    public static @NotNull CommandArgumentParseException of(final @NonNull String commandName,
                                                            final @NonNull String message,
                                                            final int idx,
                                                            final @NonNull String argumentName,
                                                            final @NonNull Class<? extends TypedArgument<?>> validType) {
        return new CommandArgumentParseException(commandName, message, idx, argumentName, validType);
    }

}
