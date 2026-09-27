package io.canvasmc.testmod;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.effect.ServerMobEffectEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.item.v1.FabricItem;
import net.fabricmc.fabric.api.util.EventResult;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.atomic.AtomicInteger;

public final class TestEntityEvents {
    private static final Identifier ITEM_ID = Identifier.fromNamespaceAndPath("horizon-testmod", "custom_damage_tool");
    private static final AttachmentType<String> NOTE = AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("horizon-testmod", "note"), Codec.STRING);

    private TestEntityEvents() {
    }

    public static void register() {
        Item tool = Registry.register(BuiltInRegistries.ITEM, ITEM_ID, new Item(((FabricItem.Properties) new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, ITEM_ID))
            .durability(100))
            .customDamage((stack, amount, entity, slot, onBreak) -> {
                TestMod.LOGGER.info("entity: custom damage {} to {}", amount, stack.getItem());
                return amount * 2;
            })));

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            TestMod.LOGGER.info("entity: allow death {}", entity.getType().toShortString());
            return true;
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> TestMod.LOGGER.info("entity: after death {}", entity.getType().toShortString()));
        ServerLivingEntityEvents.MOB_CONVERSION.register((previous, converted, context) ->
            TestMod.LOGGER.info("entity: conversion {} -> {}", previous.getType().toShortString(), converted.getType().toShortString()));
        ServerMobEffectEvents.BEFORE_ADD.register((effect, entity, context) -> TestMod.LOGGER.info("entity: before add {} to {}", effect.getEffect().getRegisteredName(), entity.getType().toShortString()));
        ServerMobEffectEvents.ALLOW_EARLY_REMOVE.register((effect, entity, context) -> {
            TestMod.LOGGER.info("entity: allow early remove {}", effect.getEffect().getRegisteredName());
            return true;
        });
        ServerMobEffectEvents.BEFORE_REMOVE.register((effect, entity, context) -> TestMod.LOGGER.info("entity: before remove {}", effect.getEffect().getRegisteredName()));
        EntitySleepEvents.ALLOW_SETTING_SPAWN.register((player, pos) -> {
            TestMod.LOGGER.info("entity: allow setting spawn at {}", pos.toShortString());
            return true;
        });
        EntitySleepEvents.ALLOW_NEARBY_MONSTERS.register((player, pos, noMonsters) -> {
            TestMod.LOGGER.info("entity: allow nearby monsters at {}, none nearby={}", pos.toShortString(), noMonsters);
            return EventResult.PASS;
        });

        AtomicInteger changes = new AtomicInteger();
        ServerChunkEvents.FULL_CHUNK_STATUS_CHANGE.register((level, chunk, from, to) -> {
            if (changes.incrementAndGet() <= 3) TestMod.LOGGER.info("entity: chunk {} {} -> {}", chunk.getPos(), from, to);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> {
            dispatcher.register(Commands.literal("horizonnote")
                .executes((command) -> {
                    ServerPlayer player = command.getSource().getPlayerOrException();
                    String note = ((AttachmentTarget) player).getAttached(NOTE);
                    TestMod.LOGGER.info("entity: note of {} is {}", player.getName().getString(), note);
                    command.getSource().sendSuccess(() -> Component.literal("note: " + note), false);
                    return 1;
                })
                .then(Commands.argument("text", StringArgumentType.greedyString()).executes((command) -> {
                    ServerPlayer player = command.getSource().getPlayerOrException();
                    ((AttachmentTarget) player).setAttached(NOTE, StringArgumentType.getString(command, "text"));
                    command.getSource().sendSuccess(() -> Component.literal("note set"), false);
                    return 1;
                })));
            dispatcher.register(Commands.literal("horizondamage").executes((command) -> {
                ServerPlayer player = command.getSource().getPlayerOrException();
                ItemStack stack = new ItemStack(tool);
                player.setItemSlot(EquipmentSlot.MAINHAND, stack);
                stack.hurtAndBreak(3, player, EquipmentSlot.MAINHAND);
                TestMod.LOGGER.info("entity: tool damage is {}", stack.getDamageValue());
                command.getSource().sendSuccess(() -> Component.literal("tool damage: " + stack.getDamageValue()), false);
                return 1;
            }));
        });
    }
}
