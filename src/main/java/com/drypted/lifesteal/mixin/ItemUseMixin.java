package com.drypted.lifesteal.mixin;

import com.drypted.lifesteal.api.HeartManager;
import com.drypted.lifesteal.api.ServerItemHelper;
import com.drypted.lifesteal.config.LifestealConfig;
import com.drypted.lifesteal.gui.ReviveGUI;

// Add your ReviveGUI import here later
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ItemUseMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onRightClick(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (world.isClientSide() || !(user instanceof ServerPlayer serverPlayer)) return;

        ItemStack heldItem = user.getItemInHand(hand);

        // --- HEART LOGIC ---
        if (ServerItemHelper.isAuthenticHeart(heldItem)) {
            double currentMax = HeartManager.getMaxHealth(serverPlayer);

            if (currentMax < LifestealConfig.maxHearts) {
                HeartManager.setMaxHealth(serverPlayer, currentMax + 2);
                if (!serverPlayer.getAbilities().instabuild) {
                    heldItem.shrink(1);
                }
                cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
            } else {
                serverPlayer.sendOverlayMessage(Component.literal("§cYou are at maximum health!"));
                cir.setReturnValue(InteractionResult.FAIL);
            }
            return;
        }

        // --- BEACON LOGIC ---
        if (ServerItemHelper.isAuthenticBeacon(heldItem)) {
            // Open the Revive GUI
            ReviveGUI.open(serverPlayer);
            
            // Consume the beacon ONLY after a successful revive inside the GUI logic.
            cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
        }
    }
}