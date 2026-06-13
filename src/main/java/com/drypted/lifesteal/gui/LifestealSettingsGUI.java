package com.drypted.lifesteal.gui;

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
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

public class LifestealSettingsGUI {

    // ---------------- CATEGORY CONTAINERS ----------------
    public static class MainMenuContainer extends SimpleContainer {
        public MainMenuContainer() { super(27); }
    }
    public static class MaceSettingsContainer extends SimpleContainer {
        public MaceSettingsContainer() { super(27); }
    }
    public static class HeartSettingsContainer extends SimpleContainer {
        public HeartSettingsContainer() { super(27); }
    }
    public static class BeaconSettingsContainer extends SimpleContainer {
        public BeaconSettingsContainer() { super(27); }
    }
    public static class MiscSettingsContainer extends SimpleContainer {
        public MiscSettingsContainer() { super(27); }
    }
    public static class DoubleAdjusterContainer extends SimpleContainer {
        private final String targetField;
        public DoubleAdjusterContainer(String targetField) {
            super(27);
            this.targetField = targetField;
        }
        public String getTargetField() { return targetField; }
    }

    // ---------------- MAIN MENU ----------------
    public static void openMainMenu(ServerPlayer player) {
        MainMenuContainer container = new MainMenuContainer();
        fillGlass(container, Items.GRAY_STAINED_GLASS_PANE, " ");

        container.setItem(10, createIcon(Items.MACE, "§cMace Settings", "Limit mace crafts, enable/disable crafting"));
        container.setItem(12, createIcon(Items.NETHER_STAR, "§cHeart Settings", "Max hearts, craft limit, revive amount"));
        container.setItem(14, createIcon(Items.BEACON, "§bRevive Beacon Settings", "Enable/disable beacon recipe"));
        container.setItem(16, createIcon(Items.COMPARATOR, "§eMisc Settings", "Totem, end crystals, ender pearls, etc."));

        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x3, id, inv, container, 3),
            Component.literal("Lifesteal Settings")
        ));
    }

    // ---------------- MACE SETTINGS ----------------
    public static void openMaceSettings(ServerPlayer player) {
        MaceSettingsContainer container = new MaceSettingsContainer();
        fillGlass(container, Items.GRAY_STAINED_GLASS_PANE, " ");
        container.setItem(10, createToggleItem(LifestealConfig.maceCraftingEnabled, "Enable Mace Crafting", "If disabled, maces cannot be crafted"));
        container.setItem(12, createDisplayItem(Items.PAPER, "§eRemaining Mace Crafts", "§a" + LifestealConfig.maceCraftsRemaining));
        container.setItem(14, createGlass(Items.RED_STAINED_GLASS_PANE, "§c-1"));
        container.setItem(15, createGlass(Items.REDSTONE_BLOCK, "§c-5"));
        container.setItem(16, createGlass(Items.LIME_STAINED_GLASS_PANE, "§a+1"));
        container.setItem(17, createGlass(Items.EMERALD_BLOCK, "§a+5"));
        container.setItem(22, createToggleItem(LifestealConfig.broadcastMaceCraft, "Broadcast Mace Craft", "Announce to all players when mace is crafted"));
        container.setItem(26, createGlass(Items.ARROW, "§eBack"));
        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x3, id, inv, container, 3),
            Component.literal("Mace Settings")
        ));
    }

    // ---------------- HEART SETTINGS ----------------
    public static void openHeartSettings(ServerPlayer player) {
        HeartSettingsContainer container = new HeartSettingsContainer();
        fillGlass(container, Items.GRAY_STAINED_GLASS_PANE, " ");
        container.setItem(10, createToggleItem(LifestealConfig.heartRecipeEnabled, "Heart Recipe Enabled", "Allow crafting of heart items"));
        container.setItem(11, createAdjustableItem("maxHeartsToCraft", "Max hearts to craft", "Players with ≥ this cannot craft hearts", LifestealConfig.maxHeartsToCraft / 2.0));
        container.setItem(12, createAdjustableItem("maxHearts", "Absolute max hearts", "Hard cap for any player", LifestealConfig.maxHearts / 2.0));
        container.setItem(13, createAdjustableItem("reviveAtHearts", "Revive at hearts", "Hearts given when revived", LifestealConfig.reviveAtHearts / 2.0));
        container.setItem(15, createToggleItem(LifestealConfig.banOnZeroHearts, "Ban on zero hearts", "Player is banned when hearts reach 0"));
        container.setItem(16, createToggleItem(LifestealConfig.loseHeartsByNaturalCauses, "Lose hearts by natural causes", "Fall/void/drowning reduce max hearts"));
        container.setItem(17, createToggleItem(LifestealConfig.broadcastElimination, "Broadcast elimination", "Announce when a player is eliminated"));
        container.setItem(26, createGlass(Items.ARROW, "§eBack"));
        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x3, id, inv, container, 3),
            Component.literal("Heart Settings")
        ));
    }

    // ---------------- BEACON SETTINGS ----------------
    public static void openBeaconSettings(ServerPlayer player) {
        BeaconSettingsContainer container = new BeaconSettingsContainer();
        fillGlass(container, Items.GRAY_STAINED_GLASS_PANE, " ");
        container.setItem(13, createToggleItem(LifestealConfig.beaconRecipeEnabled, "Beacon Recipe Enabled", "Allow crafting of revive beacons"));
        container.setItem(26, createGlass(Items.ARROW, "§eBack"));
        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x3, id, inv, container, 3),
            Component.literal("Revive Beacon Settings")
        ));
    }

    // ---------------- MISC SETTINGS ----------------
    public static void openMiscSettings(ServerPlayer player) {
        MiscSettingsContainer container = new MiscSettingsContainer();
        fillGlass(container, Items.GRAY_STAINED_GLASS_PANE, " ");
        container.setItem(10, createToggleItem(LifestealConfig.totemDisabled, "Disable Totem of Undying", "Totems will not revive players"));
        container.setItem(11, createToggleItem(LifestealConfig.endCrystalDamageDisabled, "Disable End Crystal damage", "Crystals deal no damage"));
        container.setItem(12, createToggleItem(LifestealConfig.respawnAnchorNetherOnly, "Respawn Anchor only in Nether", "Cannot be charged in Overworld/End"));
        container.setItem(13, createToggleItem(LifestealConfig.enderPearlDisabled, "Disable Ender Pearls", "Right‑click does nothing"));
        container.setItem(14, createToggleItem(LifestealConfig.dragonEggEnderChestDisabled, "Dragon Egg cannot be put in Ender Chests", "Protects the egg"));
        container.setItem(26, createGlass(Items.ARROW, "§eBack"));
        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x3, id, inv, container, 3),
            Component.literal("Misc Settings")
        ));
    }

    // ---------------- DOUBLE ADJUSTER (unchanged) ----------------
    public static void openDoubleAdjuster(ServerPlayer player, String fieldName, double minValue, double maxValue) {
        DoubleAdjusterContainer container = new DoubleAdjusterContainer(fieldName);
        fillGlass(container, Items.GRAY_STAINED_GLASS_PANE, " ");
        double current = getCurrentFieldValueStatic(fieldName);
        container.setItem(10, createGlass(Items.REDSTONE_BLOCK, "§c-1 Heart (-2 HP)"));
        container.setItem(11, createGlass(Items.RED_STAINED_GLASS_PANE, "§e-0.5 Heart (-1 HP)"));
        container.setItem(13, createGlass(Items.PAPER, "§eCurrent " + fieldName + ": §a" + current + " hearts"));
        container.setItem(15, createGlass(Items.LIME_STAINED_GLASS_PANE, "§e+0.5 Heart (+1 HP)"));
        container.setItem(16, createGlass(Items.EMERALD_BLOCK, "§a+1 Heart (+2 HP)"));
        container.setItem(22, createGlass(Items.ARROW, "§eReturn to Settings"));
        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x3, id, inv, container, 3),
            Component.literal("Adjust " + fieldName)
        ));
    }

    public static void updateDoubleAdjuster(DoubleAdjusterContainer container, String fieldName) {
        double current = getCurrentFieldValueStatic(fieldName);
        container.setItem(13, createGlass(Items.PAPER, "§eCurrent " + fieldName + ": §a" + current + " hearts"));
    }

    public static double getCurrentFieldValueStatic(String fieldName) {
        switch (fieldName) {
            case "maxHeartsToCraft": return LifestealConfig.maxHeartsToCraft / 2.0;
            case "reviveAtHearts":  return LifestealConfig.reviveAtHearts / 2.0;
            case "maxHearts":       return LifestealConfig.maxHearts / 2.0;
            default: return 0;
        }
    }

    // ---------------- HELPER METHODS ----------------
    private static void fillGlass(SimpleContainer container, Item glassType, String name) {
        for (int i = 0; i < container.getContainerSize(); i++)
            container.setItem(i, createGlass(glassType, name));
    }
    private static ItemStack createGlass(Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(s -> s.withItalic(false)));
        return stack;
    }
    private static ItemStack createIcon(Item item, String name, String lore) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(s -> s.withItalic(false)));
        stack.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("§7" + lore).withStyle(s -> s.withItalic(false)))));
        return stack;
    }
    private static ItemStack createToggleItem(boolean enabled, String name, String description) {
        Item icon = enabled ? Items.LIME_CONCRETE : Items.RED_CONCRETE;
        String status = enabled ? "§aENABLED" : "§cDISABLED";
        ItemStack stack = new ItemStack(icon);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("§6" + name + " §7[" + status + "]").withStyle(s -> s.withItalic(false)));
        stack.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("§7" + description).withStyle(s -> s.withItalic(false)))));
        return stack;
    }
    private static ItemStack createAdjustableItem(String fieldKey, String name, String description, double currentValue) {
        ItemStack stack = new ItemStack(Items.COMPARATOR);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("§6" + name + " §7[§e" + currentValue + " hearts§7]").withStyle(s -> s.withItalic(false)));
        stack.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("§7" + description).withStyle(s -> s.withItalic(false)), Component.literal("§eClick to adjust"))));
        return stack;
    }
    private static ItemStack createDisplayItem(Item item, String name, String value) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(s -> s.withItalic(false)));
        stack.set(DataComponents.LORE, new ItemLore(List.of(Component.literal(value).withStyle(s -> s.withItalic(false)))));
        return stack;
    }
}