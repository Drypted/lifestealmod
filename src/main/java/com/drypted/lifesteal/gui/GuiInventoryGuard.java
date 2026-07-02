package com.drypted.lifesteal.gui;

import com.drypted.lifesteal.config.LifestealConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Close-time safety net for the mod's admin GUIs.
 *
 * <p>The click handlers for these menus cancel every click, so no item can be dragged out. But other
 * packet paths (hotbar/off-hand swaps, keybind drops, creative slot-sets) can still race the cancel
 * and leave a real item somewhere the player controls. This guard closes that gap independently of
 * the packet path: it snapshots everything the player is holding when a guarded menu opens and, when
 * it closes, strips anything the player now holds in excess of that snapshot that is also an exact
 * copy of one of the menu's display items. A guarded panel never legitimately hands out items, so any
 * such net gain is an exploit; unrelated items and legitimate decreases (e.g. a consumed revive
 * beacon) are left alone.
 *
 * <p>"Everything the player is holding" deliberately reaches past {@code player.getInventory()} to
 * also cover the 2x2 crafting grid and the cursor/carried item — slots a creative slot-set
 * ({@code slotNum} 1-45) or an in-flight drag can reach but the plain inventory list does not.
 */
public final class GuiInventoryGuard {

    private static final Logger LOGGER = LoggerFactory.getLogger("Lifesteal/GuiGuard");

    // Snapshot of every player-controlled item slot taken when a guarded menu opened.
    private static final Map<UUID, List<ItemStack>> BASELINES = new ConcurrentHashMap<>();

    private GuiInventoryGuard() {}

    public static void capture(ServerPlayer player) {
        BASELINES.put(player.getUUID(), snapshot(player));
    }

    /**
     * Compares everything the player holds now against the snapshot taken when the guarded menu
     * opened. Any item held in greater quantity than the baseline <em>and</em> that is an exact copy
     * of one of the menu's display items was pulled out of the panel — the surplus is stripped from
     * wherever it sits (inventory, crafting grid or cursor). The client is always resynced afterward
     * so a purely client-predicted "ghost" left by a cancelled interaction cannot be committed later.
     * No-op (and cheap) when no snapshot exists.
     */
    public static void validateAndClear(ServerPlayer player, Container guiContainer) {
        List<ItemStack> baseline = BASELINES.remove(player.getUUID());
        if (baseline == null) return;

        List<ItemStack> current = snapshot(player);
        List<ItemStack> processed = new ArrayList<>();
        int totalRemoved = 0;

        for (int i = 0; i < guiContainer.getContainerSize(); i++) {
            ItemStack display = guiContainer.getItem(i);
            if (display.isEmpty() || containsSame(processed, display)) continue;
            processed.add(display);

            int surplus = countMatching(current, display) - countMatching(baseline, display);
            if (surplus > 0) {
                totalRemoved += removeMatching(player, display, surplus);
            }
        }

        if (totalRemoved > 0) {
            player.sendSystemMessage(Component.literal(LifestealConfig.messagePrefix
                    + "§cThat action was blocked — " + totalRemoved
                    + " item" + (totalRemoved == 1 ? "" : "s") + " could not be taken from the menu."));
            LOGGER.warn("Reverted {} illegitimate item(s) gained by {} while an admin GUI was open (possible dupe attempt).",
                    totalRemoved, player.getGameProfile().name());
        }

        // Always overwrite the client's view of its own inventory + cursor so any predicted ghost
        // item from a cancelled interaction disappears and cannot be committed after close.
        player.inventoryMenu.sendAllDataToRemote();
    }

    public static void forget(UUID playerId) {
        BASELINES.remove(playerId);
    }

    /**
     * Snapshots every slot a player can hold an item in: the crafting grid, armor, main inventory,
     * hotbar and off-hand (InventoryMenu slots 1-45, skipping the derived result slot 0) plus the
     * cursor/carried item on whichever menu is currently open.
     */
    private static List<ItemStack> snapshot(ServerPlayer player) {
        InventoryMenu inventoryMenu = player.inventoryMenu;
        List<ItemStack> copy = new ArrayList<>(inventoryMenu.slots.size());
        for (int i = InventoryMenu.CRAFT_SLOT_START; i < inventoryMenu.slots.size(); i++) {
            ItemStack stack = inventoryMenu.getSlot(i).getItem();
            copy.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }
        ItemStack carried = player.containerMenu.getCarried();
        copy.add(carried.isEmpty() ? ItemStack.EMPTY : carried.copy());
        return copy;
    }

    private static boolean containsSame(List<ItemStack> stacks, ItemStack ref) {
        for (ItemStack stack : stacks) {
            if (ItemStack.isSameItemSameComponents(stack, ref)) return true;
        }
        return false;
    }

    private static int countMatching(List<ItemStack> stacks, ItemStack ref) {
        int total = 0;
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, ref)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static int removeMatching(ServerPlayer player, ItemStack ref, int amount) {
        int removed = 0;

        InventoryMenu inventoryMenu = player.inventoryMenu;
        for (int i = InventoryMenu.CRAFT_SLOT_START; i < inventoryMenu.slots.size() && removed < amount; i++) {
            Slot slot = inventoryMenu.getSlot(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, ref)) continue;

            int take = Math.min(stack.getCount(), amount - removed);
            ItemStack reduced = stack.copy();
            reduced.shrink(take);
            slot.set(reduced.isEmpty() ? ItemStack.EMPTY : reduced);
            removed += take;
        }

        if (removed < amount) {
            AbstractContainerMenu menu = player.containerMenu;
            ItemStack carried = menu.getCarried();
            if (!carried.isEmpty() && ItemStack.isSameItemSameComponents(carried, ref)) {
                int take = Math.min(carried.getCount(), amount - removed);
                ItemStack reduced = carried.copy();
                reduced.shrink(take);
                menu.setCarried(reduced.isEmpty() ? ItemStack.EMPTY : reduced);
                removed += take;
            }
        }

        return removed;
    }
}
