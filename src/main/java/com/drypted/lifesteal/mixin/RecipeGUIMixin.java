package com.drypted.lifesteal.mixin;

import com.drypted.lifesteal.config.LifestealConfig;
import com.drypted.lifesteal.config.LifestealConfigManager;
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
    private void interceptRecipeMenus(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (!(this.player.containerMenu instanceof ChestMenu chestMenu)) return;

        // ----- MAIN MENU -----
        if (chestMenu.getContainer() instanceof RecipeGUI.MainMenuContainer) {
            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);
            int slot = packet.slotNum();
            if (slot == 11) {
                this.player.level().getServer().execute(() -> RecipeGUI.openRecipeEditor(this.player, "heart"));
            } else if (slot == 13) {
                this.player.level().getServer().execute(() -> RecipeGUI.openRecipeEditor(this.player, "beacon"));
            }
            chestMenu.sendAllDataToRemote();
            return;
        }

        // ----- RECIPE EDITOR -----
        if (chestMenu.getContainer() instanceof RecipeGUI.EditorContainer editor) {
            int slot = packet.slotNum();
            if (slot < 0 || slot >= editor.getContainerSize()) return;

            int[] editableSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
            boolean isGrid = Arrays.stream(editableSlots).anyMatch(x -> x == slot);

            if (isGrid) return; // Allow normal drag & drop on the grid

            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);

            if (slot == 45) { // SAVE
                ItemStack[] targetMatrix = editor.getTarget().equals("heart") ? LifestealConfig.heartRecipeMatrix
                        : LifestealConfig.beaconRecipeMatrix;
                for (int i = 0; i < 9; i++) {
                    targetMatrix[i] = editor.getItem(editableSlots[i]).copy();
                }
                this.player.sendSystemMessage(Component.literal(LifestealConfig.messagePrefix + "§aRecipe saved."));
                LifestealConfigManager.save(this.player.level().getServer());
                this.player.level().getServer().execute(() -> RecipeGUI.openMainMenu(this.player));
            } else if (slot == 49 || slot == 53) { // BACK or DISCARD
                this.player.level().getServer().execute(() -> RecipeGUI.openMainMenu(this.player));
            }

            chestMenu.sendAllDataToRemote();
        }
    }
}