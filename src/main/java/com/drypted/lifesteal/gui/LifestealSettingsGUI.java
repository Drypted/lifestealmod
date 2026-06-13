package com.drypted.lifesteal.gui;

import java.util.List;
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

public class LifestealSettingsGUI {

    // Main settings container (5 rows = 45 slots)
    public static class MainSettingsContainer extends SimpleContainer {
        public MainSettingsContainer() { super(45); }
    }

    // Reusable adjuster for double values (craft limit, revive hearts, max hearts)
    public static class DoubleAdjusterContainer extends SimpleContainer {
        private final String targetField;
        public DoubleAdjusterContainer(String targetField) {
            super(27);
            this.targetField = targetField;
        }
        public String getTargetField() { return targetField; }
    }

    // ------------------------------------------------------------
    //  Main Settings GUI builder
    // ------------------------------------------------------------
    public static void updateMainSettings(MainSettingsContainer container) {
        // Fill with gray panes
        for (int i = 0; i < 45; i++)
            container.setItem(i, createGlass(Items.GRAY_STAINED_GLASS_PANE, " "));

        // === Row 1: Core toggles (slots 10,11,12) ===
        container.setItem(10, createToggleItem(LifestealConfig.banOnZeroHearts,
                "Ban on zero hearts", "Player is banned when hearts reach 0"));
        container.setItem(11, createToggleItem(LifestealConfig.loseHeartsByNaturalCauses,
                "Lose hearts by natural causes", "Fall/void/drowning etc. reduce max hearts"));
        container.setItem(12, createToggleItem(LifestealConfig.broadcastElimination,
                "Broadcast elimination", "Announce when a player is eliminated"));

        // === Row 2: Recipe toggles & craft limit (slots 19,20,21,22) ===
        container.setItem(19, createToggleItem(LifestealConfig.heartRecipeEnabled,
                "Heart recipe enabled", "Allow crafting of heart items"));
        container.setItem(20, createToggleItem(LifestealConfig.beaconRecipeEnabled,
                "Beacon recipe enabled", "Allow crafting of revive beacons"));
        container.setItem(21, createAdjustableItem("maxHeartsToCraft",
                "Max hearts to craft", "Players with ≥ this cannot craft hearts",
                LifestealConfig.maxHeartsToCraft / 2.0));
        container.setItem(22, createAdjustableItem("reviveAtHearts",
                "Revive at hearts", "Hearts given when revived",
                LifestealConfig.reviveAtHearts / 2.0));

        // === Row 3: Absolute max hearts (slot 31) ===
        container.setItem(31, createAdjustableItem("maxHearts",
                "Absolute max hearts", "Hard cap for any player",
                LifestealConfig.maxHearts / 2.0));

        // === Row 4: Item restrictions (slots 28,29,30,32,33) ===
        container.setItem(28, createToggleItem(LifestealConfig.totemDisabled,
                "Disable Totem of Undying", "Totems will not revive players"));
        container.setItem(29, createToggleItem(LifestealConfig.endCrystalDamageDisabled,
                "Disable End Crystal damage", "Crystals deal no damage"));
        container.setItem(30, createToggleItem(LifestealConfig.respawnAnchorNetherOnly,
                "Respawn Anchor only in Nether", "Cannot be charged in Overworld/End"));
        container.setItem(32, createToggleItem(LifestealConfig.enderPearlDisabled,
                "Disable Ender Pearls", "Right‑click does nothing"));
        container.setItem(33, createToggleItem(LifestealConfig.dragonEggEnderChestDisabled,
                "Dragon Egg cannot be put in Ender Chests", "Protects the egg"));

        // === Row 5: Mace settings + close (slots 37-44) ===
        container.setItem(37, createToggleItem(LifestealConfig.maceCraftingEnabled,
                "Enable Mace Crafting", "If disabled, maces cannot be crafted"));
        container.setItem(38, createGlass(Items.PAPER,
                "§eRemaining Mace Crafts: §a" + LifestealConfig.maceCraftsRemaining));
        container.setItem(39, createGlass(Items.RED_STAINED_GLASS_PANE, "§c-1"));
        container.setItem(40, createGlass(Items.REDSTONE_BLOCK, "§c-5"));
        container.setItem(41, createGlass(Items.LIME_STAINED_GLASS_PANE, "§a+1"));
        container.setItem(42, createGlass(Items.EMERALD_BLOCK, "§a+5"));
        container.setItem(43, createToggleItem(LifestealConfig.broadcastMaceCraft,
                "Broadcast Mace Craft", "Announce to all players when a mace is crafted"));
        container.setItem(44, createGlass(Items.BARRIER, "§cClose"));
    }

    // Helper to create an item that opens a numeric adjuster
    private static ItemStack createAdjustableItem(String fieldKey, String name, String description, double currentValue) {
        ItemStack stack = new ItemStack(Items.COMPARATOR);
        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal("§6" + name + " §7[§e" + currentValue + " hearts§7]")
                        .withStyle(s -> s.withItalic(false)));
        setLore(stack, "§7" + description + "\n§eClick to adjust");
        return stack;
    }

    public static void openMainSettings(ServerPlayer player) {
        MainSettingsContainer container = new MainSettingsContainer();
        updateMainSettings(container);
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x5, id, inv, container, 5),
                Component.literal("Lifesteal Settings")
        ));
    }

    // ------------------------------------------------------------
    //  Double adjuster GUI (generic for any double field)
    // ------------------------------------------------------------
    public static void openDoubleAdjuster(ServerPlayer player, String fieldName, double minValue, double maxValue) {
        DoubleAdjusterContainer container = new DoubleAdjusterContainer(fieldName);
        updateDoubleAdjuster(container, fieldName, minValue, maxValue);
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x3, id, inv, container, 3),
                Component.literal("Adjust " + fieldName)
        ));
    }

    public static void updateDoubleAdjuster(DoubleAdjusterContainer container, String fieldName, double minValue, double maxValue) {
        for (int i = 0; i < 27; i++)
            container.setItem(i, createGlass(Items.GRAY_STAINED_GLASS_PANE, " "));

        double current = getCurrentFieldValueStatic(fieldName);
        container.setItem(10, createGlass(Items.REDSTONE_BLOCK, "§c-1 Heart (-2 HP)"));
        container.setItem(11, createGlass(Items.RED_STAINED_GLASS_PANE, "§e-0.5 Heart (-1 HP)"));
        ItemStack display = createGlass(Items.PAPER, "§eCurrent " + fieldName + ": §a" + current + " hearts");
        container.setItem(13, display);
        container.setItem(15, createGlass(Items.LIME_STAINED_GLASS_PANE, "§e+0.5 Heart (+1 HP)"));
        container.setItem(16, createGlass(Items.EMERALD_BLOCK, "§a+1 Heart (+2 HP)"));
        container.setItem(22, createGlass(Items.ARROW, "§eReturn to Settings"));
    }

    // Static helper for mixin to read current value
    public static double getCurrentFieldValueStatic(String fieldName) {
        switch (fieldName) {
            case "maxHeartsToCraft": return LifestealConfig.maxHeartsToCraft / 2.0;
            case "reviveAtHearts":  return LifestealConfig.reviveAtHearts / 2.0;
            case "maxHearts":       return LifestealConfig.maxHearts / 2.0;
            default: return 0;
        }
    }

    // ------------------------------------------------------------
    //  Common helper methods
    // ------------------------------------------------------------
    private static ItemStack createGlass(Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(s -> s.withItalic(false)));
        return stack;
    }

    private static void setLore(ItemStack stack, String text) {
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal(text).withStyle(s -> s.withItalic(false))
        )));
    }

    private static ItemStack createToggleItem(boolean enabled, String name, String description) {
        Item icon = enabled ? Items.LIME_CONCRETE : Items.RED_CONCRETE;
        String status = enabled ? "§aENABLED" : "§cDISABLED";
        ItemStack stack = new ItemStack(icon);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("§6" + name + " §7[" + status + "]")
                .withStyle(s -> s.withItalic(false)));
        setLore(stack, "§7" + description);
        return stack;
    }
}