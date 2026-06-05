package com.drypted.lifesteal.mixin;

import com.drypted.lifesteal.api.HeartManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class NetherStarHeartMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void useServerSideHeart(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (world.isClientSide()) {
            return;
        }

        ItemStack heldItem = user.getItemInHand(hand);

        // Filter: Only target Nether Stars
        if (heldItem.is(Items.NETHER_STAR)) {
            
            // Filter: Only target Nether Stars named "Heart"
            Component customName = heldItem.get(DataComponents.CUSTOM_NAME);
            if (customName == null || !customName.getString().equalsIgnoreCase("Heart")) {
                return; // Let standard vanilla Nether Stars function normally
            }

            if (user instanceof ServerPlayer serverPlayer) {
                double currentMax = HeartManager.getMaxHealth(serverPlayer);

                if (currentMax < HeartManager.MAX_MAX_HEALTH) {
                    HeartManager.setMaxHealth(serverPlayer, currentMax + 2);
                    
                    if (!serverPlayer.getAbilities().instabuild) {
                        heldItem.shrink(1);
                    }

                    cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
                } else {
                    serverPlayer.sendOverlayMessage(
                            Component.literal("§cYou already have the maximum amount of health!")
                    );
                    cir.setReturnValue(InteractionResult.FAIL);
                }
            }
        }
    }
}