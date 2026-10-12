package com.gaiagauntlet.gauntlet.plugins.chat.resource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.plugins.chat.components.ChatChannel;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.resources.UniverseResourceType;

import lombok.Getter;
import lombok.Setter;

public class UniverseChatResource {
    public static final String ID = "UniverseChatResource";
    public static final BuilderCodec<@NotNull UniverseChatResource> CODEC = BuilderCodec
            .builder(UniverseChatResource.class, UniverseChatResource::new)
            .build();

    @Setter @Getter private static UniverseResourceType<UniverseChatResource> resourceType;

    private final Map<UUID, ChatChannel> channels = new ConcurrentHashMap<>();

    @Nonnull
    public static UniverseChatResource get() {
        return Universe.get().getResource(resourceType);
    }

    @Nonnull
    public ChatChannel channelOf(@Nonnull UUID player) {
        return channels.getOrDefault(player, ChatChannel.GLOBAL);
    }

    public void select(@Nonnull UUID player, @Nonnull ChatChannel channel) {
        if (channel instanceof ChatChannel.Global)
            channels.remove(player);
        else
            channels.put(player, channel);
    }

    public void clear(@Nonnull UUID player) {
        channels.remove(player);
    }
}
