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

    public static class SettingsMainContainer extends SimpleContainer {
        public SettingsMainContainer() { super(27); }
    }

    public static class MaceLimitContainer extends SimpleContainer {
        public MaceLimitContainer() { super(27); }
    }

    public static void updateMainSettings(SettingsMainContainer container) {
        for (int i = 0; i < 27; i++) {
            container.setItem(i, createGlass(Items.GRAY_STAINED_GLASS_PANE, " "));
        }

        container.setItem(10, createToggleItem(LifestealConfig.totemDisabled, "Disable Totem of Undying", "Totems will not revive players"));
        container.setItem(11, createToggleItem(LifestealConfig.endCrystalDamageDisabled, "Disable End Crystal damage", "Crystals deal no damage to players or environment"));
        container.setItem(12, createToggleItem(LifestealConfig.respawnAnchorNetherOnly, "Respawn Anchor only in Nether", "Cannot be charged in Overworld or End"));
        container.setItem(13, createToggleItem(LifestealConfig.enderPearlDisabled, "Disable Ender Pearls", "Right-clicking does nothing"));
        container.setItem(14, createToggleItem(LifestealConfig.dragonEggEnderChestDisabled, "Dragon Egg cannot be put in Ender Chests", "Protects the egg from being hidden"));
        
        container.setItem(16, createMaceSettingsItem());
        container.setItem(22, createGlass(Items.BARRIER, "§cClose"));
    }

    public static void openMainSettings(ServerPlayer player) {
        SettingsMainContainer container = new SettingsMainContainer();
        updateMainSettings(container);

        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x3, id, inv, container, 3),
                Component.literal("Lifesteal Advanced Settings")
        ));
    }

    public static void updateMaceLimitAdjuster(MaceLimitContainer container) {
        for (int i = 0; i < 27; i++) {
            container.setItem(i, createGlass(Items.GRAY_STAINED_GLASS_PANE, " "));
        }

        container.setItem(10, createToggleItem(LifestealConfig.maceCraftingEnabled, "Enable Mace Crafting", "If disabled, maces cannot be crafted"));

        ItemStack display = createGlass(Items.PAPER, "§eRemaining Mace Crafts: §a" + LifestealConfig.maceCraftsRemaining);
        container.setItem(13, display);

        container.setItem(11, createGlass(Items.RED_STAINED_GLASS_PANE, "§c-1"));
        container.setItem(12, createGlass(Items.REDSTONE_BLOCK, "§c-5"));
        container.setItem(14, createGlass(Items.LIME_STAINED_GLASS_PANE, "§a+1"));
        container.setItem(15, createGlass(Items.EMERALD_BLOCK, "§a+5"));

        container.setItem(16, createToggleItem(LifestealConfig.broadcastMaceCraft, "Broadcast Mace Craft", "Announce to all players when a mace is crafted"));
        container.setItem(22, createGlass(Items.ARROW, "§eBack to Main Settings"));
    }

    public static void openMaceLimitAdjuster(ServerPlayer player) {
        MaceLimitContainer container = new MaceLimitContainer();
        updateMaceLimitAdjuster(container);

        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x3, id, inv, container, 3),
                Component.literal("Mace Limit Settings")
        ));
    }

    private static ItemStack createGlass(Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(s -> s.withItalic(false)));
        return stack;
    }

    private static void setLore(ItemStack stack, String text) {
        stack.set(
            DataComponents.LORE,
            new ItemLore(List.of(
                Component.literal(text).withStyle(s -> s.withItalic(false))
            ))
        );
    }

    private static ItemStack createToggleItem(boolean enabled, String name, String description) {
        Item icon = enabled ? Items.LIME_CONCRETE : Items.RED_CONCRETE;
        String status = enabled ? "§aENABLED" : "§cDISABLED";
        ItemStack stack = new ItemStack(icon);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("§6" + name + " §7[" + status + "]").withStyle(s -> s.withItalic(false)));
        setLore(stack, "§7" + description);
        return stack;
    }

    private static ItemStack createMaceSettingsItem() {
        ItemStack stack = new ItemStack(Items.MACE);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("§5Mace Settings").withStyle(s -> s.withItalic(false)));
        setLore(stack, "§7Click to configure mace crafting limit and broadcasts");
        return stack;
    }
}