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

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.msg;

public class PartyUtils {
    public static final long EXPIRY_SECONDS = 30;
    private PartyUtils() {}

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

    public static void sendPartyInvite(PlayerRef sender, PlayerRef recipient) {
        var party = getPartyForPlayer(sender);
        var invites = getInvitesComp(recipient);
        if (hasActiveInvite(party.getId(), recipient)) {
            sender.sendMessage(msg(recipient.getUsername() + " already has an active invite from you."));
            return;
        }
        var expiry = HytaleServer.SCHEDULED_EXECUTOR.schedule(
            () -> expireInvite(party.getId(), sender, recipient), EXPIRY_SECONDS,
            TimeUnit.SECONDS
        );
        invites.putInvite(party.getId(), sender.getUuid(), expiry);
        recipient.sendMessage(Message.raw("You have received a party invite from "
            + sender.getUsername() + "!"
        ));
        sender.sendMessage(Message.raw("Invited " + recipient.getUsername() + " to join your party"));
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

            if (Objects.nonNull(invite.getExpiryFuture())) invite.getExpiryFuture().cancel(false);
        });
    }

    public static void acceptInvite(PlayerRef sender, PlayerRef recipient) {

    }

    static boolean hasActiveInvite(String partyId, PlayerRef recipient) {
        var invites = getInvitesComp(recipient);
        var invite = invites.getInvite(partyId);
        if (Objects.isNull(invite)) return false;
        if (Objects.isNull(invite.getExpiryFuture())) {
            // This is a stale invite, cancel it
            invites.removeInvite(partyId);
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
