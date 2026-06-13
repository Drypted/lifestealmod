package com.drypted.lifesteal.mixin;

import com.drypted.lifesteal.config.LifestealConfig;
import com.drypted.lifesteal.gui.RecipeGUI;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(ServerGamePacketListenerImpl.class)
public class RecipeGUIMixin {

    @Shadow public ServerPlayer player;

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void interceptSecureRecipeMenus(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (!(this.player.containerMenu instanceof ChestMenu chestMenu)) return;

        // --- 1. MAIN MENU HANDLING ---
        if (chestMenu.getContainer() instanceof RecipeGUI.MainMenuContainer) {
            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);
            
            int slot = packet.slotNum();
            if (slot == 11) this.player.level().getServer().execute(() -> RecipeGUI.openRecipeEditor(this.player, "heart"));
            else if (slot == 13) this.player.level().getServer().execute(() -> RecipeGUI.openRecipeEditor(this.player, "beacon"));
            else if (slot == 15) this.player.level().getServer().execute(() -> RecipeGUI.openSettingsMenu(this.player));

            chestMenu.sendAllDataToRemote(); // Deletes ghost items
            return;
        }

        // --- 2. SETTINGS MENU HANDLING ---
        if (chestMenu.getContainer() instanceof RecipeGUI.SettingsContainer container) {
            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);

            int slot = packet.slotNum();
            boolean updated = false;

            if (slot == 10) { LifestealConfig.heartRecipeEnabled = !LifestealConfig.heartRecipeEnabled; updated = true; } 
            else if (slot == 11) { LifestealConfig.beaconRecipeEnabled = !LifestealConfig.beaconRecipeEnabled; updated = true; } 
            else if (slot == 13) {
                this.player.level().getServer().execute(() -> RecipeGUI.openMaxHeartsAdjuster(this.player));
                return;
            } else if (slot == 22) {
                this.player.level().getServer().execute(() -> RecipeGUI.openMainMenu(this.player));
                return;
            }
            
            if (updated) {
                RecipeGUI.updateSettingsMenu(container);
            }

            chestMenu.sendAllDataToRemote(); // ALWAYS send to clean ghost items
            return;
        }

        // --- 3. VALUE ADJUSTER (for max hearts to craft) ---
        if (chestMenu.getContainer() instanceof RecipeGUI.ValueAdjusterContainer container) {
            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);

            int slot = packet.slotNum();
            double currentHearts = LifestealConfig.maxHeartsToCraft / 2.0;
            double absoluteMaxHearts = LifestealConfig.maxHearts / 2.0;
            double minCraftLimit = 2.0;
            boolean updated = false;

            if (slot == 10) { currentHearts = Math.max(minCraftLimit, currentHearts - 1.0); updated = true; } 
            else if (slot == 11) { currentHearts = Math.max(minCraftLimit, currentHearts - 0.5); updated = true; } 
            else if (slot == 15) { currentHearts = Math.min(absoluteMaxHearts, currentHearts + 0.5); updated = true; } 
            else if (slot == 16) { currentHearts = Math.min(absoluteMaxHearts, currentHearts + 1.0); updated = true; } 
            else if (slot == 22) {
                this.player.level().getServer().execute(() -> RecipeGUI.openSettingsMenu(this.player));
                return;
            }

            if (updated) {
                LifestealConfig.maxHeartsToCraft = currentHearts * 2.0;
                com.drypted.lifesteal.config.LifestealConfigManager.save(this.player.level().getServer());
                RecipeGUI.updateMaxHeartsAdjuster(container);
            }

            chestMenu.sendAllDataToRemote(); // Ghost item fix
            return;
        }

        // --- 4. RECIPE MATRIX EDITOR HANDLING ---
        if (chestMenu.getContainer() instanceof RecipeGUI.EditorContainer editor) {
            int slot = packet.slotNum();
            if (slot < 0 || slot >= editor.getContainerSize()) return;

            int[] editableSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
            boolean isGrid = Arrays.stream(editableSlots).anyMatch(x -> x == slot);

            if (isGrid) return; // Allow normal dragging here

            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);
            
            if (slot == 45) { // SAVE
                ItemStack[] targetMatrix = editor.getTarget().equals("heart") ? LifestealConfig.heartRecipeMatrix : LifestealConfig.beaconRecipeMatrix;
                for (int i = 0; i < 9; i++) {
                    targetMatrix[i] = editor.getItem(editableSlots[i]).copy();
                }
                this.player.sendOverlayMessage(Component.literal("§aRecipe saved."));
                com.drypted.lifesteal.config.LifestealConfigManager.save(this.player.level().getServer());
                this.player.level().getServer().execute(() -> RecipeGUI.openMainMenu(this.player));
            } else if (slot == 49 || slot == 53) {
                this.player.level().getServer().execute(() -> RecipeGUI.openMainMenu(this.player));
            }

            chestMenu.sendAllDataToRemote(); // Important for the non-grid slots
        }
    }
}