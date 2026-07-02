package com.drypted.lifesteal.gui;

/**
 * Marker for the mod's admin-panel containers (recipe editor, settings, enchantment caps,
 * revive menu). These panels are pure display/button surfaces: interacting with them must
 * never change the player's real inventory. Tagging them lets {@link GuiInventoryGuard}
 * snapshot the inventory on open and revert any illegitimate gains on close.
 */
public interface GuardedGuiContainer {
}
