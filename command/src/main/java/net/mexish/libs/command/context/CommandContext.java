package net.mexish.libs.command.context;

import lombok.val;
import net.mexish.libs.command.argument.TypedArgument;
import net.mexish.libs.command.context.impl.SimpleMutableCommandContext;
import net.mexish.libs.command.context.impl.SimpleImmutableCommandContext;
import net.mexish.libs.command.user.CommandUser;
import net.mexish.libs.command.CommandExecutorWrapper;
import net.mexish.libs.command.exception.CommandArgumentException;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

/**
 * A command's context, could be immutable (read {@link SimpleImmutableCommandContext}),
 * rendering the setters and {@link #stripFirstArgument()} unusable
 * @author mexish
 */
@SuppressWarnings("all")
public interface CommandContext {

    CommandExecutorWrapper getWrapper();

    CommandUser getUser();

    String[] getArguments();

    void setTypedArgumentCache(final TypedArgument<?>[] cache);

    TypedArgument<?>[] getTypedArgumentCache();

    void setWrapper(final CommandExecutorWrapper wrapper);

    void setUser(final CommandUser user);

    void setArguments(final String[] arguments);

    String getArgument(final int idx);

    default int getIntArgument(final int idx) throws CommandArgumentException {
        final Integer val = getParsedArg(idx);

        if (val == null) {
            throw CommandArgumentException.of(getWrapper().getFullName(), "Argument at index " + idx + " is not a valid integer.");
        }

        return val;
    }

    default boolean getBooleanArgument(final int idx) throws CommandArgumentException {
        final Boolean val = getParsedArg(idx);

        if (val == null) {
            throw CommandArgumentException.of(getWrapper().getFullName(), "Argument at index " + idx + " is not a valid boolean.");
        }

        return val;
    }

    default double getDoubleArgument(final int idx) throws CommandArgumentException {
        final Double val = getParsedArg(idx);

        if (val == null) {
            throw CommandArgumentException.of(getWrapper().getFullName(), "Argument at index " + idx + " is not a valid double.");
        }

        return val;
    }

    default @Nullable String getStringArgument(final int idx) {
        return getParsedArg(idx);
    }

    default <T> T getParsedArg(int index) {
        val cache = getTypedArgumentCache();
        if (cache == null || index < 0 || index >= cache.length) {
            return null;
        }

        try {
            return (T) cache[index].get();
        } catch (final ClassCastException e) {
            return null;
        }
    }

    /*default */CommandContext stripFirstArgument(); /*{
        return new SimpleImmutableCommandContext(getWrapper(), getUser(), ArrayUtils.remove(getArguments(), 0));
    }*/

    default CommandContext immutable() {
        if (this instanceof SimpleImmutableCommandContext) {
            throw new IllegalStateException("This command context is already immutable, nothing to finalize!");
        }

        return ((SimpleMutableCommandContext) this).immutableInternal();
    }

}
