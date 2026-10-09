package com.gaiagauntlet.gauntlet.core.commands;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class OrchestratorCommands extends AbstractCommandCollection {
    public OrchestratorCommands() {
        super("admin", "Gaia Gauntlet controls");
        // requirePermission(Permissions.ADMIN);
        addAliases("gm");
        addSubCommand(new SomeSubCommand());
    }

    // just mocked for now until we need it / can use it
    private class SomeSubCommand extends AbstractWorldCommand {
        public SomeSubCommand() {
            super("subcommand", "Create a new session");
        }

        @Override
        protected void execute(CommandContext arg0, World arg1, Store<EntityStore> arg2) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'execute'");
        }
    }
}
