package com.gaiagauntlet.gauntlet.core.party.commands;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.Nullable;

import com.gaiagauntlet.gauntlet.core.events.GauntletEventRegistry;
import com.gaiagauntlet.gauntlet.core.events.events.PlayerGameEvent;
import com.gaiagauntlet.gauntlet.core.events.events.PlayerPartyEvent;
import com.gaiagauntlet.gauntlet.core.party.utils.PartyUtils;
import com.hypixel.hytale.builtin.adventure.reputation.command.ReputationAddCommand;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * All of these are, currently, debug and a stop-gap until the full eventing
 * pipeline is finished
 * 
 * 
 * Once the eventing pipeline is done, the orchestrator will be managing all of
 * this stuff and these commands will just
 * emit intents
 */
public class RejoinCommand extends AbstractTargetPlayerCommand {

    /**
     * Default Constructor for a {@link ReputationAddCommand} instance.
     */
    public RejoinCommand() {
        super("rejoin", "server.commands.reputation.add.desc");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nullable final Ref<EntityStore> sourceRef,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world,
            @Nonnull Store<EntityStore> store) {

        // rejoins a party the player is in
        var party = PartyUtils.getPartyNullable(playerRef).orElse(null);
        if (party == null) return; // not in party

        if (!party.includesOfflinePlayer(playerRef)) {
            return; // player is not considered offline
        }

        // dispatch add to party - marks as online again and joins the session if the session is active
        GauntletEventRegistry.dispatch(PlayerPartyEvent.Join(playerRef, party.getId()));
    }
}
