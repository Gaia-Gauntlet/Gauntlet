package com.gaiagauntlet.gauntlet.core.commands;

import org.jspecify.annotations.NonNull;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.orchestrator.GauntletOrchestrator;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/** A command any player can run that opens one page, focused on the player's own session. */
public class OpenPageCommand extends AbstractPlayerCommand {

    private final String pageId;

    public OpenPageCommand(@NonNull String name, @NonNull String description, @NonNull String pageId) {
        super(name, description);
        this.pageId = pageId;
        requireNoPermission();
    }

    @Override
    protected void execute(@NonNull CommandContext context, @NonNull Store<EntityStore> store, @NonNull Ref<EntityStore> ref,
            @NonNull PlayerRef playerRef, @NonNull World world) {
        var session = GauntletUtils.sessionFor(playerRef).orElse(null);
        GauntletOrchestrator.openPage(store, ref, playerRef, pageId, session);
    }
}
