package com.drypted.lifesteal.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.drypted.lifesteal.config.LifestealConfigManager;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@Mixin(EnderpearlItem.class)
public class EnderPearlMixin {
    
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onUse(Level world, Player user, InteractionHand hand,
                    CallbackInfoReturnable<InteractionResult> cir) {
        if (!world.isClientSide() &&
            LifestealConfigManager.getInstance().enderPearlDisabled) {
            user.sendOverlayMessage(Component.literal("§cEnder Pearls are disabled on this server!"));
            // Prevent any action on server
            cir.setReturnValue(InteractionResult.FAIL);
            // (Optional) add a short cooldown to avoid spamming
            user.getCooldowns().addCooldown((ItemStack)(Object)this, 20);
        }
    }
}