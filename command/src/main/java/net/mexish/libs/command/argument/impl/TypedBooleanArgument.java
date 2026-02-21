package net.mexish.libs.command.argument.impl;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import net.mexish.libs.command.argument.TypedArgument;
import net.mexish.libs.command.exception.CommandArgumentParseException;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
public final class TypedBooleanArgument implements TypedArgument<Boolean> {
    boolean value;

    @Override
    public Boolean get() {
        return value;
    }

    @Override
    public void set(final @NonNull Boolean value) {
        this.value = value;
    }

    @Override
    public @NotNull Boolean parse(final @NonNull String value) throws CommandArgumentParseException {
        if ("true".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value)) {
            set(true);
            return true;
        }

        if ("false".equalsIgnoreCase(value) || "off".equalsIgnoreCase(value)) {
            set(false);
            return false;
        }

        throw CommandArgumentParseException.of("unknown", "expected true/false/on/off");
    }

    @Override
    public Set<String> tabComplete(final @NonNull String input) {
        val token = input.toLowerCase();
        return Stream.of("true", "false")
                .filter(s -> s.startsWith(token))
                .collect(Collectors.toSet());
    }
}
