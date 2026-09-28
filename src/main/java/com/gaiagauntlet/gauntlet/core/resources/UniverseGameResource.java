package com.gaiagauntlet.gauntlet.core.resources;

import java.lang.module.ModuleDescriptor.Builder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.core.session.components.SessionComponent;
import com.gaiagauntlet.gauntlet.core.session.components.SessionState;
import com.gaiagauntlet.gauntlet.core.session.registry.SessionRegistry;
import com.gaiagauntlet.gauntlet.core.session.testing.SessionComponentTest;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.server.core.universe.resources.UniverseResourceType;

import lombok.Getter;
import lombok.Setter;

/**
 * Universe-scoped resource for game management. Mutations should only happen
 * via the Orchestrator and nowhere else.
 * 
 * Reads
 */
public class UniverseGameResource {
        public static final String ID = "UniverseGameResource";
        public static final BuilderCodec<@NotNull UniverseGameResource> CODEC = BuilderCodec
                        .builder(UniverseGameResource.class, UniverseGameResource::new)
                        .append(new KeyedCodec("Sessions", new MapCodec<>(SessionState.CODEC, ConcurrentHashMap::new, false)),
                                        (resource, v) -> resource.sessions = v,
                                        resource -> resource.sessions)
                        .add()
                        .build();
        @Setter
        @Getter
        private static UniverseResourceType<UniverseGameResource> resourceType;

        private Map<String, SessionState> sessions = new ConcurrentHashMap<>();
}
