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

    // Map 9x6 layout to side-by-side
    private static final int[] LEFT_SLOTS = {0,1,2,3, 9,10,11,12, 18,19,20,21, 27,28,29,30, 36,37,38,39};
    private static final int[] RIGHT_SLOTS = {5,6,7,8, 14,15,16,17, 23,24,25,26, 32,33,34,35, 41,42,43,44};
    private static final int[] DIVIDER_SLOTS = {4, 13, 22, 31, 40, 49};

    private static class PageState {
        int leftPage = 0;
        int rightCategory = 0;
        int rightPage = 0;
    }

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
        CapListContainer container = new CapListContainer();
        updateMainMenu(player, container); // Populate the container
        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x6, id, inv, container, 6),
            Component.literal("Enchantment Caps")
        ));
    }

    // New In-Place Update Method
    public static void updateMainMenu(ServerPlayer player, CapListContainer container) {
        PageState state = PAGE_STATES.computeIfAbsent(player, k -> new PageState());

        // Fill background and divider
        for (int i = 0; i < 54; i++) container.setItem(i, createGlass(Items.STAINED_GLASS_PANE.gray(), " "));
        for (int slot : DIVIDER_SLOTS) container.setItem(slot, createGlass(Items.STAINED_GLASS_PANE.black(), " "));

        // Left panel: active caps (20 slots)
        List<Map.Entry<String, Integer>> capsList = new ArrayList<>(LifestealConfigManager.getInstance().enchantmentCaps.entrySet());
        int capsPerPage = LEFT_SLOTS.length;
        int leftTotalPages = Math.max(1, (capsList.size() + capsPerPage - 1) / capsPerPage);
        if (state.leftPage >= leftTotalPages) state.leftPage = leftTotalPages - 1;
        if (state.leftPage < 0) state.leftPage = 0;

        int startLeft = state.leftPage * capsPerPage;
        int endLeft = Math.min(startLeft + capsPerPage, capsList.size());
        
        for (int i = 0; i < capsPerPage; i++) {
            int slot = LEFT_SLOTS[i];
            int listIndex = startLeft + i;
            
            if (listIndex < endLeft) {
                Map.Entry<String, Integer> entry = capsList.get(listIndex);
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
            } else {
                container.setItem(slot, ItemStack.EMPTY);
            }
        }

        container.setItem(45, createGlass(Items.ARROW, "§e« Prev Caps"));
        container.setItem(46, createGlass(Items.PAPER, "§7Page " + (state.leftPage + 1) + "/" + leftTotalPages));
        container.setItem(47, createGlass(Items.ARROW, "§eNext Caps »"));

        // Right panel: enchantments by category (20 slots)
        Registry<Enchantment> registry = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Category currentCat = CATEGORIES.get(state.rightCategory);
        List<Holder.Reference<Enchantment>> catEnchants = getEnchantmentsForCategory(registry, currentCat);
        int itemsPerPage = RIGHT_SLOTS.length;
        int rightTotalPages = Math.max(1, (catEnchants.size() + itemsPerPage - 1) / itemsPerPage);
        if (state.rightPage >= rightTotalPages) state.rightPage = rightTotalPages - 1;
        if (state.rightPage < 0) state.rightPage = 0;

        int startRight = state.rightPage * itemsPerPage;
        int endRight = Math.min(startRight + itemsPerPage, catEnchants.size());

        for (int i = 0; i < itemsPerPage; i++) {
            int slot = RIGHT_SLOTS[i];
            int listIndex = startRight + i;
            
            if (listIndex < endRight) {
                Holder.Reference<Enchantment> enchHolder = catEnchants.get(listIndex);
                Identifier id = registry.getKey(enchHolder.value());
                if (id == null) continue;
                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                book.set(DataComponents.CUSTOM_NAME, Component.literal("§b" + id.getPath()));
                CompoundTag tag = new CompoundTag();
                tag.putString("enchant_id", id.toString());
                book.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                container.setItem(slot, book);
            } else {
                container.setItem(slot, ItemStack.EMPTY);
            }
        }

        container.setItem(50, createGlass(Items.ARROW, "§e« Prev Enchants"));
        container.setItem(51, createGlass(Items.PAPER, "§7Page " + (state.rightPage + 1) + "/" + rightTotalPages));
        container.setItem(52, createGlass(Items.ARROW, "§eNext Enchants »"));
        container.setItem(53, createGlass(Items.NAME_TAG, "§eCycle Cat: " + currentCat.displayName()));
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

    public static void changeLeftPage(ServerPlayer player, int delta, CapListContainer container) {
        PageState state = PAGE_STATES.computeIfAbsent(player, k -> new PageState());
        List<Map.Entry<String, Integer>> capsList = new ArrayList<>(LifestealConfigManager.getInstance().enchantmentCaps.entrySet());
        int totalPages = Math.max(1, (capsList.size() + LEFT_SLOTS.length - 1) / LEFT_SLOTS.length);
        state.leftPage = Math.max(0, Math.min(totalPages - 1, state.leftPage + delta));
        updateMainMenu(player, container); // Update in-place
    }

    public static void changeRightPage(ServerPlayer player, int delta, CapListContainer container) {
        PageState state = PAGE_STATES.computeIfAbsent(player, k -> new PageState());
        Registry<Enchantment> registry = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Category currentCat = CATEGORIES.get(state.rightCategory);
        List<Holder.Reference<Enchantment>> catEnchants = getEnchantmentsForCategory(registry, currentCat);
        int totalPages = Math.max(1, (catEnchants.size() + RIGHT_SLOTS.length - 1) / RIGHT_SLOTS.length);
        state.rightPage = Math.max(0, Math.min(totalPages - 1, state.rightPage + delta));
        updateMainMenu(player, container); // Update in-place
    }

    public static void changeCategory(ServerPlayer player, int delta, CapListContainer container) {
        PageState state = PAGE_STATES.computeIfAbsent(player, k -> new PageState());
        int newCat = state.rightCategory + delta;
        if (newCat < 0) newCat = CATEGORIES.size() - 1;
        if (newCat >= CATEGORIES.size()) newCat = 0;
        state.rightCategory = newCat;
        state.rightPage = 0;
        updateMainMenu(player, container); // Update in-place
    }

    // openAdjuster, getMaxPossibleLevel, getEnchantmentHolder, createGlass remain the exact same below...
    public static void openAdjuster(ServerPlayer player, String enchantmentId, int currentCap, int maxPossible) {
        AdjusterContainer container = new AdjusterContainer(enchantmentId, currentCap, maxPossible);
        for (int i = 0; i < 27; i++) container.setItem(i, createGlass(Items.STAINED_GLASS_PANE.gray(), " "));

        container.setItem(11, createGlass(Items.STAINED_GLASS_PANE.red(), "§c-1"));
        container.setItem(13, createGlass(Items.PAPER, "§eCurrent cap: §a" + currentCap + " / " + maxPossible));
        container.setItem(15, createGlass(Items.STAINED_GLASS_PANE.lime(), "§a+1"));
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