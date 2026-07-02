package com.drypted.lifesteal.gui;

import com.drypted.lifesteal.api.ServerItemHelper;
import com.drypted.lifesteal.config.LifestealConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class RecipeGUI {

    // Main menu button slots
    public static final int HEART_BUTTON_SLOT = 11;
    public static final int BEACON_BUTTON_SLOT = 13;

    // Editor layout
    public static final int[] GRID_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    public static final int RESULT_SLOT = 23;
    public static final int BRUSH_SLOT = 25;
    public static final int SAVE_SLOT = 45;
    public static final int BACK_SLOT = 49;
    public static final int DISCARD_SLOT = 53;
    public static final int CONTAINER_SIZE = 54;

    public static class MainMenuContainer extends SimpleContainer { public MainMenuContainer() { super(27); } }

    public static class EditorContainer extends SimpleContainer {
        private final String target;
        // The ingredient currently "picked" by the admin. Never a real inventory item — always a
        // count-1 ghost copy that lives only in this transient container, so it can never be lost.
        private ItemStack selected = ItemStack.EMPTY;

        public EditorContainer(String target) { super(CONTAINER_SIZE); this.target = target; }
        public String getTarget() { return this.target; }

        public ItemStack getSelected() { return this.selected; }
        public void setSelected(ItemStack stack) {
            this.selected = (stack == null || stack.isEmpty()) ? ItemStack.EMPTY : stack.copyWithCount(1);
        }
    }

    public static boolean isGridSlot(int slot) {
        for (int s : GRID_SLOTS) if (s == slot) return true;
        return false;
    }

    public static void openMainMenu(ServerPlayer player) {
        MainMenuContainer container = new MainMenuContainer();
        for (int i = 0; i < 27; i++) container.setItem(i, createGlass(Items.GRAY_STAINED_GLASS_PANE, " "));

        container.setItem(HEART_BUTTON_SLOT, createItemWithName(ServerItemHelper.createHeart(), "§cConfigure Heart Recipe"));
        container.setItem(BEACON_BUTTON_SLOT, createItemWithName(ServerItemHelper.createReviveBeacon(), "§bConfigure Revive Beacon Recipe"));

        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x3, id, inv, container, 3),
            Component.literal("Lifesteal - Recipe Editor")
        ));
    }

    public static void openRecipeEditor(ServerPlayer player, String target) {
        EditorContainer container = new EditorContainer(target);
        ItemStack[] currentMatrix = target.equals("heart") ? LifestealConfig.heartRecipeMatrix : LifestealConfig.beaconRecipeMatrix;

        for (int i = 0; i < CONTAINER_SIZE; i++) container.setItem(i, createGlass(Items.GRAY_STAINED_GLASS_PANE, " "));

        for (int i = 0; i < 9; i++) {
            ItemStack cell = currentMatrix[i];
            container.setItem(GRID_SLOTS[i], (cell == null || cell.isEmpty()) ? ItemStack.EMPTY : cell.copyWithCount(1));
        }

        container.setItem(4, createGlass(Items.BOOK, "§eClick an item in your inventory to select it, then click the grid to place it"));
        container.setItem(RESULT_SLOT, target.equals("heart") ? ServerItemHelper.createHeart() : ServerItemHelper.createReviveBeacon());
        updateBrush(container);
        container.setItem(SAVE_SLOT, createGlass(Items.EMERALD_BLOCK, "§a§lSAVE RECIPE"));
        container.setItem(BACK_SLOT, createGlass(Items.ARROW, "§e§lGO BACK"));
        container.setItem(DISCARD_SLOT, createGlass(Items.REDSTONE_BLOCK, "§c§lDISCARD CHANGES"));

        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x6, id, inv, container, 6),
            Component.literal("Editing: " + target.toUpperCase() + " Recipe")
        ));
    }

    // Refreshes the "selected ingredient" indicator slot to match the container's current selection.
    public static void updateBrush(EditorContainer container) {
        ItemStack selected = container.getSelected();
        if (selected.isEmpty()) {
            container.setItem(BRUSH_SLOT, createGlass(Items.LIGHT_GRAY_STAINED_GLASS_PANE,
                    "§7Selected: none §8(click an item in your inventory)"));
        } else {
            container.setItem(BRUSH_SLOT, createItemWithName(selected.copyWithCount(1), "§aSelected ingredient"));
        }
    }

    private static ItemStack createGlass(Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(s -> s.withItalic(false)));
        return stack;
    }

    private static ItemStack createItemWithName(ItemStack stack, String name) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(s -> s.withItalic(false)));
        return stack;
    }
}
