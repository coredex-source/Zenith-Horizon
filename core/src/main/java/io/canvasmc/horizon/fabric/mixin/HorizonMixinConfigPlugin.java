package io.canvasmc.horizon.fabric.mixin;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.logger.Logger;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.util.List;
import java.util.Set;

public final class HorizonMixinConfigPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = Logger.fork(HorizonLoader.LOGGER, "mixin_preflight");

    private FabricMixinConfigs.@Nullable Entry entry;
    private @Nullable IMixinConfigPlugin delegate;

    @Override
    public void onLoad(String mixinPackage) {
        entry = FabricMixinConfigs.claim(mixinPackage);
        if (entry == null || entry.plugin() == null) {
            return;
        }

        try {
            Class<?> pluginClass = MixinService.getService().getClassProvider().findClass(entry.plugin(), true);
            delegate = (IMixinConfigPlugin) pluginClass.getDeclaredConstructor().newInstance();
        } catch (Throwable thrown) {
            LOGGER.error(thrown, "Error loading plugin class [{}] for mixin config [{}]", entry.plugin(), entry.name());
            return;
        }

        delegate.onLoad(mixinPackage);
    }

    @Override
    public String getRefMapperConfig() {
        return delegate != null ? delegate.getRefMapperConfig() : null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (delegate != null && !delegate.shouldApplyMixin(targetClassName, mixinClassName)) {
            return false;
        }
        return entry == null || MixinPreflight.check(entry, targetClassName, mixinClassName);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        MixinPreflight.finish();
        if (delegate != null) {
            delegate.acceptTargets(myTargets, otherTargets);
        }
    }

    @Override
    public List<String> getMixins() {
        return delegate != null ? delegate.getMixins() : null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (delegate != null) {
            delegate.preApply(targetClassName, targetClass, mixinClassName, mixinInfo);
        }
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (delegate != null) {
            delegate.postApply(targetClassName, targetClass, mixinClassName, mixinInfo);
        }
    }
}
