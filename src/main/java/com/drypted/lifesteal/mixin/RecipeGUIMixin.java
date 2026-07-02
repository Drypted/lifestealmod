package com.drypted.lifesteal.mixin;

import com.drypted.lifesteal.config.LifestealConfig;
import com.drypted.lifesteal.config.LifestealConfigManager;
import com.drypted.lifesteal.gui.RecipeGUI;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class RecipeGUIMixin {

    @Shadow public ServerPlayer player;

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void interceptRecipeMenus(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (!(this.player.containerMenu instanceof ChestMenu chestMenu)) return;

        // ---- Main menu: pure button panel, every click is cancelled. ----
        if (chestMenu.getContainer() instanceof RecipeGUI.MainMenuContainer) {
            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);

            int slot = packet.slotNum();
            if (slot == RecipeGUI.HEART_BUTTON_SLOT) {
                this.player.level().getServer().execute(() -> RecipeGUI.openRecipeEditor(this.player, "heart"));
            } else if (slot == RecipeGUI.BEACON_BUTTON_SLOT) {
                this.player.level().getServer().execute(() -> RecipeGUI.openRecipeEditor(this.player, "beacon"));
            } else {
                chestMenu.sendAllDataToRemote();
            }
            return;
        }

        // ---- Recipe editor: also a fully controlled panel. Every click is cancelled and the
        // cursor is forced empty, so no real inventory item is ever moved into this transient
        // container (which is what used to make items vanish). Ingredients are chosen via a
        // "select then stamp" flow using count-1 ghost copies only. ----
        if (chestMenu.getContainer() instanceof RecipeGUI.EditorContainer editor) {
            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);

            int slot = packet.slotNum();

            if (slot == RecipeGUI.SAVE_SLOT) {
                saveRecipe(editor);
                this.player.level().getServer().execute(() -> RecipeGUI.openMainMenu(this.player));
                return;
            }

            if (slot == RecipeGUI.BACK_SLOT || slot == RecipeGUI.DISCARD_SLOT) {
                this.player.level().getServer().execute(() -> RecipeGUI.openMainMenu(this.player));
                return;
            }

            if (RecipeGUI.isGridSlot(slot)) {
                ItemStack selected = editor.getSelected();
                editor.setItem(slot, selected.isEmpty() ? ItemStack.EMPTY : selected.copyWithCount(1));
                chestMenu.sendAllDataToRemote();
                return;
            }

            if (slot == RecipeGUI.BRUSH_SLOT) {
                editor.setSelected(ItemStack.EMPTY);
                RecipeGUI.updateBrush(editor);
                chestMenu.sendAllDataToRemote();
                return;
            }

            // A slot in the player's own inventory: pick that item as the current ingredient
            // (a ghost copy only — the real item stays where it is).
            if (slot >= RecipeGUI.CONTAINER_SIZE && slot < chestMenu.slots.size()) {
                ItemStack clicked = chestMenu.getSlot(slot).getItem();
                editor.setSelected(clicked.isEmpty() ? ItemStack.EMPTY : clicked);
                RecipeGUI.updateBrush(editor);
                chestMenu.sendAllDataToRemote();
                return;
            }

            // Decorative slot or a click outside the window: just resync to kill any client-side ghost.
            chestMenu.sendAllDataToRemote();
            return;
        }
    }

    private void saveRecipe(RecipeGUI.EditorContainer editor) {
        ItemStack[] targetMatrix = editor.getTarget().equals("heart")
                ? LifestealConfig.heartRecipeMatrix
                : LifestealConfig.beaconRecipeMatrix;

        for (int i = 0; i < 9; i++) {
            ItemStack cell = editor.getItem(RecipeGUI.GRID_SLOTS[i]);
            targetMatrix[i] = cell.isEmpty() ? ItemStack.EMPTY : cell.copyWithCount(1);
        }

        MinecraftServer server = this.player.level().getServer();
        this.player.sendSystemMessage(Component.literal(LifestealConfig.messagePrefix + "§aRecipe saved successfully."));
        LifestealConfigManager.save(server);

        // Refresh any open menus so the updated recipe is reflected immediately.
        if (server != null) {
            server.getPlayerList().getPlayers().forEach(serverPlayer -> {
                if (serverPlayer.containerMenu != null) {
                    serverPlayer.containerMenu.slotsChanged(serverPlayer.getInventory());
                    serverPlayer.containerMenu.broadcastChanges();
                }
            });
        }
    }
}
