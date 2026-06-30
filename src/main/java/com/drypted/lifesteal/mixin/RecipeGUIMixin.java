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

@Mixin(ServerGamePacketListenerImpl.class)
public class RecipeGUIMixin {

    @Shadow public ServerPlayer player;

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void interceptRecipeMenus(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (!(this.player.containerMenu instanceof ChestMenu chestMenu)) return;

        if (chestMenu.getContainer() instanceof RecipeGUI.MainMenuContainer) {
            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);

            int slot = packet.slotNum();
            if (slot == 11) {
                this.player.level().getServer().execute(() -> RecipeGUI.openRecipeEditor(this.player, "heart"));
            } else if (slot == 15) { 
                this.player.level().getServer().execute(() -> RecipeGUI.openRecipeEditor(this.player, "beacon"));
            } else {
                chestMenu.sendAllDataToRemote();
            }
            return;
        }

        if (chestMenu.getContainer() instanceof RecipeGUI.EditorContainer editor) {
            int slot = packet.slotNum();
            int[] editableSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};

            boolean isEditable = false;
            for (int s : editableSlots) {
                if (s == slot) {
                    isEditable = true;
                    break;
                }
            }

            if (!isEditable && slot < 54) {
                ci.cancel();
                chestMenu.setCarried(ItemStack.EMPTY);
            }

            if (slot == 45) {
                ItemStack[] targetMatrix = editor.getTarget().equals("heart") 
                        ? LifestealConfig.heartRecipeMatrix 
                        : LifestealConfig.beaconRecipeMatrix;

                for (int i = 0; i < 9; i++) {
                    ItemStack slotItem = editor.getItem(editableSlots[i]);
                    targetMatrix[i] = slotItem.isEmpty() ? ItemStack.EMPTY : slotItem.copyWithCount(1);
                }

                this.player.sendSystemMessage(Component.literal(LifestealConfig.messagePrefix + "§aRecipe saved successfully."));
                LifestealConfigManager.save(this.player.level().getServer());

                if (this.player.level().getServer() != null) {
                    this.player.level().getServer().getPlayerList().getPlayers().forEach(serverPlayer -> {
                        if (serverPlayer.containerMenu != null) {
                            serverPlayer.containerMenu.slotsChanged(serverPlayer.getInventory());
                            serverPlayer.containerMenu.broadcastChanges();
                        }
                    });
                }

                this.player.level().getServer().execute(() -> RecipeGUI.openMainMenu(this.player));

            } else if (slot == 49 || slot == 53) {
                this.player.level().getServer().execute(() -> RecipeGUI.openMainMenu(this.player));
            }

            chestMenu.sendAllDataToRemote();
        }
    }
}