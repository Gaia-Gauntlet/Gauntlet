package com.gaiagauntlet.gauntlet.core.codec;

import com.hypixel.hytale.codec.builder.BuilderCodec;

/** Abstract interface to be implemented by registries so they can be decoded - used in all the stores */
public interface CodecRegistry<P> {
    <T extends P> BuilderCodec<T> getCodec(String id);
}
