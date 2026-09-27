package gaiagauntlet.plugins.gamestore.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;
import gaiagauntlet.plugins.gamestore.game.Hub;

import javax.annotation.Nonnull;
import java.util.List;

/** The on-disk shape of the hub list. */
public final class HubsFile {

    public static final BuilderCodec<HubsFile> CODEC = BuilderCodec.builder(HubsFile.class, HubsFile::new)
            .append(new KeyedCodec<>("Hubs", new ArrayCodec<>(Entry.CODEC, Entry[]::new)),
                    (file, v) -> file.hubs = v == null ? new Entry[0] : v, file -> file.hubs)
            .documentation("Every hub and its permanent world.")
            .add()
            .build();

    private Entry[] hubs = new Entry[0];

    public HubsFile() {
    }

    @Nonnull
    public List<Entry> hubs() {
        return List.of(hubs);
    }

    public void setHubs(@Nonnull List<Entry> entries) {
        hubs = entries.toArray(Entry[]::new);
    }

    @Nonnull
    public static Entry entryOf(@Nonnull Hub hub) {
        var entry = new Entry();
        entry.id = hub.id();
        entry.world = hub.worldName();
        return entry;
    }

    public static final class Entry {

        public static final BuilderCodec<Entry> CODEC = BuilderCodec.builder(Entry.class, Entry::new)
                .append(new KeyedCodec<>("Id", Codec.STRING), (e, v) -> e.id = v, e -> e.id)
                .addValidator(Validators.nonEmptyString())
                .documentation("Hub id used by commands.")
                .add()
                .append(new KeyedCodec<>("World", Codec.STRING), (e, v) -> e.world = v == null ? "" : v, e -> e.world)
                .documentation("Name of the hub's permanent world.")
                .add()
                .build();

        String id = "";
        String world = "";

        public Entry() {
        }

        @Nonnull
        public String id() {
            return id;
        }

        @Nonnull
        public String world() {
            return world;
        }
    }
}
