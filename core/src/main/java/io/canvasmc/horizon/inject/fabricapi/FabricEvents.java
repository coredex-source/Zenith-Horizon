package io.canvasmc.horizon.inject.fabricapi;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.effect.ServerMobEffectEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.EntityLoadData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerPickItemEvents;
import net.fabricmc.fabric.api.item.v1.CustomDamageHandler;
import net.fabricmc.fabric.api.menu.v1.FabricMenuProvider;
import net.fabricmc.fabric.impl.entity.event.effect.MobEffectUtil;
import net.fabricmc.fabric.impl.event.lifecycle.EntityLoadDataSetter;
import net.fabricmc.fabric.impl.item.ItemExtensions;
import net.fabricmc.fabric.impl.registry.sync.validate.RegistryCustomContentState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class FabricEvents {
    private FabricEvents() {
    }

    public static void chunkLoad(@NonNull ServerLevel level, @NonNull LevelChunk chunk, boolean generated) {
        if (generated) {
            ServerChunkEvents.CHUNK_GENERATE.invoker().onChunkGenerate(level, chunk);
        }
        ServerChunkEvents.CHUNK_LOAD.invoker().onChunkLoad(level, chunk, generated);
    }

    public static boolean allowEntityLoad(@NonNull ServerLevel level, @NonNull Entity entity, boolean fromDisk) {
        ((EntityLoadDataSetter) entity).fabric_setLoadedFromDisk(fromDisk);
        return ServerEntityEvents.ALLOW_LOAD.invoker().onAllowLoad(entity, level, ((EntityLoadData) entity).spawnReason(), fromDisk);
    }

    public static void chunkUnload(@NonNull ServerLevel level, @NonNull LevelChunk chunk) {
        ServerChunkEvents.CHUNK_UNLOAD.invoker().onChunkUnload(level, chunk);
    }

    public static void levelUnload(@NonNull MinecraftServer server, @NonNull ServerLevel level) {
        ServerLevelEvents.UNLOAD.invoker().onLevelUnload(server, level);
    }

    public static void beforeSave(@NonNull MinecraftServer server, boolean flush, boolean force) {
        ServerLifecycleEvents.BEFORE_SAVE.invoker().onBeforeSave(server, flush, force);
    }

    public static void afterSave(@NonNull MinecraftServer server, boolean flush, boolean force) {
        ServerLifecycleEvents.AFTER_SAVE.invoker().onAfterSave(server, flush, force);
    }

    public static @Nullable ItemStack pickItemFromBlock(@NonNull ServerPlayer player, @NonNull BlockPos pos, @NonNull BlockState state, boolean includeData) {
        return PlayerPickItemEvents.BLOCK.invoker().onPickItemFromBlock(player, pos, state, includeData);
    }

    public static @Nullable ItemStack pickItemFromEntity(@NonNull ServerPlayer player, @NonNull Entity entity, boolean includeData) {
        return PlayerPickItemEvents.ENTITY.invoker().onPickItemFromEntity(player, entity, includeData);
    }

    public static void fullChunkStatusChange(@NonNull ServerLevel level, @NonNull LevelChunk chunk, @NonNull FullChunkStatus from, @NonNull FullChunkStatus to) {
        ServerChunkEvents.FULL_CHUNK_STATUS_CHANGE.invoker().onFullChunkStatusChange(level, chunk, from, to);
    }

    public static boolean allowSettingSpawn(@NonNull ServerPlayer player, @NonNull BlockPos pos) {
        return EntitySleepEvents.ALLOW_SETTING_SPAWN.invoker().allowSettingSpawn(player, pos);
    }

    public static boolean allowNearbyMonsters(@NonNull ServerPlayer player, @NonNull BlockPos pos, boolean noMonsters) {
        return EntitySleepEvents.ALLOW_NEARBY_MONSTERS.invoker().allowNearbyMonsters(player, pos, noMonsters).allowAction(noMonsters);
    }

    public static boolean allowEarlyRemove(@NonNull MobEffectInstance effect, @NonNull LivingEntity entity) {
        return ServerMobEffectEvents.ALLOW_EARLY_REMOVE.invoker().allowEarlyRemove(effect, entity, MobEffectUtil.getCommandContext());
    }

    public static void beforeRemove(@NonNull MobEffectInstance effect, @NonNull LivingEntity entity) {
        ServerMobEffectEvents.BEFORE_REMOVE.invoker().beforeRemove(effect, entity, MobEffectUtil.getCommandContext());
    }

    public static @Nullable Integer customDamage(@NonNull ItemStack stack, int amount, @NonNull LivingEntity entity, @NonNull EquipmentSlot slot, @NonNull Runnable onBreak) {
        CustomDamageHandler handler = ((ItemExtensions) stack.getItem()).fabric_getCustomDamageHandler();
        return handler != null ? handler.hurtAndBreak(stack, amount, entity, slot, onBreak) : null;
    }

    public static boolean shouldCloseCurrentScreen(@NonNull MenuProvider provider) {
        return ((FabricMenuProvider) provider).shouldCloseCurrentScreen();
    }

    public static void writeRegistryState(LevelStorageSource.@NonNull LevelStorageAccess storage, @NonNull RegistryAccess registries) {
        if (present("net.fabricmc.fabric.impl.registry.sync.validate.RegistryCustomContentState")) {
            RegistryCustomContentState.writeIfNeeded(storage, registries);
        }
    }

    private static boolean present(@NonNull String name) {
        try {
            Class.forName(name, false, FabricEvents.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    public static void mobConversion(@NonNull Mob previous, @NonNull Mob converted, @NonNull ConversionParams params) {
        ServerLivingEntityEvents.MOB_CONVERSION.invoker().onConversion(previous, converted, params);
    }
}
