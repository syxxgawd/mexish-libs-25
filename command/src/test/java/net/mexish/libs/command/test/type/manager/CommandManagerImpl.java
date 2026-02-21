package net.mexish.libs.command.test.type.manager;

import net.mexish.libs.command.CommandExecutorWrapper;
import net.mexish.libs.command.manager.impl.SimpleCommandManager;
import net.mexish.libs.commons.util.IndexMap;

public final class CommandManagerImpl extends SimpleCommandManager {
    private final IndexMap<CommandExecutorWrapper> commands = IndexMap.<CommandExecutorWrapper>newMap()
            .registerIndex(String.class, CommandExecutorWrapper::getFullName);

    @Override
    public void store(final CommandExecutorWrapper wrapper) {
        commands.store(wrapper);
    }

    @Override
    public void remove(CommandExecutorWrapper wrapper) {
        commands.remove(wrapper);
    }

    @Override
    public CommandExecutorWrapper getCommand(final String name) {
        return commands.get(String.class, name);
    }

    @Override
    public IndexMap<CommandExecutorWrapper> commands() {
        return commands;
    }
}
