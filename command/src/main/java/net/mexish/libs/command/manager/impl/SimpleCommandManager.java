package net.mexish.libs.command.manager.impl;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import net.mexish.libs.command.*;
import net.mexish.libs.command.annotation.NamedCommand;
import net.mexish.libs.command.annotation.NamedSubcommand;
import net.mexish.libs.command.argument.ArgumentManager;
import net.mexish.libs.command.context.CommandContext;
import net.mexish.libs.command.exception.CommandCallException;
import net.mexish.libs.command.exception.CommandRegistrationException;
import net.mexish.libs.command.executor.CommandExecutor;
import net.mexish.libs.command.executor.SubcommandExecutor;
import net.mexish.libs.command.manager.CommandManager;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author mexish
 */
// TODO instance support along with statics (along with delegation of wrappers?)
//  if not delegation then objectweb asm bytecode fuckery like in MetafactoryBuilder from core-internals

// TODO support for strong typing of arguments with custom parsers

// TODO tabcompletion

// TODO command executor relays that also depend on argument signatures

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SuppressWarnings("all")
@Getter
public abstract class SimpleCommandManager implements CommandManager {
    ArgumentManager argumentManager = new ArgumentManager();

    @Override
    public void register(final @NonNull Class<?> command)
            throws IllegalAccessException, CommandRegistrationException {

        val namedCommand = command.getDeclaredAnnotation(NamedCommand.class);
        val lookup = MethodHandles.lookup();

        if (namedCommand == null) {
            throw CommandRegistrationException.withReason(command, "NamedCommand annotation not found!");
        }

        val name = namedCommand.value();

        if (name.isEmpty()) {
            throw CommandRegistrationException.withReason(command, "Command name cannot be empty!");
        }

        registerRecursive(name, command, lookup);
    }

    protected void registerRecursive(final @NonNull String name,
                                     final @NonNull Class<?> command,
                                     final @NonNull MethodHandles.Lookup lookup)
            throws IllegalAccessException, CommandRegistrationException {

        val wrapper = new CommandExecutorWrapper();
        wrapper.setName(name);

        for (val method : command.getMethods()) {
            val sub = checkSubExecutor(name, command, lookup, method);

            if (sub == null) {
                val execAnnotation = method.getDeclaredAnnotation(CommandExecutor.class);

                if (execAnnotation == null) {
                    continue;
                }

                if (wrapper.getHandle() != null) {
                    throw CommandRegistrationException.withReason(command, "Command cannot have multiple executors!");
                }

                wrapper.setHandle(lookup.unreflect(method));
                wrapper.setAsync(execAnnotation.async());
                argumentManager.bakeArgumentSignature(wrapper.getFullName(), method);
                continue;
            }

            sub.setParent(wrapper);
            wrapper.addSubcommand(sub);

            argumentManager.bakeArgumentSignature(sub.getFullName(), method);
            store(sub);
            onStoreCommandMethod(method);
        }

        checkForSubcommands(wrapper, command, lookup);
        store(wrapper);
        onStoreWrapper(wrapper, command);
    }

    protected void registerSubcommandRecursive(final @NonNull String name,
                                               final @NonNull Class<?> command,
                                               final @NonNull CommandExecutorWrapper parent,
                                               final @NonNull MethodHandles.Lookup lookup)
            throws CommandRegistrationException, IllegalAccessException {

        val wrapper = new CommandExecutorWrapper();

        wrapper.setName(name);
        wrapper.setParent(parent);

        parent.addSubcommand(wrapper);

        val methods = command.getMethods();

        for (val method : methods) {
            val execAnnotation = method.getDeclaredAnnotation(CommandExecutor.class);

            if (execAnnotation == null) {
                continue;
            }

            if (wrapper.getHandle() != null) {
                throw CommandRegistrationException.withReason(command, "Command cannot have multiple executors!");
            }

            argumentManager.bakeArgumentSignature(wrapper.getFullName(), method);

            wrapper.setHandle(lookup.unreflect(method));
            wrapper.setAsync(execAnnotation.async());
            onStoreCommandMethod(method);
        }

        for (val method : methods) {
            val sub = checkSubExecutor(name, command, lookup, method);

            if (sub == null) {
                continue;
            }

            sub.setParent(wrapper);
            wrapper.addSubcommand(sub);

            argumentManager.bakeArgumentSignature(sub.getFullName(), method);

            store(sub);
            onStoreCommandMethod(method);
        }

        checkForSubcommands(wrapper, command, lookup);
        store(wrapper);
        onStoreWrapper(wrapper, command);
    }

    protected void checkForSubcommands(final @NonNull CommandExecutorWrapper parent,
                                       final @NonNull Class<?> clazz,
                                       final @NonNull MethodHandles.Lookup lookup)
            throws CommandRegistrationException, IllegalAccessException {

        val clazzes = clazz.getDeclaredClasses();

        for (val subcommand : clazzes) {
            val namedSubcommand = subcommand.getDeclaredAnnotation(NamedSubcommand.class);

            if (namedSubcommand == null) {
                continue;
            }

            val subcommandName = namedSubcommand.value();

            if (subcommandName.isEmpty()) {
                throw CommandRegistrationException.withReason(subcommand, "Subcommand name cannot be empty!");
            }

            registerSubcommandRecursive(subcommandName, subcommand, parent, lookup);
        }
    }

    protected @Nullable CommandExecutorWrapper checkSubExecutor(final @NonNull String parentName,
                                                                final @NonNull Class<?> clazz,
                                                                final @NonNull MethodHandles.Lookup lookup,
                                                                final @NonNull Method method)
            throws CommandRegistrationException, IllegalAccessException {

        val subcommandExecutorAnnotation = method.getDeclaredAnnotation(SubcommandExecutor.class);
        val sub = new CommandExecutorWrapper();

        if (subcommandExecutorAnnotation == null) {
            return null;
        }

        val name = subcommandExecutorAnnotation.value();

        if (name.isEmpty()) {
            throw CommandRegistrationException.withReason(clazz, "Command name cannot be empty!");
        }

        sub.setName(name);
        sub.setHandle(lookup.unreflect(method));
        sub.setAsync(subcommandExecutorAnnotation.async());

        return sub;
    }

    @Override
    public Set<String> tabComplete(final @NonNull CommandContext ctx) {
        val args = ctx.getArguments();
        if (args.length == 0) return Collections.emptySet();

        val rootName = args[0];

        if (args.length == 1) {
            val token = rootName.toLowerCase();
            return commands().values().stream()
                    .map(CommandExecutorWrapper::getFullName)
                    .filter(name -> !name.contains("$"))
                    .filter(name -> name.toLowerCase().startsWith(token))
                    .collect(Collectors.toSet());
        }

        val wrapper = getCommand(rootName);
        if (wrapper == null) return Collections.emptySet();

        var subCtx = ctx.stripFirstArgument();
        var currentWrapper = wrapper;

        while (true) {
            val subArgs = subCtx.getArguments();
            if (subArgs.length == 0) break;

            val potentialSub = subArgs[0];
            val child = currentWrapper.getSubcommands().get(potentialSub);

            if (child != null) {
                currentWrapper = child;
                subCtx = subCtx.stripFirstArgument();
            } else {
                break;
            }
        }

        return currentWrapper.tabComplete(argumentManager, subCtx.getArguments());
    }

//    @Override
//    public void call(@NonNull CommandContext ctx)
//            throws CommandCallException, CommandArgumentException {
//
//        val originalWrapper = ctx.getWrapper(); // could either be a subcommand wrapper, or null
//
//        if (originalWrapper == null) { // parent command
//            val command = ctx.getArgument(0);
//
//            if (command == null) {
//                throw CommandCallException.of("Empty command string! This shouldn't happen!");
//            }
//
//            val wrapper = getCommand(command);
//
//            if (wrapper == null) {
//                throw CommandCallException.of("Invalid command `{}`!", command);
//            }
//
//            // wrapper non null for first command, proceed like normal
//
//            val subName = ctx.getArgument(1); // next arg could be subcommand
//
//            if (subName != null) {
//                val subFullname = command + "$" + subName;
//                val sub = getCommand(subFullname);
//
//                ctx.setWrapper(sub);
//
//                if (sub != null) {
//                    call(ctx.stripFirstArgument()); // strip parent command name
//                    return;
//                }
//            }
//
//            ctx.setWrapper(wrapper);
//            ctx = ctx.stripFirstArgument();
//
//            val immutableCtx = ctx.immutable();
//
//            // no subcommand next, so proceed with normal parent command call
//
//            try {
//                wrapper.call(argumentManager, immutableCtx);
//            } catch (final CommandException e) {
//                throw CommandCallException.with(e);
//            }
//
//            return;
//        }
//
//        // subcommands
//        val curSubCtx = ctx.stripFirstArgument(); // strip the current subcommand name
//        val subName = curSubCtx.getArgument(0); // next arg could be another subcommand
//
//        if (subName != null) {
//            val subFullname = originalWrapper.getFullName() + "$" + subName;
//            val sub = getCommand(subFullname);
//
//            ctx.setWrapper(sub);
//
//            if (sub != null) {
//                call(ctx.stripFirstArgument()); // strip previous subcommand name
//                return;
//            }
//        }
//
//        try {
//            originalWrapper.call(argumentManager, curSubCtx);
//        } catch (final CommandException e) {
//            throw CommandCallException.with(e);
//        }
//    }

    @Override
    public CompletableFuture<Void> call(final @NonNull CommandContext ctx) {
        try {
            return resolveAndExecute(ctx);
        } catch (final Throwable t) {
            return CompletableFuture.failedFuture(t);
        }
    }

    private CompletableFuture<Void> resolveAndExecute(final @NonNull CommandContext ctx) {
        val originalWrapper = ctx.getWrapper();

        if (originalWrapper == null) {
            val commandArg = ctx.getArgument(0);

            if (commandArg == null) {
                throw CommandCallException.of("Empty command string!");
            }

            val wrapper = getCommand(commandArg);

            if (wrapper == null) {
                throw CommandCallException.of("Unknown command `{}`", commandArg);
            }

            val nextArg = ctx.getArgument(1);

            if (nextArg != null) {
                val sub = wrapper.getSubcommands().get(nextArg);
                if (sub != null) {
                    ctx.setWrapper(sub);
                    return resolveAndExecute(ctx.stripFirstArgument());
                }
            }

            ctx.setWrapper(wrapper);
            return execute(wrapper, ctx.stripFirstArgument());
        }

        val curCtx = ctx.stripFirstArgument();
        val possibleSubName = curCtx.getArgument(0);

        if (possibleSubName != null) {
            val sub = originalWrapper.getSubcommands().get(possibleSubName);

            if (sub != null) {
                ctx.setWrapper(sub);
                return resolveAndExecute(ctx.stripFirstArgument());
            }
        }

        return execute(originalWrapper, curCtx);
    }

    private CompletableFuture<Void> execute(final @NonNull CommandExecutorWrapper wrapper,
                                            final @NonNull CommandContext ctx) {
        argumentManager.checkArguments(ctx);
        return wrapper.call(argumentManager, ctx.immutable());
    }

}
