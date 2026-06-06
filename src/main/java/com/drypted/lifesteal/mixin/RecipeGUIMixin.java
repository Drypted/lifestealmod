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

        // --- 2. SETTINGS MENU HANDLING ---
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
                LifestealConfig.limitHeartCraftingByHealth = !LifestealConfig.limitHeartCraftingByHealth;
                this.player.level().getServer().execute(() -> RecipeGUI.openSettingsMenu(this.player));
            } else if (slot == 14) {
                this.player.level().getServer().execute(() -> RecipeGUI.openValueAdjuster(this.player, "min"));
            } else if (slot == 15) {
                this.player.level().getServer().execute(() -> RecipeGUI.openValueAdjuster(this.player, "max"));
            } else if (slot == 22) {
                this.player.level().getServer().execute(() -> RecipeGUI.openMainMenu(this.player));
            }
            return;
        }

        // --- 3. VALUE INCREMENT/DECREMENT ADJ SUB-GUI ---
        if (chestMenu.getContainer() instanceof RecipeGUI.ValueAdjusterContainer adjuster) {
            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);
            chestMenu.sendAllDataToRemote();

            int slot = packet.slotNum();
            String target = adjuster.getTargetSetting();
            double currentVal = target.equals("min") ? LifestealConfig.minHeartsToCraft : LifestealConfig.maxHeartsToCraft;

            if (slot == 10) currentVal = Math.max(2.0, currentVal - 2.0);      // -1 Heart
            else if (slot == 11) currentVal = Math.max(2.0, currentVal - 1.0); // -0.5 Heart
            else if (slot == 15) currentVal = Math.min(200.0, currentVal + 1.0); // +0.5 Heart
            else if (slot == 16) currentVal = Math.min(200.0, currentVal + 2.0); // +1.0 Heart
            else if (slot == 22) {
                this.player.level().getServer().execute(() -> RecipeGUI.openSettingsMenu(this.player));
                return;
            } else {
                return;
            }

            if (target.equals("min")) LifestealConfig.minHeartsToCraft = currentVal;
            else LifestealConfig.maxHeartsToCraft = currentVal;

            this.player.level().getServer().execute(() -> RecipeGUI.openValueAdjuster(this.player, target));
            return;
        }

        // --- 4. RECIPE MATRIX EDITOR HANDLING ---
        if (chestMenu.getContainer() instanceof RecipeGUI.EditorContainer editor) {
            int slot = packet.slotNum();
            if (slot < 0 || slot >= editor.getContainerSize()) return;

            int[] editableSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
            boolean isGrid = Arrays.stream(editableSlots).anyMatch(x -> x == slot);

            if (isGrid) return; // Allow normal items to be moved here

            // Freeze background glass, functional buttons, and preview results completely
            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);
            chestMenu.sendAllDataToRemote();

            if (slot == 45) { // SAVE ACTION
                ItemStack[] targetMatrix = editor.getTarget().equals("heart") ? LifestealConfig.heartRecipeMatrix : LifestealConfig.beaconRecipeMatrix;
                for (int i = 0; i < 9; i++) {
                    targetMatrix[i] = editor.getItem(editableSlots[i]).copy();
                }
                this.player.sendSystemMessage(Component.literal("§aCustom matrix adjustments updated successfully."));
                this.player.level().getServer().execute(() -> RecipeGUI.openMainMenu(this.player));
            } else if (slot == 49 || slot == 53) { // CANCEL / GO BACK
                this.player.level().getServer().execute(() -> RecipeGUI.openMainMenu(this.player));
            }
        }
    }
}