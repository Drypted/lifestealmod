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
    public static class SettingsContainer extends SimpleContainer { public SettingsContainer() { super(27); } }
    
    public static class EditorContainer extends SimpleContainer {
        private final String target;
        public EditorContainer(String target) { super(54); this.target = target; }
        public String getTarget() { return this.target; }
    }

    public static class ValueAdjusterContainer extends SimpleContainer {
        private final String targetSetting;
        public ValueAdjusterContainer(String targetSetting) { super(27); this.targetSetting = targetSetting; }
        public String getTargetSetting() { return this.targetSetting; }
    }

    public static void openMainMenu(ServerPlayer player) {
        MainMenuContainer container = new MainMenuContainer();
        for (int i = 0; i < 27; i++) container.setItem(i, createGlass(Items.GRAY_STAINED_GLASS_PANE, " "));

        container.setItem(11, createItemWithName(ServerItemHelper.createHeart(), "§cConfigure Heart Recipe"));
        container.setItem(13, createItemWithName(ServerItemHelper.createReviveBeacon(), "§bConfigure Revive Beacon Recipe"));
        container.setItem(15, createGlass(Items.COMPARATOR, "§eRecipe & Crafting Settings"));

        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x3, id, inv, container, 3),
            Component.literal("Lifesteal - Recipe Hub")
        ));
    }

    public static void openRecipeEditor(ServerPlayer player, String target) {
        EditorContainer container = new EditorContainer(target);
        ItemStack[] currentMatrix = target.equals("heart") ? LifestealConfig.heartRecipeMatrix : LifestealConfig.beaconRecipeMatrix;

        for (int i = 0; i < 54; i++) container.setItem(i, createGlass(Items.GRAY_STAINED_GLASS_PANE, " "));

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

    public static void updateSettingsMenu(SettingsContainer container) {
        for (int i = 0; i < 27; i++) container.setItem(i, createGlass(Items.GRAY_STAINED_GLASS_PANE, " "));

        double maxCraftHearts = LifestealConfig.maxHeartsToCraft / 2.0;
        double absoluteMaxHearts = LifestealConfig.maxHearts / 2.0;

        container.setItem(10, createGlass(LifestealConfig.heartRecipeEnabled ? Items.LIME_CONCRETE : Items.RED_CONCRETE,
                "§7Heart Crafting: " + (LifestealConfig.heartRecipeEnabled ? "§aENABLED" : "§cDISABLED")));
        container.setItem(11, createGlass(LifestealConfig.beaconRecipeEnabled ? Items.LIME_CONCRETE : Items.RED_CONCRETE,
                "§7Beacon Crafting: " + (LifestealConfig.beaconRecipeEnabled ? "§aENABLED" : "§cDISABLED")));
        
        container.setItem(13, createGlass(Items.ANVIL,
                "§6Max Hearts to Craft: §e" + maxCraftHearts + " hearts §7(Click to Edit)"));
        container.setItem(14, createGlass(Items.NETHERITE_INGOT,
                "§cAbsolute Max Hearts: §e" + absoluteMaxHearts + " hearts §7(Command only)"));
        
        container.setItem(22, createGlass(Items.ARROW, "§eBack to Menu"));
    }

    public static void openSettingsMenu(ServerPlayer player) {
        SettingsContainer container = new SettingsContainer();
        updateSettingsMenu(container);
        
        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x3, id, inv, container, 3),
            Component.literal("Lifesteal Recipe Settings")
        ));
    }

    public static void updateMaxHeartsAdjuster(ValueAdjusterContainer container) {
        for (int i = 0; i < 27; i++) container.setItem(i, createGlass(Items.GRAY_STAINED_GLASS_PANE, " "));

        double currentHearts = LifestealConfig.maxHeartsToCraft / 2.0;

        container.setItem(10, createGlass(Items.REDSTONE_BLOCK, "§c-1 Heart (-2 HP)"));
        container.setItem(11, createGlass(Items.RED_STAINED_GLASS_PANE, "§e-0.5 Heart (-1 HP)"));

        ItemStack display = createGlass(Items.PAPER, "§eCurrent Max Craft Limit:");
        display.set(DataComponents.CUSTOM_NAME, Component.literal("§eLimit: §a" + currentHearts + " hearts"));
        container.setItem(13, display);

        container.setItem(15, createGlass(Items.LIME_STAINED_GLASS_PANE, "§e+0.5 Heart (+1 HP)"));
        container.setItem(16, createGlass(Items.EMERALD_BLOCK, "§a+1 Heart (+2 HP)"));
        container.setItem(22, createGlass(Items.ARROW, "§eReturn to Settings"));
    }

    public static void openMaxHeartsAdjuster(ServerPlayer player) {
        ValueAdjusterContainer container = new ValueAdjusterContainer("max");
        updateMaxHeartsAdjuster(container);
        
        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x3, id, inv, container, 3),
            Component.literal("Adjust Maximum Hearts for Crafting")
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