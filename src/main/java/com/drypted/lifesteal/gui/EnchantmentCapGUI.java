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
import net.minecraft.tags.EnchantmentTags;
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

    private static final Map<ServerPlayer, PageState> PAGE_STATES = new ConcurrentHashMap<>();

    private static class PageState {
        int leftPage = 0;
        int rightCategory = 0;
        int rightPage = 0;
    }

    // Category predicates using Holder<Enchantment>.is(TagKey)
    private static final List<Category> CATEGORIES = Arrays.asList(
        new Category("§cARMOR", holder -> holder.is(EnchantmentTags.ARMOR_EXCLUSIVE) || holder.is(EnchantmentTags.BOOTS_EXCLUSIVE)),
        new Category("§6WEAPON", holder -> holder.is(EnchantmentTags.DAMAGE_EXCLUSIVE)),
        new Category("§bBOW", holder -> holder.is(EnchantmentTags.BOW_EXCLUSIVE)),
        new Category("§3CROSSBOW", holder -> holder.is(EnchantmentTags.CROSSBOW_EXCLUSIVE)),
        new Category("§aTOOL", holder -> holder.is(EnchantmentTags.MINING_EXCLUSIVE)),
        new Category("§dTRIDENT", holder -> holder.is(EnchantmentTags.RIPTIDE_EXCLUSIVE)),
        new Category("§5CURSE", holder -> holder.is(EnchantmentTags.CURSE)),
        new Category("§7OTHER", holder -> !(holder.is(EnchantmentTags.ARMOR_EXCLUSIVE) || holder.is(EnchantmentTags.BOOTS_EXCLUSIVE) ||
                                            holder.is(EnchantmentTags.DAMAGE_EXCLUSIVE) || holder.is(EnchantmentTags.BOW_EXCLUSIVE) ||
                                            holder.is(EnchantmentTags.CROSSBOW_EXCLUSIVE) || holder.is(EnchantmentTags.MINING_EXCLUSIVE) ||
                                            holder.is(EnchantmentTags.RIPTIDE_EXCLUSIVE) || holder.is(EnchantmentTags.CURSE)))
    );

    private record Category(String displayName, java.util.function.Predicate<Holder<Enchantment>> predicate) {}

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

        // Left panel: active caps (slots 0-26)
        List<Map.Entry<String, Integer>> capsList = new ArrayList<>(LifestealConfigManager.getInstance().enchantmentCaps.entrySet());
        int capsPerPage = 27;
        int leftTotalPages = Math.max(1, (capsList.size() + capsPerPage - 1) / capsPerPage);
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

        container.setItem(45, createGlass(Items.ARROW, "§e« Prev Caps"));
        container.setItem(46, createGlass(Items.PAPER, "§7Page " + (state.leftPage + 1) + "/" + leftTotalPages));
        container.setItem(47, createGlass(Items.ARROW, "§eNext Caps »"));

        // Right panel: enchantments by category (slots 27-44)
        Registry<Enchantment> registry = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Category currentCat = CATEGORIES.get(state.rightCategory);
        List<Holder.Reference<Enchantment>> catEnchants = getEnchantmentsForCategory(registry, currentCat);
        int itemsPerPage = 18;
        int rightTotalPages = Math.max(1, (catEnchants.size() + itemsPerPage - 1) / itemsPerPage);
        if (state.rightPage >= rightTotalPages) state.rightPage = rightTotalPages - 1;
        if (state.rightPage < 0) state.rightPage = 0;

        int startRight = state.rightPage * itemsPerPage;
        int endRight = Math.min(startRight + itemsPerPage, catEnchants.size());
        slot = 27;
        for (int i = startRight; i < endRight; i++) {
            Holder.Reference<Enchantment> enchHolder = catEnchants.get(i);
            Identifier id = registry.getKey(enchHolder.value());
            if (id == null) continue;
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            book.set(DataComponents.CUSTOM_NAME, Component.literal("§b" + id.getPath()));
            CompoundTag tag = new CompoundTag();
            tag.putString("enchant_id", id.toString());
            book.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            container.setItem(slot, book);
            slot++;
        }
        for (; slot < 45; slot++) container.setItem(slot, createGlass(Items.GRAY_STAINED_GLASS_PANE, " "));

        container.setItem(48, createGlass(Items.ARROW, "§e« Prev Enchants"));
        container.setItem(49, createGlass(Items.PAPER, "§7Page " + (state.rightPage + 1) + "/" + rightTotalPages));
        container.setItem(50, createGlass(Items.ARROW, "§eNext Enchants »"));
        container.setItem(51, createGlass(Items.BOOKSHELF, "§e« Prev Category"));
        container.setItem(52, createGlass(Items.NAME_TAG, currentCat.displayName()));
        container.setItem(53, createGlass(Items.BOOKSHELF, "§eNext Category »"));

        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x6, id, inv, container, 6),
            Component.literal("Enchantment Caps")
        ));
    }

    private static List<Holder.Reference<Enchantment>> getEnchantmentsForCategory(Registry<Enchantment> registry, Category cat) {
        List<Holder.Reference<Enchantment>> result = new ArrayList<>();
        for (Identifier id : registry.keySet()) {
            Optional<Holder.Reference<Enchantment>> holderOpt = registry.get(id);
            if (holderOpt.isPresent()) {
                Holder.Reference<Enchantment> holder = holderOpt.get();
                if (cat.predicate().test(holder)) {
                    result.add(holder);
                }
            }
        }
        result.sort(Comparator.comparing(h -> registry.getKey(h.value()).getPath()));
        return result;
    }

    // Navigation methods (same as before)
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
        Category currentCat = CATEGORIES.get(state.rightCategory);
        List<Holder.Reference<Enchantment>> catEnchants = getEnchantmentsForCategory(registry, currentCat);
        int itemsPerPage = 18;
        int totalPages = Math.max(1, (catEnchants.size() + itemsPerPage - 1) / itemsPerPage);
        state.rightPage = Math.max(0, Math.min(totalPages - 1, state.rightPage + delta));
        openMainMenu(player);
    }

    public static void changeCategory(ServerPlayer player, int delta) {
        PageState state = PAGE_STATES.computeIfAbsent(player, k -> new PageState());
        int newCat = state.rightCategory + delta;
        if (newCat < 0) newCat = CATEGORIES.size() - 1;
        if (newCat >= CATEGORIES.size()) newCat = 0;
        state.rightCategory = newCat;
        state.rightPage = 0;
        openMainMenu(player);
    }

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