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

    public static class MainMenuContainer extends SimpleContainer { public MainMenuContainer() { super(27); } }

    public static class EditorContainer extends SimpleContainer {
        private final String target;
        public EditorContainer(String target) { super(54); this.target = target; }
        public String getTarget() { return this.target; }
    }

    public static void openMainMenu(ServerPlayer player) {
        MainMenuContainer container = new MainMenuContainer();
        for (int i = 0; i < 27; i++) container.setItem(i, createGlass(Items.STAINED_GLASS_PANE.gray(), " "));

        container.setItem(11, createItemWithName(ServerItemHelper.createHeart(), "§cConfigure Heart Recipe"));
        container.setItem(13, createItemWithName(ServerItemHelper.createReviveBeacon(), "§bConfigure Revive Beacon Recipe"));

        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x3, id, inv, container, 3),
            Component.literal("Lifesteal - Recipe Editor")
        ));
    }

    public static void openRecipeEditor(ServerPlayer player, String target) {
        EditorContainer container = new EditorContainer(target);
        ItemStack[] currentMatrix = target.equals("heart") ? LifestealConfig.heartRecipeMatrix : LifestealConfig.beaconRecipeMatrix;

        for (int i = 0; i < 54; i++) container.setItem(i, createGlass(Items.STAINED_GLASS_PANE.gray(), " "));

        int[] gridSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
        for (int i = 0; i < 9; i++) {
            container.setItem(gridSlots[i], currentMatrix[i].copy());
        }

        container.setItem(23, target.equals("heart") ? ServerItemHelper.createHeart() : ServerItemHelper.createReviveBeacon());
        container.setItem(45, createGlass(Items.EMERALD_BLOCK, "§a§lSAVE RECIPE"));
        container.setItem(49, createGlass(Items.ARROW, "§e§lGO BACK"));
        container.setItem(53, createGlass(Items.REDSTONE_BLOCK, "§c§lDISCARD CHANGES"));

        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x6, id, inv, container, 6),
            Component.literal("Editing: " + target.toUpperCase() + " Recipe")
        ));
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