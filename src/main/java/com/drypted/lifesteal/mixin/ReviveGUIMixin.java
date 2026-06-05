package com.drypted.lifesteal.mixin;

import com.drypted.lifesteal.api.HeartManager;
import com.drypted.lifesteal.api.ServerItemHelper;
import com.drypted.lifesteal.gui.ReviveGUI;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ReviveGUIMixin {

    @Shadow public ServerPlayer player;

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void onContainerClick(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (this.player.containerMenu instanceof ChestMenu chestMenu) {
            
            // Validate that this is explicitly the Revive Container
            if (chestMenu.getContainer() instanceof ReviveGUI.ReviveContainer) {
                ci.cancel(); // Prevent taking items out of the menu

                int slot = packet.slotNum();
                if (slot < 0 || slot >= chestMenu.getContainer().getContainerSize()) return;

                ItemStack clickedItem = chestMenu.getContainer().getItem(slot);
                if (clickedItem.isEmpty()) return;

                ResolvableProfile profileComponent = clickedItem.get(DataComponents.PROFILE);
                if (profileComponent != null && profileComponent.partialProfile() != null) {
                    GameProfile targetProfile = profileComponent.partialProfile();
                    
                    boolean success = HeartManager.revivePlayer(this.player.level().getServer(), targetProfile);
                    
                    if (success) {
                        this.player.sendSystemMessage(Component.literal("§aSuccessfully revived " + targetProfile.name() + "!"));
                        
                        // Safely consume the Revive Beacon item used to trigger the GUI
                        ItemStack mainHand = this.player.getMainHandItem();
                        if (ServerItemHelper.isAuthenticBeacon(mainHand)) {
                            mainHand.shrink(1);
                        } else {
                            ItemStack offHand = this.player.getOffhandItem();
                            if (ServerItemHelper.isAuthenticBeacon(offHand)) {
                                offHand.shrink(1);
                            }
                        }
                        
                        this.player.closeContainer();
                    } else {
                        this.player.sendSystemMessage(Component.literal("§cCould not revive " + targetProfile.name() + "."));
                    }
                }
            }
        }
    }
}