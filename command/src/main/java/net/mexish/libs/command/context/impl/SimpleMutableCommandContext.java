package net.mexish.libs.command.context.impl;

import lombok.*;
import lombok.experimental.FieldDefaults;
import net.mexish.libs.command.argument.TypedArgument;
import net.mexish.libs.command.context.CommandContext;
import net.mexish.libs.command.user.CommandUser;
import net.mexish.libs.command.CommandExecutorWrapper;
import net.mexish.libs.command.exception.CommandArgumentException;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author mexish
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@Setter
@Getter
public class SimpleMutableCommandContext implements CommandContext {

    CommandExecutorWrapper wrapper;
    CommandUser user;
    String[] arguments;

    TypedArgument<?>[] typedArgumentCache;

    @Override
    public void setTypedArgumentCache(final @NonNull TypedArgument<?>[] cache) {
        typedArgumentCache = cache;
    }

    @Contract(pure = true)
    @Override
    public @Nullable String getArgument(final int idx) {
        if (idx >= arguments.length) {
            return null; // throw CommandArgumentException.of(wrapper.getFullName(), "Invalid argument position in context: " + idx);
        }

        return arguments[idx];
    }

    @Override
    public int getIntArgument(final int idx) throws CommandArgumentException {
        val arg = getArgument(idx);

        if (arg == null) {
            throw CommandArgumentException.of(wrapper.getFullName(), "Invalid int argument position in context: " + idx);
        }

        try {
            return Integer.parseInt(arg);
        } catch (final NumberFormatException e) {
            throw CommandArgumentException.of(wrapper.getFullName(), "Not an int argument at pos " + idx);
        }
    }

    @Contract(" -> new")
    @Override
    public @NotNull CommandContext stripFirstArgument() {
        arguments = ArrayUtils.remove(arguments, 0);
        return this;
    }

    @Contract(" -> new")
    @NotNull
    public SimpleImmutableCommandContext immutableInternal() {
        return new SimpleImmutableCommandContext(wrapper, user, arguments, typedArgumentCache);
    }

}
