package com.gaiagauntlet.gauntlet.plugins.scoring.components;

import com.gaiagauntlet.gauntlet.core.gamestore.components.GameComponent;
import com.gaiagauntlet.gauntlet.core.session.components.SessionComponent;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

public class ScoresComponent implements SessionComponent, GameComponent {
    public static final BuilderCodec<@NotNull ScoresComponent> CODEC = BuilderCodec
        .builder(ScoresComponent.class, ScoresComponent::new)
        .append(new KeyedCodec<>("TeamScoreMap", new MapCodec<>(Codec.INTEGER, HashMap::new)),
            (c, v) -> c.teamScoreMap = v,
            ScoresComponent::getTeamScoreMap)
        .documentation("A map from team ids to scores")
        .add()
        .build();

    @Getter Map<String, Integer> teamScoreMap;

    public ScoresComponent() {}

    public int getScore(String teamId) {
        return teamScoreMap.getOrDefault(teamId, 0);
    }

    public int grantScore(String teamId, int score) {
        Integer oldScore = teamScoreMap.get(teamId);
        if (Objects.isNull(oldScore)) {
            teamScoreMap.put(teamId, score);
        }
        else {
            oldScore += score;
            teamScoreMap.put(teamId, oldScore + score);
        }
        return score;
    }

    public int revokeScore(String teamId, int score) {
        return grantScore(teamId, -score);
    }

    public void setScore(String teamId, int score) {
        teamScoreMap.put(teamId, score);
    }

    public void clear(String teamId) {
        teamScoreMap.remove(teamId);
    }

    public void clear() {
        teamScoreMap.clear();
    }
}
