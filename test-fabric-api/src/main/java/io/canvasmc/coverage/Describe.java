package io.canvasmc.coverage;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;

final class Describe {
    private Describe() {
    }

    static @NonNull String kind(@Nullable Object value) {
        return switch (value) {
            case BlockEntity blockEntity -> "block entity " + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
            case ChunkAccess ignored -> "chunk";
            case null, default -> of(value);
        };
    }

    static @NonNull String of(@Nullable Object value) {
        return switch (value) {
            case null -> "null";
            case String text -> text;
            case Boolean bool -> bool.toString();
            case Float number -> String.format(Locale.ROOT, "%.1f", number);
            case Double number -> String.format(Locale.ROOT, "%.1f", number);
            case Number number -> number.toString();
            case Enum<?> constant -> constant.name();
            case Identifier identifier -> identifier.toString();
            case ResourceKey<?> key -> key.identifier().toString();
            case Holder<?> holder -> holder.getRegisteredName();
            case MinecraftServer ignored -> "server";
            case Player player -> "player " + player.getName().getString();
            case Entity entity -> "entity " + BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            case Level level -> level.dimension().identifier().toString();
            case BlockPos pos -> pos.toShortString();
            case Vec3 vec -> String.format(Locale.ROOT, "%.0f,%.0f,%.0f", vec.x, vec.y, vec.z);
            case BlockState state -> BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            case BlockEntity blockEntity -> "block entity " + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()) + " at " + blockEntity.getBlockPos().toShortString();
            case ChunkAccess chunk -> "chunk " + chunk.getPos();
            case ItemStack stack -> stack.isEmpty() ? "empty" : BuiltInRegistries.ITEM.getKey(stack.getItem()) + " x" + stack.getCount();
            case DamageSource source -> "damage " + source.getMsgId();
            case MobEffectInstance effect -> "effect " + effect.getEffect().getRegisteredName();
            case Component component -> "text " + component.getString();
            case PlayerChatMessage message -> "chat " + message.signedContent();
            case ChatType.Bound bound -> "chat type " + bound.chatType().getRegisteredName();
            case CommandSourceStack source -> "source " + source.getTextName();
            case ServerGamePacketListenerImpl listener -> "connection " + listener.player.getName().getString();
            case ServerConfigurationPacketListenerImpl listener -> "configuring " + listener.getOwner().name();
            case BlockHitResult hit -> "hit block " + hit.getBlockPos().toShortString();
            case EntityHitResult hit -> "hit " + of(hit.getEntity());
            case UseOnContext context -> "use on " + context.getClickedPos().toShortString();
            case Collection<?> collection -> "collection of " + collection.size();
            case Map<?, ?> map -> "map of " + map.size();
            default -> value.getClass().getSimpleName().replaceAll("/0x\\p{XDigit}+$", "");
        };
    }
}
