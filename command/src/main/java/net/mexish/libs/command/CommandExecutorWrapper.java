package net.mexish.libs.command;

import lombok.*;
import lombok.experimental.FieldDefaults;
import net.mexish.libs.command.argument.ArgumentManager;
import net.mexish.libs.command.context.CommandContext;
import net.mexish.libs.command.exception.CommandMethodCallException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString(exclude = {"parent", "subcommands"})
@Getter
@Setter
public final class CommandExecutorWrapper {

    String name = null;
    boolean async = false;

    // for subcommands
    CommandExecutorWrapper parent;
    final Map<String, CommandExecutorWrapper> subcommands = new LinkedHashMap<>();

    MethodHandle handle;

    public CompletableFuture<Void> call(final @NonNull ArgumentManager argumentManager,
                                        final @NonNull CommandContext ctx) {
        if (async) {
            return CompletableFuture.runAsync(() -> invokeInternal(argumentManager, ctx), ForkJoinPool.commonPool());
        } else {
            try {
                invokeInternal(argumentManager, ctx);
                return CompletableFuture.completedFuture(null);
            } catch (final Throwable t) {
                return CompletableFuture.failedFuture(t);
            }
        }
    }

    @SneakyThrows
    private void invokeInternal(final @NonNull ArgumentManager argumentManager,
                                final @NonNull CommandContext ctx) {
        try {
            //argumentManager.checkArguments(ctx);
            handle.invoke(ctx);
        } catch (Throwable t) {
            throw CommandMethodCallException.with(t);
        }
    }

    public @NotNull Set<String> tabComplete(final @NonNull ArgumentManager argManager,
                                            final @NonNull String @NotNull [] args) {
        if (args.length == 1) {
            val token = args[0].toLowerCase();
            val subMatches = subcommands.keySet().stream()
                    .filter(name -> name.toLowerCase().startsWith(token))
                    .collect(Collectors.toUnmodifiableSet());

            if (!subMatches.isEmpty()) {
                return subMatches;
            }
        }

        if (args.length > 0) {
            val sub = subcommands.get(args[0]);
            if (sub != null) {
                return sub.tabComplete(argManager, Arrays.copyOfRange(args, 1, args.length));
            }
        }

        return argManager.tabComplete(this.getFullName(), args);
    }

    public void addSubcommand(final @NonNull CommandExecutorWrapper child) {
        subcommands.put(child.getName(), child);
    }

    @Contract(pure = true)
    public @NotNull String getFullName() {
        if (parent == null) {
            return name;
        }

        return parent.getFullName() + "$" + name;
    }

}
