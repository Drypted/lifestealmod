package com.drypted.lifesteal.mixin;

import com.drypted.lifesteal.api.ServerItemHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockPlaceMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void preventBeaconPlacement(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (ServerItemHelper.isAuthenticBeacon(context.getItemInHand())) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}