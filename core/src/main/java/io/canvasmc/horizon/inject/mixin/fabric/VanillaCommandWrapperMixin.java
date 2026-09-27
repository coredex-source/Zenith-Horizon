package io.canvasmc.horizon.inject.mixin.fabric;

import com.mojang.brigadier.tree.CommandNode;
import io.canvasmc.horizon.fabric.HorizonFabric;
import io.canvasmc.horizon.fabric.ModOwners;
import io.papermc.paper.command.brigadier.ShadowBrigNode;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.commands.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.craftbukkit.command.VanillaCommandWrapper;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VanillaCommandWrapper.class)
public abstract class VanillaCommandWrapperMixin extends BukkitCommand {
    @Shadow
    @Final
    public CommandNode<CommandSourceStack> vanillaCommand;
    @Shadow
    @Final
    @Mutable
    public String helpCommandNamespace;

    protected VanillaCommandWrapperMixin(String name) {
        super(name);
    }

    @Inject(method = "<init>(Lcom/mojang/brigadier/tree/CommandNode;)V", at = @At("RETURN"))
    private void horizon$modCommandOwner(CommandNode<CommandSourceStack> node, CallbackInfo ci) {
        if (!HorizonFabric.isLoaded() || !horizon$modCommand(this.getPermission())) {
            return;
        }

        ModContainer owner = horizon$owner(horizon$vanilla(node));
        if (owner != null && !owner.getMetadata().getId().equals("minecraft")) {
            this.helpCommandNamespace = owner.getMetadata().getName();
            this.setDescription("A " + owner.getMetadata().getName() + " provided command.");
        }
    }

    @Override
    public boolean testPermissionSilent(@NonNull CommandSender target) {
        String permission = this.getPermission();
        if (HorizonFabric.isLoaded() && horizon$modCommand(permission) && !target.isPermissionSet(permission)) {
            try {
                return horizon$vanilla(this.vanillaCommand).canUse(VanillaCommandWrapper.getListener(target));
            } catch (IllegalArgumentException exception) {
                return super.testPermissionSilent(target);
            }
        }
        return super.testPermissionSilent(target);
    }

    @SuppressWarnings("unchecked")
    private static @NonNull CommandNode<CommandSourceStack> horizon$vanilla(@NonNull CommandNode<?> node) {
        return (CommandNode<CommandSourceStack>) (node instanceof ShadowBrigNode shadow ? shadow.getHandle() : node);
    }

    private static @Nullable ModContainer horizon$owner(@NonNull CommandNode<?> node) {
        if (node.getCommand() != null) {
            return ModOwners.of(node.getCommand().getClass());
        }
        for (CommandNode<?> child : node.getChildren()) {
            ModContainer owner = horizon$owner(child);
            if (owner != null) {
                return owner;
            }
        }
        return null;
    }

    private static boolean horizon$modCommand(@Nullable String permission) {
        return permission != null && Bukkit.getServer() != null && Bukkit.getPluginManager().getPermission(permission) == null;
    }
}
