package net.mexish.libs.command.manager;

import net.mexish.libs.command.argument.ArgumentManager;
import net.mexish.libs.command.context.CommandContext;
import net.mexish.libs.command.CommandExecutorWrapper;
import net.mexish.libs.command.context.impl.SimpleMutableCommandContext;
import net.mexish.libs.command.exception.CommandCallException;
import net.mexish.libs.command.exception.CommandRegistrationException;
import net.mexish.libs.command.manager.impl.SimpleCommandManager;
import net.mexish.libs.commons.util.IndexMap;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * The main command manager interface that all underlying command managers MUST implement,
 * certain method implementations are overloaded and wrapped by {@link SimpleCommandManager}
 * @author mexish
 */
public interface CommandManager {

    /**
     * Turns a command class into a wrapper with a method handle and registers it
     * @param command the command class
     */
    void register(final Class<?> command)
            throws IllegalAccessException, CommandRegistrationException;

    void store(final CommandExecutorWrapper wrapper);

    default void onStoreWrapper(final CommandExecutorWrapper wrapper,
                                final Class<?> clazz) {
        // noop
    }

    default void onStoreCommandMethod(final Method method) {
        // noop
    }

    //void store(final CommandExecutorWrapper wrapper, final Class<?> commandClazz);

    void remove(final CommandExecutorWrapper wrapper);

    CommandExecutorWrapper getCommand(final String name);

    IndexMap<CommandExecutorWrapper> commands();

    default CompletableFuture<Void> call(final String command) {
        if (command == null || command.isEmpty()) {
            return CompletableFuture.failedFuture(CommandCallException.of("Empty command"));
        }

        return call(new SimpleMutableCommandContext(null, null, command.split(" "), null));
    }

    CompletableFuture<Void> call(final CommandContext ctx);

    Set<String> tabComplete(final @NotNull CommandContext ctx);

    ArgumentManager getArgumentManager();

}
