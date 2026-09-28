package com.gaiagauntlet.gauntlet.core.session.testing;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.core.session.components.SessionComponent;
import com.gaiagauntlet.gauntlet.core.session.components.SessionComponentType;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import lombok.Getter;
import lombok.Setter;

public class SessionComponentTest implements SessionComponent {
        public static final BuilderCodec<@NotNull SessionComponentTest> CODEC = BuilderCodec
                        .builder(SessionComponentTest.class, SessionComponentTest::new)
                        .append(new KeyedCodec<>("TestValue", Codec.STRING),
                                        (tst, v) -> tst.value = v,
                                        tst -> tst.value)
                        .add()
                        .build();
        @Getter
        @Setter
        private static SessionComponentType<SessionComponentTest> componentType;

        public SessionComponentTest() {

        }

        @Setter
        private String value;
}