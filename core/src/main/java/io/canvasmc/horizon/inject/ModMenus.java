package io.canvasmc.horizon.inject;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class ModMenus {
    private static final Map<AbstractContainerMenu, InventoryView> VIEWS = Collections.synchronizedMap(new WeakHashMap<>());

    private ModMenus() {
    }

    public static @NonNull InventoryView bukkitView(@NonNull AbstractContainerMenu menu) {
        return VIEWS.computeIfAbsent(menu, ModMenus::create);
    }

    private static @NonNull InventoryView create(@NonNull AbstractContainerMenu menu) {
        Player player = null;
        List<Slot> slots = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory inventory) {
                if (player == null) player = inventory.player;
            } else {
                slots.add(slot);
            }
        }
        if (player == null) {
            player = viewer(menu);
        }
        if (player == null) {
            throw new IllegalStateException("Couldn't find the player of " + menu.getClass().getName() + " for its Bukkit view");
        }
        return new CraftInventoryView<>(player.getBukkitEntity(), new CraftInventory(new SlotContainer(menu, slots)), menu);
    }

    private static @Nullable Player viewer(@NonNull AbstractContainerMenu menu) {
        for (ServerPlayer player : MinecraftServer.getServer().getPlayerList().getPlayers()) {
            if (player.containerMenu == menu) return player;
        }
        return null;
    }

    private static final class SlotContainer implements Container {
        private final AbstractContainerMenu menu;
        private final List<Slot> slots;
        private final List<HumanEntity> viewers = new ArrayList<>();
        private int maxStackSize = Container.MAX_STACK;

        private SlotContainer(@NonNull AbstractContainerMenu menu, @NonNull List<Slot> slots) {
            this.menu = menu;
            this.slots = slots;
        }

        @Override
        public int getContainerSize() {
            return this.slots.size();
        }

        @Override
        public boolean isEmpty() {
            for (Slot slot : this.slots) {
                if (!slot.getItem().isEmpty()) return false;
            }
            return true;
        }

        @Override
        public @NonNull ItemStack getItem(int index) {
            return index >= 0 && index < this.slots.size() ? this.slots.get(index).getItem() : ItemStack.EMPTY;
        }

        @Override
        public @NonNull ItemStack removeItem(int index, int count) {
            return index >= 0 && index < this.slots.size() ? this.slots.get(index).remove(count) : ItemStack.EMPTY;
        }

        @Override
        public @NonNull ItemStack removeItemNoUpdate(int index) {
            if (index < 0 || index >= this.slots.size()) return ItemStack.EMPTY;

            ItemStack item = this.slots.get(index).getItem();
            this.slots.get(index).set(ItemStack.EMPTY);
            return item;
        }

        @Override
        public void setItem(int index, @NonNull ItemStack item) {
            if (index >= 0 && index < this.slots.size()) this.slots.get(index).set(item);
        }

        @Override
        public int getMaxStackSize() {
            return this.maxStackSize;
        }

        @Override
        public void setChanged() {
            for (Slot slot : this.slots) {
                slot.setChanged();
            }
        }

        @Override
        public boolean stillValid(@NonNull Player player) {
            return this.menu.stillValid(player);
        }

        @Override
        public @NonNull List<ItemStack> getContents() {
            List<ItemStack> contents = new ArrayList<>(this.slots.size());
            for (Slot slot : this.slots) {
                contents.add(slot.getItem());
            }
            return contents;
        }

        @Override
        public void onOpen(@NonNull CraftHumanEntity player) {
            this.viewers.add(player);
        }

        @Override
        public void onClose(@NonNull CraftHumanEntity player) {
            this.viewers.remove(player);
        }

        @Override
        public @NonNull List<HumanEntity> getViewers() {
            return this.viewers;
        }

        @Override
        public @Nullable InventoryHolder getOwner() {
            return null;
        }

        @Override
        public void setMaxStackSize(int size) {
            this.maxStackSize = size;
        }

        @Override
        public @Nullable Location getLocation() {
            return null;
        }

        @Override
        public void clearContent() {
            for (Slot slot : this.slots) {
                slot.set(ItemStack.EMPTY);
            }
        }
    }
}
