package net.mexish.libs.command.test;

import net.mexish.libs.command.exception.CommandException;
import net.mexish.libs.command.exception.CommandRegistrationException;
import net.mexish.libs.command.manager.CommandManager;
import net.mexish.libs.command.test.type.command.NiggerCommand;
import net.mexish.libs.command.test.type.manager.CommandManagerImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class CommandTest {

    private static final CommandManager manager = new CommandManagerImpl();

    @BeforeAll
    public static void registerTestCommand() throws CommandRegistrationException, IllegalAccessException {
        manager.register(NiggerCommand.class);
    }

    @Test
    public void testRegistrationParent() {
        assertNotNull(manager.getCommand("niggerparent"));
    }

    @Test
    public void testRegistrationSub() {
        assertNotNull(manager.getCommand("niggerparent$niggersubtest1"));
    }

    @Test
    public void callTestParent() throws CommandException {
        manager.call("niggerparent");
    }

    @Test
    public void callTestSub() throws CommandException {
        manager.call("niggerparent niggersubtest1");
    }

}
