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

public class LifestealSettingsGUI {

    // ----- Containers -----
    public static class SettingsMainContainer extends SimpleContainer {
        public SettingsMainContainer() { super(27); }
    }

    public static class MaceLimitContainer extends SimpleContainer {
        public MaceLimitContainer() { super(27); }
    }

    // ----- Open main settings GUI -----
    public static void openMainSettings(ServerPlayer player) {
        SettingsMainContainer container = new SettingsMainContainer();
        // Fill with gray panes
        for (int i = 0; i < 27; i++) {
            container.setItem(i, createGlass(Items.GRAY_STAINED_GLASS_PANE, " "));
        }

        // Toggle: Disable Totem of Undying
        container.setItem(10, createToggleItem(
                LifestealConfig.totemDisabled,
                "Disable Totem of Undying",
                "Totems will not revive players"
        ));

        // Toggle: Disable End Crystal damage
        container.setItem(11, createToggleItem(
                LifestealConfig.endCrystalDamageDisabled,
                "Disable End Crystal damage",
                "Crystals deal no damage to players or environment"
        ));

        // Toggle: Respawn Anchor only in Nether
        container.setItem(12, createToggleItem(
                LifestealConfig.respawnAnchorNetherOnly,
                "Respawn Anchor only in Nether",
                "Cannot be charged in Overworld or End"
        ));

        // Toggle: Disable Ender Pearls
        container.setItem(13, createToggleItem(
                LifestealConfig.enderPearlDisabled,
                "Disable Ender Pearls",
                "Right-clicking does nothing"
        ));

        // Toggle: Dragon Egg not allowed in Ender Chests
        container.setItem(14, createToggleItem(
                LifestealConfig.dragonEggEnderChestDisabled,
                "Dragon Egg cannot be put in Ender Chests",
                "Protects the egg from being hidden"
        ));

        // Mace settings (opens submenu)
        container.setItem(16, createMaceSettingsItem());

        // Back button? Not needed, just close with ESC. But add a close button if desired.
        container.setItem(22, createGlass(Items.BARRIER, "§cClose"));

        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x3, id, inv, container, 3),
                Component.literal("Lifesteal Advanced Settings")
        ));
    }

    // ----- Open mace limit adjuster -----
    public static void openMaceLimitAdjuster(ServerPlayer player) {
        MaceLimitContainer container = new MaceLimitContainer();
        for (int i = 0; i < 27; i++) {
            container.setItem(i, createGlass(Items.GRAY_STAINED_GLASS_PANE, " "));
        }

        // Mace crafting toggle
        container.setItem(10, createToggleItem(
                LifestealConfig.maceCraftingEnabled,
                "Enable Mace Crafting",
                "If disabled, maces cannot be crafted"
        ));

        // Remaining crafts display and adjust buttons
        ItemStack display = createGlass(Items.PAPER, "§eRemaining Mace Crafts: §a" + LifestealConfig.maceCraftsRemaining);
        container.setItem(13, display);

        container.setItem(11, createGlass(Items.RED_STAINED_GLASS_PANE, "§c-1"));
        container.setItem(12, createGlass(Items.REDSTONE_BLOCK, "§c-5"));
        container.setItem(14, createGlass(Items.LIME_STAINED_GLASS_PANE, "§a+1"));
        container.setItem(15, createGlass(Items.EMERALD_BLOCK, "§a+5"));

        // Broadcast toggle
        container.setItem(16, createToggleItem(
                LifestealConfig.broadcastMaceCraft,
                "Broadcast Mace Craft",
                "Announce to all players when a mace is crafted"
        ));

        // Back button
        container.setItem(22, createGlass(Items.ARROW, "§eBack to Main Settings"));

        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x3, id, inv, container, 3),
                Component.literal("Mace Limit Settings")
        ));
    }

    // ----- Helper methods -----
    private static ItemStack createGlass(Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(s -> s.withItalic(false)));
        return stack;
    }

    private static ItemStack createToggleItem(boolean enabled, String name, String description) {
        Item icon = enabled ? Items.LIME_CONCRETE : Items.RED_CONCRETE;
        String status = enabled ? "§aENABLED" : "§cDISABLED";
        ItemStack stack = new ItemStack(icon);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("§6" + name + " §7[" + status + "]").withStyle(s -> s.withItalic(false)));
        stack.set(DataComponents.LORE, Component.literal("§7" + description).withStyle(s -> s.withItalic(false)));
        return stack;
    }

    private static ItemStack createMaceSettingsItem() {
        ItemStack stack = new ItemStack(Items.MACE);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("§5Mace Settings").withStyle(s -> s.withItalic(false)));
        stack.set(DataComponents.LORE, Component.literal("§7Click to configure mace crafting limit and broadcasts").withStyle(s -> s.withItalic(false)));
        return stack;
    }
}