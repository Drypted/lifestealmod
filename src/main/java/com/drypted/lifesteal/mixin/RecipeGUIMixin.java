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
    private void onRecipeGuiClick(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (!(this.player.containerMenu instanceof ChestMenu chestMenu)) return;

        // 1. MAIN MENU INTERACTION
        if (chestMenu.getContainer() instanceof RecipeGUI.MainMenuContainer) {
            ci.cancel(); 
            int slot = packet.slotNum();
            if (slot == 11) RecipeGUI.openRecipeEditor(this.player, "heart");
            if (slot == 13) RecipeGUI.openRecipeEditor(this.player, "beacon");
            if (slot == 15) RecipeGUI.openSettingsMenu(this.player);
            return;
        }

        // 2. SETTINGS MENU INTERACTION
        if (chestMenu.getContainer() instanceof RecipeGUI.SettingsContainer) {
            ci.cancel();
            int slot = packet.slotNum();
            if (slot == 10) {
                LifestealConfig.heartRecipeEnabled = !LifestealConfig.heartRecipeEnabled;
                RecipeGUI.openSettingsMenu(this.player);
            } else if (slot == 11) {
                LifestealConfig.beaconRecipeEnabled = !LifestealConfig.beaconRecipeEnabled;
                RecipeGUI.openSettingsMenu(this.player);
            } else if (slot == 13) {
                LifestealConfig.limitHeartCraftingByHealth = !LifestealConfig.limitHeartCraftingByHealth;
                RecipeGUI.openSettingsMenu(this.player);
            } else if (slot == 14) {
                // Adjust minimum crafting constraints
                LifestealConfig.minHeartsToCraft = LifestealConfig.minHeartsToCraft >= 40.0 ? 2.0 : LifestealConfig.minHeartsToCraft + 2.0;
                RecipeGUI.openSettingsMenu(this.player);
            } else if (slot == 15) {
                // Adjust maximum crafting constraints
                LifestealConfig.maxHeartsToCraft = LifestealConfig.maxHeartsToCraft >= 40.0 ? 2.0 : LifestealConfig.maxHeartsToCraft + 2.0;
                RecipeGUI.openSettingsMenu(this.player);
            } else if (slot == 22) {
                RecipeGUI.openMainMenu(this.player);
            }
            return;
        }

        // 3. RECIPE MATRIX EDITOR INTERACTION
        if (chestMenu.getContainer() instanceof RecipeGUI.EditorContainer editorContainer) {
            int slot = packet.slotNum();
            
            // Check if slot index falls outside top chest container interface bounds
            if (slot < 0 || slot >= editorContainer.getContainerSize()) return;

            int[] openSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
            boolean isCraftingSlot = Arrays.stream(openSlots).anyMatch(x -> x == slot);

            if (isCraftingSlot) {
                // Allow user item placement/shuffling transitions freely inside grid
                return;
            }

            // Lock structural interfaces down safely
            ci.cancel();

            if (slot == 45) { // SAVE ACTION BUTTON
                var matrixTarget = editorContainer.getRecipeTarget().equals("heart") ? LifestealConfig.heartRecipeMatrix : LifestealConfig.beaconRecipeMatrix;
                for (int i = 0; i < 9; i++) {
                    matrixTarget.set(i, editorContainer.getItem(openSlots[i]).copy());
                }
                this.player.sendSystemMessage(Component.literal("§aRecipe mapping update saved successfully."));
                RecipeGUI.openMainMenu(this.player);
            } else if (slot == 49 || slot == 53) { // CANCEL OR GO BACK
                RecipeGUI.openMainMenu(this.player);
            }
        }
    }
}