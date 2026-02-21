package net.mexish.libs.command.test;

import lombok.val;
import net.mexish.libs.command.annotation.ArgumentSignature;
import net.mexish.libs.command.annotation.NamedCommand;
import net.mexish.libs.command.context.CommandContext;
import net.mexish.libs.command.context.impl.SimpleMutableCommandContext;
import net.mexish.libs.command.executor.CommandExecutor;
import net.mexish.libs.command.executor.SubcommandExecutor;
import net.mexish.libs.command.manager.impl.SimpleCommandManager;
import net.mexish.libs.command.test.type.manager.CommandManagerImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class CommandTests {

    private static final SimpleCommandManager commandManager = new CommandManagerImpl();
    private static volatile String lastResult = null;

    @BeforeAll
    public static void setup() throws IllegalAccessException {
        lastResult = null;
        commandManager.register(TestCommand.class);
    }

    @NamedCommand("test")
    public static class TestCommand {

        @CommandExecutor
        public static void main(CommandContext ctx) {
            lastResult = "main";
        }

        @SubcommandExecutor(value = "typed")
        @ArgumentSignature("int bool double string")
        public static void typedSub(final CommandContext ctx) {
            val i = ctx.getIntArgument(0);
            val b = ctx.getBooleanArgument(1);
            val d = ctx.getDoubleArgument(2);
            val s = ctx.getStringArgument(3);

            lastResult = i + "|" + b + "|" + d + "|" + s;
        }

        @SubcommandExecutor(value = "async", async = true)
        public static void asyncSub(CommandContext ctx) {
            try { Thread.sleep(50); } catch (Exception e) {}
            lastResult = "async_done";
        }
    }

    @Test
    public void testTypedArguments() throws IllegalAccessException {
        commandManager.register(TestCommand.class);

        // Execute: /test typed 42 true 3.14 hello
        commandManager.call("test typed 42 true 3.14 hello").join();

        Assertions.assertEquals("42|true|3.14|hello", lastResult);
    }

    @Test
    public void testStateIsolation() throws IllegalAccessException {
        commandManager.register(TestCommand.class);

        // Run two commands effectively back-to-back
        // Since we create NEW TypedArgument instances per call, this is safe.
        commandManager.call("test typed 1 true 1.0 one").join();
        String res1 = lastResult;

        commandManager.call("test typed 2 false 2.0 two").join();
        String res2 = lastResult;

        Assertions.assertEquals("1|true|1.0|one", res1);
        Assertions.assertEquals("2|false|2.0|two", res2);
    }

    @Test
    public void testAsync() throws IllegalAccessException {
        commandManager.register(TestCommand.class);

        long start = System.currentTimeMillis();
        CompletableFuture<Void> future = commandManager.call("test async");

        // If it were sync, it would have slept 50ms before returning here
        // Since it's async, future is returned almost instantly (usually <10ms)
        // Assertions.assertTrue(System.currentTimeMillis() - start < 40, "Should return future immediately");

        future.join();
        Assertions.assertEquals("async_done", lastResult);
    }

    @Test
    public void testTabCompletion() throws IllegalAccessException {
        commandManager.register(TestCommand.class);

        // 1. Subcommand completion
        // Context: "test" -> expect "typed", "async"
        Set<String> subOpts = commandManager.tabComplete(createContext("test", ""));
        Assertions.assertTrue(subOpts.contains("typed"));
        Assertions.assertTrue(subOpts.contains("async"));

        // 2. Boolean Argument completion
        // Context: "test" "typed" "1" "tr" -> expect "true"
        // (Args: int=1, bool=tr...)
        Set<String> boolOpts = commandManager.tabComplete(createContext("test", "typed", "1", "tr"));
        Assertions.assertTrue(boolOpts.contains("true"));
        Assertions.assertFalse(boolOpts.contains("false"));
    }

    // Simple helper to create a context wrapper for testing tab complete logic
    private CommandContext createContext(final String... args) {
        return new SimpleMutableCommandContext(
                null, null, args, null
        );
    }
}
