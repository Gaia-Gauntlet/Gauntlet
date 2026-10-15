package com.gaiagauntlet.earlyplugin;

import com.hypixel.hytale.plugin.early.ClassTransformer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassHierarchyResolver;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.Label;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;

/**
 * Makes {@code Interaction.handle} skip syncing when the context entity lives in a different store
 * (world) than the ref being handled. Equivalent to this source change:
 *
 * <pre>
 * if (!entity.isValid() || entity.getStore() != ref.getStore()) {
 *     return;
 * }
 * </pre>
 */
public final class InteractionWorldGuard implements ClassTransformer {
    private static final String TARGET = "com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction";
    private static final String HANDLE_DESCRIPTOR = "(Lcom/hypixel/hytale/component/Ref;ZFLcom/hypixel/hytale/protocol/InteractionType;Lcom/hypixel/hytale/server/core/entity/InteractionContext;)V";

    private static final ClassDesc REF = ClassDesc.of("com.hypixel.hytale.component.Ref");
    private static final ClassDesc STORE = ClassDesc.of("com.hypixel.hytale.component.Store");
    private static final ClassDesc CONTEXT = ClassDesc.of("com.hypixel.hytale.server.core.entity.InteractionContext");

    @Nullable
    @Override
    public byte[] transform(@Nonnull String name, @Nonnull String internalName, @Nonnull byte[] bytes) {
        if (!name.equals(TARGET)) {
            return null;
        }

        // Stack map regeneration needs to look up server classes without loading them.
        ClassFile classFile = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(
                ClassHierarchyResolver.ofResourceParsing(InteractionWorldGuard.class.getClassLoader())
                        .orElse(ClassHierarchyResolver.defaultResolver())));

        boolean[] patched = {false};
        byte[] result = classFile.transformClass(classFile.parse(bytes), ClassTransform.transformingMethodBodies(
                method -> method.methodName().equalsString("handle") && method.methodType().equalsString(HANDLE_DESCRIPTOR),
                CodeTransform.ofStateful(() -> new CodeTransform() {
                    private boolean injected;

                    @Override
                    public void accept(CodeBuilder builder, CodeElement element) {
                        if (!injected) {
                            injected = true;
                            patched[0] = true;
                            emitGuard(builder);
                        }
                        builder.with(element);
                    }
                })));

        if (!patched[0]) {
            System.err.println("[GauntletEarlyPlugin] Interaction.handle not found, world guard not applied");
            return null;
        }
        System.out.println("[GauntletEarlyPlugin] Applied world guard to Interaction.handle");
        return result;
    }

    private static void emitGuard(CodeBuilder builder) {
        Label proceed = builder.newLabel();
        int refSlot = builder.parameterSlot(0);
        int contextSlot = builder.parameterSlot(4);

        builder.aload(contextSlot)
                .invokevirtual(CONTEXT, "getEntity", MethodTypeDesc.of(REF))
                .invokevirtual(REF, "getStore", MethodTypeDesc.of(STORE))
                .aload(refSlot)
                .invokevirtual(REF, "getStore", MethodTypeDesc.of(STORE))
                .if_acmpeq(proceed)
                .return_()
                .labelBinding(proceed);
    }
}
