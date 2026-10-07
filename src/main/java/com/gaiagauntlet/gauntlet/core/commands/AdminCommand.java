package com.gaiagauntlet.gauntlet.core.commands;

import com.gaiagauntlet.gauntlet.core.orchestrator.GauntletOrchestrator;
import com.gaiagauntlet.gauntlet.core.ui.pages.AdminPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

public class AdminCommand extends AbstractPlayerCommand {

    public AdminCommand() {
        super("dashboard", "Open the game admin dashboard");
        addAliases("dash");
    }

    @Override
    protected void execute(
        @NonNull CommandContext context,
        @NonNull Store<EntityStore> store,
        @NonNull Ref<EntityStore> ref,
        @NonNull PlayerRef playerRef,
        @NonNull World world
    ) {
        // TODO: Fill in session from player
        GauntletOrchestrator.openPage(store, ref, playerRef, AdminPage.ID, null);
    }
}
