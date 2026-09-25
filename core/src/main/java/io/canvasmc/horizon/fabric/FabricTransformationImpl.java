package io.canvasmc.horizon.fabric;

import io.canvasmc.horizon.service.transform.TransformPhase;
import io.canvasmc.horizon.service.transform.TransformationService;
import io.canvasmc.horizon.transformer.MixinTransformationImpl;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.game.GameProvider.BuiltinTransform;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.transformer.ClassStripper;
import net.fabricmc.loader.impl.transformer.EnvironmentStrippingData;
import net.fabricmc.loader.impl.transformer.PackageAccessFixer;
import net.fabricmc.loader.impl.util.SystemProperties;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.Set;

public final class FabricTransformationImpl implements TransformationService {
    private static final boolean DISABLE_ENVIRONMENT_STRIP = SystemProperties.isSet(SystemProperties.DISABLE_ENVIRONMENT_STRIP);

    @Override
    public void preboot() {
    }

    @Override
    public int priority(final @NonNull TransformPhase phase) {
        if (phase != TransformPhase.INITIALIZE) return -1;
        return 20;
    }

    @Override
    public boolean shouldTransform(final @NonNull Type type, final @NonNull ClassNode node) {
        return HorizonFabric.isLoaded();
    }

    @Override
    public @Nullable ClassNode transform(final @NonNull Type type, final @NonNull ClassNode node, final @NonNull TransformPhase phase) {
        FabricLoaderImpl loader = FabricLoaderImpl.INSTANCE;
        Set<BuiltinTransform> transforms = loader.getGameProvider().getBuiltinTransforms(type.getClassName());

        boolean applyClassTweaker = transforms.contains(BuiltinTransform.CLASS_TWEAKS)
            && loader.getClassTweaker().getTargets().contains(type.getInternalName());
        boolean transformAccess = transforms.contains(BuiltinTransform.WIDEN_ALL_PACKAGE_ACCESS)
            && FabricLauncherBase.getLauncher().getMappingConfiguration().requiresPackageAccessHack();

        EnvironmentStrippingData stripData = null;
        if (transforms.contains(BuiltinTransform.STRIP_ENVIRONMENT) && !DISABLE_ENVIRONMENT_STRIP) {
            stripData = new EnvironmentStrippingData(MixinTransformationImpl.ASM_VERSION, EnvType.SERVER.toString());
            node.accept(stripData);

            if (stripData.stripEntireClass()) {
                throw new IllegalStateException("Cannot load class " + type.getClassName() + " in " + EnvType.SERVER);
            }

            if (stripData.isEmpty()) {
                stripData = null;
            }
        }

        if (!applyClassTweaker && !transformAccess && stripData == null) {
            return null;
        }

        ClassNode result = new ClassNode(MixinTransformationImpl.ASM_VERSION);
        ClassVisitor visitor = result;

        if (applyClassTweaker) {
            visitor = loader.getClassTweaker().createClassVisitor(MixinTransformationImpl.ASM_VERSION, visitor, null);
        }

        if (transformAccess) {
            visitor = new PackageAccessFixer(MixinTransformationImpl.ASM_VERSION, visitor);
        }

        if (stripData != null) {
            visitor = new ClassStripper(MixinTransformationImpl.ASM_VERSION, visitor, stripData.getStripInterfaces(), stripData.getStripFields(), stripData.getStripMethods());
        }

        node.accept(visitor);
        return result;
    }
}
