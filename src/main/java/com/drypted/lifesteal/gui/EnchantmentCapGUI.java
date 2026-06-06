package com.drypted.lifesteal.gui;

import com.drypted.lifesteal.config.LifestealConfigManager;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.*;

public class EnchantmentCapGUI {

    public static class CapListContainer extends SimpleContainer {
        public CapListContainer() { super(54); }
    }

    public static class AdjusterContainer extends SimpleContainer {
        private final String enchantmentId;
        private int currentCap;
        private int maxPossible;
        public AdjusterContainer(String enchantmentId, int currentCap, int maxPossible) {
            super(27);
            this.enchantmentId = enchantmentId;
            this.currentCap = currentCap;
            this.maxPossible = maxPossible;
        }
        public String getEnchantmentId() { return enchantmentId; }
        public int getCurrentCap() { return currentCap; }
        public int getMaxPossible() { return maxPossible; }
        public void setCurrentCap(int cap) { this.currentCap = cap; }
        public void updateDisplay() {
            ItemStack display = createGlass(Items.PAPER, "§eCurrent cap: §a" + currentCap + " / " + maxPossible);
            this.setItem(13, display);
        }
    }

    public static void openMainMenu(ServerPlayer player) {
        CapListContainer container = new CapListContainer();
        // Fill with gray glass
        for (int i = 0; i < 54; i++) container.setItem(i, createGlass(Items.GRAY_STAINED_GLASS_PANE, " "));

        // Left side (slots 0-26): active caps
        Map<String, Integer> caps = LifestealConfigManager.getInstance().enchantmentCaps;
        int slot = 0;
        for (Map.Entry<String, Integer> entry : caps.entrySet()) {
            if (slot >= 27) break;
            String enchId = entry.getKey();
            int maxLevel = entry.getValue();
            Optional<Holder.Reference<Enchantment>> enchHolder = getEnchantmentHolder(player, enchId);
            if (enchHolder.isPresent()) {
                ItemStack icon = new ItemStack(Items.ENCHANTED_BOOK);
                ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                mutable.set(enchHolder.get(), maxLevel);
                icon.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
                icon.set(DataComponents.CUSTOM_NAME, Component.literal("§e" + enchId.split(":")[1] + " §7Cap: §a" + maxLevel));
                // Store enchantment ID in custom data so clicks work
                CompoundTag tag = new CompoundTag();
                tag.putString("enchant_id", enchId);
                icon.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                container.setItem(slot, icon);
            } else {
                container.setItem(slot, createGlass(Items.BARRIER, "§cUnknown: " + enchId));
            }
            slot++;
        }

        // Right side (slots 27-53): all enchantments
        slot = 27;
        Registry<Enchantment> registry = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (Identifier id : registry.keySet()) {
            if (slot >= 54) break;
            Optional<Holder.Reference<Enchantment>> opt = registry.get(id);
            if (opt.isEmpty()) continue;
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            book.set(DataComponents.CUSTOM_NAME, Component.literal("§b" + id.getPath()));
            CompoundTag tag = new CompoundTag();
            tag.putString("enchant_id", id.toString());
            book.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            container.setItem(slot, book);
            slot++;
        }

        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x6, id, inv, container, 6),
            Component.literal("Enchantment Caps")
        ));
    }

    public static void openAdjuster(ServerPlayer player, String enchantmentId, int currentCap, int maxPossible) {
        AdjusterContainer container = new AdjusterContainer(enchantmentId, currentCap, maxPossible);
        for (int i = 0; i < 27; i++) container.setItem(i, createGlass(Items.GRAY_STAINED_GLASS_PANE, " "));

        // Buttons: -1, +1, Remove Cap, Back
        container.setItem(11, createGlass(Items.RED_STAINED_GLASS_PANE, "§c-1"));
        container.setItem(13, createGlass(Items.PAPER, "§eCurrent cap: §a" + currentCap + " / " + maxPossible));
        container.setItem(15, createGlass(Items.LIME_STAINED_GLASS_PANE, "§a+1"));
        container.setItem(22, createGlass(Items.BARRIER, "§cRemove Cap"));
        container.setItem(26, createGlass(Items.ARROW, "§eBack"));

        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x3, id, inv, container, 3),
            Component.literal("Adjust Cap: " + enchantmentId)
        ));
    }

    public static int getMaxPossibleLevel(ServerPlayer player, String enchantmentId) {
        Optional<Holder.Reference<Enchantment>> ench = getEnchantmentHolder(player, enchantmentId);
        return ench.map(e -> e.value().getMaxLevel()).orElse(0);
    }

    private static Optional<Holder.Reference<Enchantment>> getEnchantmentHolder(ServerPlayer player, String idStr) {
        Identifier id = Identifier.tryParse(idStr);
        if (id == null) return Optional.empty();
        return player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(id);
    }

    private static ItemStack createGlass(Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(s -> s.withItalic(false)));
        return stack;
    }
}