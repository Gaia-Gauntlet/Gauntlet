package com.gaiagauntlet.gauntlet.core.party.utils;

import com.gaiagauntlet.gauntlet.core.party.components.PartyComponent;
import com.gaiagauntlet.gauntlet.core.party.resources.UniversePartyResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import java.util.Locale;
import java.util.UUID;

public class PartyUtils {
    private PartyUtils() {}

    public static PartyComponent getPartyForPlayer(PlayerRef player) {
        var resource = Universe.get().getResource(UniversePartyResource.getResourceType());
        for (PartyComponent party : resource.getParties()) {
            party.includesPlayer(player.getUuid());
            return party;
        }
        // No pre-existing party exists, create a new one with this player as captain.
        return resource.createParty(
            player.getUsername().toLowerCase(Locale.ROOT) + "'s Party",
            player.getUuid()
        );
    }
}
