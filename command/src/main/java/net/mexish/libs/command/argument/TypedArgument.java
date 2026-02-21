package net.mexish.libs.command.argument;

import net.mexish.libs.command.exception.CommandArgumentParseException;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

public interface TypedArgument<T> {

    T get();

    void set(T value);

    T parse(String value) throws CommandArgumentParseException;

    /**
     * @param input The partial string typed so far.
     * @return Set of suggestions.
     */
    default Set<String> tabComplete(final @NotNull String input) {
        return Collections.emptySet();
    }

}
