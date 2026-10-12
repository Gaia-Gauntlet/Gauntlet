package com.gaiagauntlet.gauntlet.plugins.auto.components.rotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.plugins.auto.components.AutoComponent;
import com.gaiagauntlet.gauntlet.plugins.auto.components.assets.SessionPreset;
import com.gaiagauntlet.gauntlet.plugins.auto.components.VoteComponent;
import com.gaiagauntlet.gauntlet.plugins.auto.utils.VoteUtils;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;

import lombok.Getter;

public final class VoteRotation extends PresetRotation {
    public static final String TYPE = "Vote";

    public static final BuilderCodec<@NotNull VoteRotation> CODEC = BuilderCodec
            .builder(VoteRotation.class, VoteRotation::new, PresetRotation.BASE_CODEC)
            .append(new KeyedCodec<>("Pool", Codec.STRING_ARRAY),
                    (rotation, v) -> rotation.pool = v,
                    rotation -> rotation.pool)
            .documentation("The presets a vote draws its choices from")
            .addValidatorLate(() -> SessionPreset.VALIDATOR_CACHE.getArrayValidator().late())
            .add()
            .append(new KeyedCodec<>("Choices", Codec.INTEGER),
                    (rotation, v) -> rotation.choices = v,
                    rotation -> rotation.choices)
            .addValidator(Validators.greaterThanOrEqual(1))
            .documentation("How many presets each vote offers. The preset just played is left out while the pool has enough others")
            .add()
            .build();

    @Getter
    private String[] pool = new String[0];
    private int choices = 3; // hardcoded cuz i dont wanna make dynamic ui 

    @Override
    public void open(@Nonnull GameSession session, @Nonnull AutoComponent auto) {
        if (VoteUtils.isOpen(session) || pool.length == 0)
            return;
        var offered = new ArrayList<>(List.of(pool));
        if (offered.size() > choices)
            offered.remove(auto.getCurrentPreset());
        Collections.shuffle(offered, ThreadLocalRandom.current());
        VoteUtils.open(session, offered.subList(0, Math.min(choices, offered.size())));
    }

    @Nullable
    @Override
    public String pick(@Nonnull GameSession session, @Nonnull AutoComponent auto) {
        var vote = VoteUtils.get(session);
        var winner = vote == null ? random(List.of(pool)) : decided(vote);
        session.remove(VoteComponent.getComponentType());
        return winner;
    }

    @Nullable
    @Override
    public String upNext(@Nonnull GameSession session, @Nonnull AutoComponent auto) {
        var vote = VoteUtils.get(session);
        if (vote == null)
            return null;
        return vote.isClosed() ? vote.getWinner() : vote.leader(ThreadLocalRandom.current());
    }

    @Override
    public boolean holdsIntermission() {
        return true;
    }

    @Nullable
    private static String decided(@Nonnull VoteComponent vote) {
        var winner = vote.isClosed() ? vote.getWinner() : vote.leader(ThreadLocalRandom.current());
        return winner != null ? winner : random(vote.getOptions());
    }

    @Nullable
    private static String random(@Nonnull List<String> presets) {
        return presets.isEmpty() ? null : presets.get(ThreadLocalRandom.current().nextInt(presets.size()));
    }
}
