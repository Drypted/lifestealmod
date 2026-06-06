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
import java.util.concurrent.ConcurrentHashMap;

public class EnchantmentCapGUI {

    // Per‑player pagination state (weak map to avoid memory leaks)
    private static final Map<ServerPlayer, PageState> PAGE_STATES = new ConcurrentHashMap<>();

    private static class PageState {
        int leftPage = 0;   // active caps
        int rightPage = 0;  // all enchantments
    }

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
        PageState state = PAGE_STATES.computeIfAbsent(player, k -> new PageState());
        CapListContainer container = new CapListContainer();
        for (int i = 0; i < 54; i++) container.setItem(i, createGlass(Items.GRAY_STAINED_GLASS_PANE, " "));

        // ----- LEFT SIDE: active caps (slots 0-26) -----
        List<Map.Entry<String, Integer>> capsList = new ArrayList<>(LifestealConfigManager.getInstance().enchantmentCaps.entrySet());
        int capsPerPage = 27;
        int leftTotalPages = Math.max(1, (capsList.size() + capsPerPage - 1) / capsPerPage);
        // clamp page
        if (state.leftPage >= leftTotalPages) state.leftPage = leftTotalPages - 1;
        if (state.leftPage < 0) state.leftPage = 0;

        int startLeft = state.leftPage * capsPerPage;
        int endLeft = Math.min(startLeft + capsPerPage, capsList.size());
        int slot = 0;
        for (int i = startLeft; i < endLeft; i++) {
            Map.Entry<String, Integer> entry = capsList.get(i);
            String enchId = entry.getKey();
            int maxLevel = entry.getValue();
            Optional<Holder.Reference<Enchantment>> enchHolder = getEnchantmentHolder(player, enchId);
            if (enchHolder.isPresent()) {
                ItemStack icon = new ItemStack(Items.ENCHANTED_BOOK);
                ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                mutable.set(enchHolder.get(), maxLevel);
                icon.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
                icon.set(DataComponents.CUSTOM_NAME, Component.literal("§e" + enchId.split(":")[1] + " §7Cap: §a" + maxLevel));
                CompoundTag tag = new CompoundTag();
                tag.putString("enchant_id", enchId);
                icon.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                container.setItem(slot, icon);
            } else {
                container.setItem(slot, createGlass(Items.BARRIER, "§cUnknown: " + enchId));
            }
            slot++;
        }
        // fill remaining left slots with glass
        for (; slot < 27; slot++) {
            container.setItem(slot, createGlass(Items.GRAY_STAINED_GLASS_PANE, " "));
        }

        // Left pagination controls (bottom row)
        container.setItem(45, createGlass(Items.ARROW, "§e« Previous Caps"));
        container.setItem(46, createGlass(Items.PAPER, "§7Page " + (state.leftPage + 1) + "/" + leftTotalPages));
        container.setItem(47, createGlass(Items.ARROW, "§eNext Caps »"));

        // ----- RIGHT SIDE: all enchantments (slots 27-53) -----
        List<Identifier> allEnchIds = new ArrayList<>();
        Registry<Enchantment> registry = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (Identifier id : registry.keySet()) {
            allEnchIds.add(id);
        }
        int rightTotalPages = Math.max(1, (allEnchIds.size() + capsPerPage - 1) / capsPerPage);
        if (state.rightPage >= rightTotalPages) state.rightPage = rightTotalPages - 1;
        if (state.rightPage < 0) state.rightPage = 0;

        int startRight = state.rightPage * capsPerPage;
        int endRight = Math.min(startRight + capsPerPage, allEnchIds.size());
        slot = 27;
        for (int i = startRight; i < endRight; i++) {
            Identifier id = allEnchIds.get(i);
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
        // fill remaining right slots with glass
        for (; slot < 54; slot++) {
            container.setItem(slot, createGlass(Items.GRAY_STAINED_GLASS_PANE, " "));
        }

        // Right pagination controls
        container.setItem(50, createGlass(Items.ARROW, "§e« Previous Enchants"));
        container.setItem(51, createGlass(Items.PAPER, "§7Page " + (state.rightPage + 1) + "/" + rightTotalPages));
        container.setItem(52, createGlass(Items.ARROW, "§eNext Enchants »"));

        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x6, id, inv, container, 6),
            Component.literal("Enchantment Caps")
        ));
    }

    public static void changeLeftPage(ServerPlayer player, int delta) {
        PageState state = PAGE_STATES.computeIfAbsent(player, k -> new PageState());
        List<Map.Entry<String, Integer>> capsList = new ArrayList<>(LifestealConfigManager.getInstance().enchantmentCaps.entrySet());
        int capsPerPage = 27;
        int totalPages = Math.max(1, (capsList.size() + capsPerPage - 1) / capsPerPage);
        state.leftPage = Math.max(0, Math.min(totalPages - 1, state.leftPage + delta));
        openMainMenu(player);
    }

    public static void changeRightPage(ServerPlayer player, int delta) {
        PageState state = PAGE_STATES.computeIfAbsent(player, k -> new PageState());
        Registry<Enchantment> registry = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<Identifier> allEnchIds = new ArrayList<>(registry.keySet());
        int capsPerPage = 27;
        int totalPages = Math.max(1, (allEnchIds.size() + capsPerPage - 1) / capsPerPage);
        state.rightPage = Math.max(0, Math.min(totalPages - 1, state.rightPage + delta));
        openMainMenu(player);
    }

    // Adjuster methods remain the same
    public static void openAdjuster(ServerPlayer player, String enchantmentId, int currentCap, int maxPossible) {
        AdjusterContainer container = new AdjusterContainer(enchantmentId, currentCap, maxPossible);
        for (int i = 0; i < 27; i++) container.setItem(i, createGlass(Items.GRAY_STAINED_GLASS_PANE, " "));

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