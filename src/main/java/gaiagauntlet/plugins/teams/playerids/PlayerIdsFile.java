package gaiagauntlet.plugins.teams.playerids;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;

/** The on-disk shape of the known player account ids. */
public final class PlayerIdsFile {

    public static final BuilderCodec<PlayerIdsFile> CODEC = BuilderCodec.builder(PlayerIdsFile.class, PlayerIdsFile::new)
            .append(new KeyedCodec<>("Players", new MapCodec<>(Codec.STRING, LinkedHashMap::new)),
                    (f, v) -> f.players = v == null ? new LinkedHashMap<>() : new LinkedHashMap<>(v), f -> f.players)
            .documentation("Lower-cased username to account UUID, learned from joins and profile lookups.")
            .add()
            .build();

    private Map<String, String> players = new LinkedHashMap<>();

    public PlayerIdsFile() {
    }

    @Nonnull
    public Map<String, String> players() {
        return players;
    }
}
