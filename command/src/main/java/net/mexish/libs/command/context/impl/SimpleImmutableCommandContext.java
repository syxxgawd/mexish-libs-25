package net.mexish.libs.command.context.impl;

import lombok.*;
import lombok.experimental.FieldDefaults;
import net.mexish.libs.command.CommandExecutorWrapper;
import net.mexish.libs.command.argument.TypedArgument;
import net.mexish.libs.command.context.CommandContext;
import net.mexish.libs.command.exception.CommandArgumentException;
import net.mexish.libs.command.user.CommandUser;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * An immutable instance of a command context,
 * can be used in situations where it is guaranteed that the command context
 * no longer requires to be modified, however {@link #stripFirstArgument()}
 * is an exception where it instances a new {@link SimpleImmutableCommandContext} upon call,
 * this method is not recommended for use as it is very slow
 * (obviously instancing a new object is a heavier task than modifying an array).
 * @author mexish
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public class SimpleImmutableCommandContext implements CommandContext {

    CommandExecutorWrapper wrapper;
    CommandUser user;
    String[] arguments;
    TypedArgument<?>[] typedArgumentCache;

    @Contract(pure = true)
    @Override
    public @Nullable String getArgument(final int idx) {
        if (idx >= arguments.length) {
            return null; // throw CommandArgumentException.of(wrapper.getFullName(), "Invalid argument position in context: " + idx);
        }

        return arguments[idx];
    }

    @Override
    public CommandContext stripFirstArgument() {
        return null;
    }

    // unsupported

    @Override
    public void setTypedArgumentCache(final @NonNull TypedArgument<?>[] cache) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setWrapper(CommandExecutorWrapper wrapper) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUser(CommandUser user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setArguments(String[] arguments) {
        throw new UnsupportedOperationException();
    }

}
