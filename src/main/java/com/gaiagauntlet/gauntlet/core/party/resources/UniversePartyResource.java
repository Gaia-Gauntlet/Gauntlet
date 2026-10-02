package com.gaiagauntlet.gauntlet.core.party.resources;

import com.gaiagauntlet.gauntlet.core.party.components.PartyComponent;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.server.core.universe.resources.UniverseResourceType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
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
        var party = parties.get(partyId);
        if (Objects.isNull(party)) {
            party = new PartyComponent();
            parties.put(partyId, party);
        }
        return party;
    }

    public PartyComponent removeParty(String partyId) {
        return parties.remove(partyId);
    }

    @Override
    public @Nullable Resource<EntityStore> clone() {
        return new UniversePartyResource();
    }
}
