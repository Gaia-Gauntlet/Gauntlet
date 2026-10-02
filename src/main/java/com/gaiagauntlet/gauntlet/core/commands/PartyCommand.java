package com.gaiagauntlet.gauntlet.core.commands;

import com.gaiagauntlet.gauntlet.core.party.utils.PartyUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public class PartyCommand extends AbstractCommandCollection {
    public PartyCommand() {
        super("party", "Commands related to player parties.");
        addAliases("p");
    }

    public static class AddCommand extends AbstractPlayerCommand {

        private final RequiredArg<PlayerRef> inviteeArg = withRequiredArg(
            "player", "Player to invite to your party", ArgTypes.PLAYER_REF
        );

        public AddCommand() {
            super("add", "Invite a player to your party");
        }

        @Override
        protected void execute(@NonNull CommandContext context, @NonNull Store<EntityStore> store, @NonNull Ref<EntityStore> ref, @NonNull PlayerRef playerRef, @NonNull World world) {
            PlayerRef invitee = inviteeArg.get(context);

            var inviteeParty = PartyUtils.getPartyForPlayer(invitee);
            var inviterParty = PartyUtils.getPartyForPlayer(playerRef);

        }
    }

    // TODO: Leave command
}
