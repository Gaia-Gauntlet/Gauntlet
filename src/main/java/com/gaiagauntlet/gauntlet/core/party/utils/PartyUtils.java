package com.gaiagauntlet.gauntlet.core.party.utils;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.party.components.PartyComponent;
import com.gaiagauntlet.gauntlet.core.party.components.PartyInvitesComponent;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.msg;

public class PartyUtils {
    public static final long EXPIRY_SECONDS = 180;
    private PartyUtils() {}

    public static PartyComponent getParty(String partyId) {
        var resource = GauntletUtils.withResource();
        return resource.getParty(partyId);
    }

    /**
     * Ensures that the player is always in a party, even if it's a singleton.
     */
    @Nonnull
    public static PartyComponent getPartyForPlayer(PlayerRef player) {
        var resource = GauntletUtils.withResource();
        for (PartyComponent party : resource.getParties().values()) {
            party.includesPlayer(player.getUuid());
            return party;
        }
        // No pre-existing party exists, create a new one with this player as captain.
        return resource.createParty(
            player.getUsername().toLowerCase(Locale.ROOT) + "'s Party",
            player.getUuid()
        );
    }

    /**
     * Gets the party for the player without making a new one if they aren't in one already.
     */
    @Nullable
    private static PartyComponent getPartyForPlayerNullable(PlayerRef player) {
        var resource = GauntletUtils.withResource();
        for (PartyComponent party : resource.getParties().values()) {
            party.includesPlayer(player.getUuid());
            return party;
        }
        return null;
    }

    public static void leaveParty(PlayerRef playerRef) {
        var party = getPartyForPlayer(playerRef);
        if (party.size() == 1) {
            playerRef.sendMessage(msg("You're the only one in this party, so you cannot leave.").color(Color.RED));
            return;
        }
        var success = party.removePlayer(playerRef.getUuid());
        if (success) {
            playerRef.sendMessage(msg("server.gg.commands.party.left.you").param("party", party.getId()));
        } else {
            playerRef.sendMessage(msg("Failed to leave the party. See logs.").color(Color.RED));
        }
    }

    // Invites

    public static void sendPartyInvite(PlayerRef sender, PlayerRef recipient) {
        var party = getPartyForPlayer(sender);
        var invites = getInvitesComp(recipient);
        if (sender.getUuid().equals(recipient.getUuid())) {
            sender.sendMessage(msg("You can't invite yourself to a party!").color(Color.RED));
            return;
        } else if (hasActiveInvite(party.getId(), recipient)) {
            sender.sendMessage(msg(recipient.getUsername() + " already has an active invite from you."));
            return;
        } else if (party.includesPlayer(recipient.getUuid())) {
            sender.sendMessage(msg(recipient.getUsername() + " is already in this party!"));
            return;
        }
        var expiry = HytaleServer.SCHEDULED_EXECUTOR.schedule(
            () -> expireInvite(party.getId(), sender, recipient), EXPIRY_SECONDS,
            TimeUnit.SECONDS
        );
        invites.putInvite(party.getId(), sender.getUuid(), expiry);
        recipient.sendMessage(msg("server.gg.commands.party.invite.received")
            .param("sender", sender.getUsername())
            .param("party", party.getId())
            .param("expiry", EXPIRY_SECONDS));
        sender.sendMessage(msg("server.gg.commands.party.invite.sent")
            .param("recipient", recipient.getUsername())
            .param("party", party.getId())
            .param("expiry", EXPIRY_SECONDS));
    }

    public static void expireInvite(String partyId, PlayerRef sender, PlayerRef recipient) {
        // Since this is called by a schedule executor, hop to recipient world thread to ensure
        // we can successfully modify entity components.
        assert recipient.getWorldUuid() != null;
        World world = Universe.get().getWorld(recipient.getWorldUuid());
        assert world != null;
        world.execute(() -> {
            var invites = getInvitesComp(recipient);
            var invite = invites.removeInvite(partyId);
            if (Objects.isNull(invite)) return;

            recipient.sendMessage(Message.raw("Your invite from " + sender.getUsername()
                + " has expired."
            ));
            sender.sendMessage(Message.raw("Your invite to " + recipient.getUsername()
                + " has expired."
            ));

            invite.cancel();
        });
    }


    public static void acceptInvite(PartyComponent party, PlayerRef recipient) {
        var recipParty = getPartyForPlayerNullable(recipient);
        if (Objects.nonNull(recipParty)) { // Remove player from current party
            recipParty.removePlayer(recipient.getUuid());
        }
        if (!hasActiveInvite(party.getId(), recipient)) {
            recipient.sendMessage(msg("You don't have an invite from this party!").color(Color.RED));
            return;
        }
        var invites = getInvitesComp(recipient);
        var invite = invites.getInvite(party.getId());
        invite.cancel();
        party.addPlayer(recipient.getUuid());
        recipient.sendMessage(msg("server.gg.commands.party.joined.you").param("party", party.getId()));
    }

    public static void declineInvite(PartyComponent party, PlayerRef recipient) {
        var invites = getInvitesComp(recipient);
        var invite = invites.removeInvite(party.getId());
        if (Objects.isNull(invite)) {
            recipient.sendMessage(msg("You don't have an invite to that party to decline!").color(Color.RED));
            return;
        }
        recipient.sendMessage(msg("party.invite.declined.you").param("party", party.getId()));
        PlayerRef sender = PlayerUtils.get(invite.getSender());
        if (Objects.nonNull(sender)) {
            sender.sendMessage(msg("party.invite.declined")
                .param("recipient", recipient.getUsername())
                .param("party", party.getId())
            );
        }
    }

    static boolean hasActiveInvite(String partyId, PlayerRef recipient) {
        var invites = getInvitesComp(recipient);
        var invite = invites.getInvite(partyId);
        if (Objects.isNull(invite)) return false;
        if (Objects.isNull(invite.getExpiryFuture())) {
            // This is a stale invite, discount it
            return false;
        }
        return true;
    }

    private static PartyInvitesComponent getInvitesComp(PlayerRef recipient) {
        var recipRef = recipient.getReference();
        assert recipRef != null;
        var recipStore = recipRef.getStore();
        return recipStore.ensureAndGetComponent(recipRef, PartyInvitesComponent.getComponentType());
    }
}
