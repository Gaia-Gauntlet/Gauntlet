package com.gaiagauntlet.gauntlet.core.ui.events;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import lombok.Getter;

public class AdminPageEvent {

    public static final BuilderCodec<@NotNull AdminPageEvent> CODEC = BuilderCodec
            .builder(AdminPageEvent.class, AdminPageEvent::new)
            .append(new KeyedCodec<>("Action", Codec.STRING), (e, v) -> e.action = v, e -> e.action).add()
            .append(new KeyedCodec<>("Arg", Codec.STRING, false), (e, v) -> e.arg = v, e -> e.arg).add()
            .append(new KeyedCodec<>("@Pick", Codec.STRING, false), (e, v) -> e.pick = v, e -> e.pick).add()
            .append(new KeyedCodec<>("@Text", Codec.STRING, false), (e, v) -> e.text = v, e -> e.text).add()
            .append(new KeyedCodec<>("@Num", Codec.STRING, false), (e, v) -> e.num = v, e -> e.num).add()
            .build();

    private String action;
    private String arg;
    private String pick;
    private String text;
    private String num;

    @Nonnull 
    public String action() {
        return action == null ? "" : action;
    }

    @Nonnull
    public String arg() {
        return arg == null ? "" : arg;
    }

    @Nonnull
    public String pick() {
        return pick == null ? "" : pick.trim();
    }

    @Nonnull
    public String text() {
        return text == null ? "" : text.trim();
    }

    @Nonnull
    public String num() {
        return num == null ? "" : num.trim();
    }
}
