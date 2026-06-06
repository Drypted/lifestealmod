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
            chestMenu.sendAllDataToRemote();
            
            int slot = packet.slotNum();
            if (slot == 11) this.player.level().getServer().execute(() -> RecipeGUI.openRecipeEditor(this.player, "heart"));
            if (slot == 13) this.player.level().getServer().execute(() -> RecipeGUI.openRecipeEditor(this.player, "beacon"));
            if (slot == 15) this.player.level().getServer().execute(() -> RecipeGUI.openSettingsMenu(this.player));
            return;
        }

        // --- 2. SETTINGS MENU HANDLING (updated) ---
        if (chestMenu.getContainer() instanceof RecipeGUI.SettingsContainer) {
            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);
            chestMenu.sendAllDataToRemote();

            int slot = packet.slotNum();
            if (slot == 10) {
                LifestealConfig.heartRecipeEnabled = !LifestealConfig.heartRecipeEnabled;
                this.player.level().getServer().execute(() -> RecipeGUI.openSettingsMenu(this.player));
            } else if (slot == 11) {
                LifestealConfig.beaconRecipeEnabled = !LifestealConfig.beaconRecipeEnabled;
                this.player.level().getServer().execute(() -> RecipeGUI.openSettingsMenu(this.player));
            } else if (slot == 13) {
                this.player.level().getServer().execute(() -> RecipeGUI.openMaxHeartsAdjuster(this.player));
            } else if (slot == 22) {
                this.player.level().getServer().execute(() -> RecipeGUI.openMainMenu(this.player));
            }
            return;
        }

        // --- 3. VALUE ADJUSTER (for max hearts to craft) ---
        if (chestMenu.getContainer() instanceof RecipeGUI.ValueAdjusterContainer adjuster) {
            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);
            chestMenu.sendAllDataToRemote();

            int slot = packet.slotNum();
            double currentHearts = LifestealConfig.maxHeartsToCraft / 2.0;
            double absoluteMaxHearts = LifestealConfig.maxHearts / 2.0;
            
            // Minimum crafting limit is 2 hearts (can't be lower than that)
            double minCraftLimit = 2.0;

            if (slot == 10) {
                // -1 Heart
                currentHearts = Math.max(minCraftLimit, currentHearts - 1.0);
            } else if (slot == 11) {
                // -0.5 Heart
                currentHearts = Math.max(minCraftLimit, currentHearts - 0.5);
            } else if (slot == 15) {
                // +0.5 Heart
                currentHearts = Math.min(absoluteMaxHearts, currentHearts + 0.5);
            } else if (slot == 16) {
                // +1 Heart
                currentHearts = Math.min(absoluteMaxHearts, currentHearts + 1.0);
            } else if (slot == 22) {
                // Return to settings
                this.player.level().getServer().execute(() -> RecipeGUI.openSettingsMenu(this.player));
                return;
            } else {
                return;
            }

            // Update the config value (convert hearts back to HP)
            LifestealConfig.maxHeartsToCraft = currentHearts * 2.0;
            
            // Save config to disk
            com.drypted.lifesteal.config.LifestealConfigManager.save(this.player.level().getServer());
            
            // Refresh the adjuster GUI
            this.player.level().getServer().execute(() -> RecipeGUI.openMaxHeartsAdjuster(this.player));
            return;
        }

        // --- 4. RECIPE MATRIX EDITOR HANDLING (unchanged except save triggers config save) ---
        if (chestMenu.getContainer() instanceof RecipeGUI.EditorContainer editor) {
            int slot = packet.slotNum();
            if (slot < 0 || slot >= editor.getContainerSize()) return;

            int[] editableSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
            boolean isGrid = Arrays.stream(editableSlots).anyMatch(x -> x == slot);

            if (isGrid) return;

            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);
            chestMenu.sendAllDataToRemote();

            if (slot == 45) { // SAVE
                ItemStack[] targetMatrix = editor.getTarget().equals("heart") ? LifestealConfig.heartRecipeMatrix : LifestealConfig.beaconRecipeMatrix;
                for (int i = 0; i < 9; i++) {
                    targetMatrix[i] = editor.getItem(editableSlots[i]).copy();
                }
                this.player.sendSystemMessage(Component.literal("§aCustom matrix adjustments saved."));
                // Save config to disk
                com.drypted.lifesteal.config.LifestealConfigManager.save(this.player.level().getServer());
                this.player.level().getServer().execute(() -> RecipeGUI.openMainMenu(this.player));
            } else if (slot == 49 || slot == 53) {
                this.player.level().getServer().execute(() -> RecipeGUI.openMainMenu(this.player));
            }
        }
    }
}