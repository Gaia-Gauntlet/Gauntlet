package com.gaiagauntlet.gauntlet.core.party.resources;

import com.gaiagauntlet.gauntlet.core.party.components.PartyComponent;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.resources.UniverseResourceType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UniversePartyResource implements Resource<EntityStore> {
    public static final String ID = "UniversePartyResource";
    @Getter @Setter private static UniverseResourceType<UniversePartyResource> resourceType;

    public static final BuilderCodec<UniversePartyResource> CODEC = BuilderCodec
        .builder(UniversePartyResource.class, UniversePartyResource::new)
        .append(new KeyedCodec<>("Sessions",
                new MapCodec<>(PartyComponent.CODEC, ConcurrentHashMap::new, false)),
            (resource, v) -> resource.parties = v,
            resource -> resource.parties)
        .add()
        .build();

    private Map<String, PartyComponent> parties;

    public PartyComponent getParty(String partyId) {
        return parties.get(partyId);
    }

    public Collection<PartyComponent> getParties() {
        return parties.values();
    }

    public PartyComponent createParty(String partyId, UUID owner) {
        var newParty = new PartyComponent(partyId, new UUID[]{owner});
        parties.put(partyId, newParty);
        return newParty;
    }

    public PartyComponent removeParty(String partyId) {
        return parties.remove(partyId);
    }

    @Override
    public @Nullable Resource<EntityStore> clone() {
        return new UniversePartyResource();
    }
}
