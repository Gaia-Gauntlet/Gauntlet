package com.gaiagauntlet.gauntlet.core.codec;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.bson.BsonDocument;
import org.bson.BsonValue;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.ObjectSchema;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.logger.HytaleLogger;

/** Standard component that lets you pass a supplier for dynamic codec decoding/encoding */
public class StringRegistryCodec<T extends SerializableComponent, M extends Map<String, T>>
        implements Codec<Map<String, T>> {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private CodecRegistry<T> registry;
    private Supplier<M> supplier;

    public StringRegistryCodec(CodecRegistry<T> registry, Supplier<M> supplier) {
        this.registry = registry;
        this.supplier = supplier;
    }

    @Override
    public Schema toSchema(@Nonnull SchemaContext context) {
        var schema = new ObjectSchema();
        schema.setTitle("GameStore components");
        schema.setAdditionalProperties(true);
        return schema;
    }

    @Override
    public Map<String, T> decode(@Nonnull BsonValue bsonValue, @Nonnull ExtraInfo extraInfo) {
        BsonDocument document = bsonValue.asDocument();
        var map = supplier.get();

        
        for (Map.Entry<String, BsonValue> entry : document.entrySet()) {
            var id = entry.getKey();
            var codec = registry.getCodec(id);
            BsonValue value = entry.getValue();

            if (codec == null) {
                LOGGER.atWarning().atMostEvery(30, TimeUnit.SECONDS)
                        .log("Skipping game component without a codec while decoding: %s", id);
                continue;
            }
            extraInfo.pushKey(id);
            try {

                map.put(id, codec.decode(value, extraInfo));
            } catch (Exception exception) {
                LOGGER.atWarning().withCause(exception).log("Failed to decode game component: %s", id);
            } finally {
                extraInfo.popKey();
            }
        }
        return map;
    }

    @Override
    public BsonValue encode(@Nonnull Map<String, T> components, @Nonnull ExtraInfo extraInfo) {
        BsonDocument document = new BsonDocument();

        for (var entry : components.entrySet()) {
            String id = entry.getKey();
            var codec = registry.getCodec(id);
            var value = entry.getValue();
            
            if (codec == null) {
                // LOGGER.atWarning().atMostEvery(30, TimeUnit.SECONDS).log("Skipping game
                // component without a codec while encoding: %s", id);
                continue;
            }
            extraInfo.pushKey(id);
            try {
                document.put(id, codec.encode(value, extraInfo));
            } catch (Exception exception) {
                LOGGER.atWarning().withCause(exception).log("Failed to encode game component: %s", id);
            } finally {
                extraInfo.popKey();
            }
        }
        return document;
    }
}
