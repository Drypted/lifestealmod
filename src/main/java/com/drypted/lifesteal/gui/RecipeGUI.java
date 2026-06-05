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
import java.util.List;

public class RecipeGUI {

    // --- CUSTOM SEPARATE CONTAINER CLASSES FOR IDENTIFICATION ---
    public static class MainMenuContainer extends SimpleContainer { public MainMenuContainer() { super(27); } }
    public static class SettingsContainer extends SimpleContainer { public SettingsContainer() { super(27); } }
    
    public static class EditorContainer extends SimpleContainer {
        private final String recipeTarget; // "heart" or "beacon"
        public EditorContainer(String recipeTarget) {
            super(54);
            this.recipeTarget = recipeTarget;
        }
        public String getRecipeTarget() { return this.recipeTarget; }
    }

    // --- OPEN MENU METHODS ---

    public static void openMainMenu(ServerPlayer player) {
        MainMenuContainer container = new MainMenuContainer();
        
        // Fill background with gray glass
        for (int i = 0; i < 27; i++) {
            container.setItem(i, createGuiGlass(Items.GRAY_STAINED_GLASS_PANE, " "));
        }

        // Heart Recipe configuration button
        ItemStack heartItem = ServerItemHelper.createHeart();
        heartItem.set(DataComponents.CUSTOM_NAME, Component.literal("§cConfigure Heart Recipe"));
        container.setItem(11, heartItem);

        // Revive Beacon Recipe configuration button
        ItemStack beaconItem = ServerItemHelper.createReviveBeacon();
        beaconItem.set(DataComponents.CUSTOM_NAME, Component.literal("§bConfigure Revive Beacon Recipe"));
        container.setItem(13, beaconItem);

        // Settings Button
        ItemStack settingsItem = new ItemStack(Items.COMPARATOR);
        settingsItem.set(DataComponents.CUSTOM_NAME, Component.literal("§eRecipe & Crafting Settings"));
        container.setItem(15, settingsItem);

        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x3, id, inv, container, 3),
            Component.literal("Lifesteal - Recipe Hub")
        ));
    }

    public static void openRecipeEditor(ServerPlayer player, String target) {
        EditorContainer container = new EditorContainer(target);
        List<ItemStack> currentMatrix = target.equals("heart") ? LifestealConfig.heartRecipeMatrix : LifestealConfig.beaconRecipeMatrix;

        // Build Frame Blueprint
        for (int i = 0; i < 54; i++) {
            container.setItem(i, createGuiGlass(Items.GRAY_STAINED_GLASS_PANE, " "));
        }

        // Map existing recipe items into the 3x3 active editing fields
        int[] gridSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
        for (int i = 0; i < 9; i++) {
            container.setItem(gridSlots[i], currentMatrix.get(i).copy());
        }

        // Display output element layout
        ItemStack visualResult = target.equals("heart") ? ServerItemHelper.createHeart() : ServerItemHelper.createReviveBeacon();
        container.setItem(23, visualResult);

        // Functional action navigation items
        container.setItem(45, createGuiGlass(Items.EMERALD_BLOCK, "§a§lSAVE RECIPE"));
        container.setItem(49, createGuiGlass(Items.ARROW, "§e§lGO BACK"));
        container.setItem(53, createGuiGlass(Items.REDSTONE_BLOCK, "§c§lDISCARD CHANGES"));

        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x6, id, inv, container, 6),
            Component.literal("Editing: " + target.toUpperCase() + " Recipe")
        ));
    }

    public static void openSettingsMenu(ServerPlayer player) {
        SettingsContainer container = new SettingsContainer();

        for (int i = 0; i < 27; i++) {
            container.setItem(i, createGuiGlass(Items.GRAY_STAINED_GLASS_PANE, " "));
        }

        // Toggle Heart Recipe Component
        ItemStack toggleHeart = new ItemStack(LifestealConfig.heartRecipeEnabled ? Items.LIME_CONCRETE : Items.RED_CONCRETE);
        toggleHeart.set(DataComponents.CUSTOM_NAME, Component.literal("§7Heart Crafting: " + (LifestealConfig.heartRecipeEnabled ? "§aENABLED" : "§cDISABLED")));
        container.setItem(10, toggleHeart);

        // Toggle Beacon Recipe Component
        ItemStack toggleBeacon = new ItemStack(LifestealConfig.beaconRecipeEnabled ? Items.LIME_CONCRETE : Items.RED_CONCRETE);
        toggleBeacon.set(DataComponents.CUSTOM_NAME, Component.literal("§7Beacon Crafting: " + (LifestealConfig.beaconRecipeEnabled ? "§aENABLED" : "§cDISABLED")));
        container.setItem(11, toggleBeacon);

        // Toggle Restrictions By Health Status
        ItemStack toggleLimit = new ItemStack(LifestealConfig.limitHeartCraftingByHealth ? Items.ENDER_EYE : Items.ENDER_PEARL);
        toggleLimit.set(DataComponents.CUSTOM_NAME, Component.literal("§7Health Crafting Limits: " + (LifestealConfig.limitHeartCraftingByHealth ? "§aACTIVE" : "§cINACTIVE")));
        container.setItem(13, toggleLimit);

        // Edit Minimum Limits Configuration
        ItemStack minLimit = new ItemStack(Items.CHIPPED_ANVIL);
        minLimit.set(DataComponents.CUSTOM_NAME, Component.literal("§cMin Hearts to Craft: §e" + (LifestealConfig.minHeartsToCraft / 2.0)));
        container.setItem(14, minLimit);

        // Edit Maximum Limits Configuration
        ItemStack maxLimit = new ItemStack(Items.ANVIL);
        maxLimit.set(DataComponents.CUSTOM_NAME, Component.literal("§aMax Hearts to Craft: §e" + (LifestealConfig.maxHeartsToCraft / 2.0)));
        container.setItem(15, maxLimit);

        // Return Navigation Vector
        container.setItem(22, createGuiGlass(Items.ARROW, "§eBack to Menu"));

        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x3, id, inv, container, 3),
            Component.literal("Lifesteal Recipe Settings")
        ));
    }

    
    private static ItemStack createGuiGlass(Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(
            DataComponents.CUSTOM_NAME,
            Component.literal(name).withStyle(s -> s.withItalic(false))
        );
        return stack;
    }
}