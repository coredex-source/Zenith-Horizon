package io.canvasmc.horizon.inject.fabricapi.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.canvasmc.horizon.fabric.FabricApiModule;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Map;

@Mixin(TagLoader.class)
public abstract class TagLoaderMixin {

    @WrapOperation(method = "loadTagsForRegistry(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/core/WritableRegistry;Lio/papermc/paper/plugin/lifecycle/event/registrar/ReloadableRegistrarEvent$Cause;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/tags/TagLoader;loadTagsForRegistry(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/tags/TagLoader$ElementLookup;Lnet/minecraft/core/WritableRegistry;Lio/papermc/paper/plugin/lifecycle/event/registrar/ReloadableRegistrarEvent$Cause;)Ljava/util/Map;"))
    private static <T> Map<TagKey<T>, List<Holder<T>>> horizon$fabricBindTags(ResourceManager manager, ResourceKey<? extends Registry<T>> key, TagLoader.ElementLookup<Holder<T>> lookup, WritableRegistry<T> registry, ReloadableRegistrarEvent.Cause cause, Operation<Map<TagKey<T>, List<Holder<T>>>> original) {
        Map<TagKey<T>, List<Holder<T>>> tags = original.call(manager, key, lookup, registry, cause);
        if (FabricApiModule.TAG_API.isLoaded()) {
            registry.bindTags(tags);
        }
        return tags;
    }
}
