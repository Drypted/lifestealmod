package com.drypted.lifesteal.mixin;

import com.drypted.lifesteal.config.LifestealConfigManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEnderChestContainer.class)
public class EnderChestMixin {

    @Inject(method = "canPlaceItem", at = @At("HEAD"), cancellable = true)
    private void onCanPlaceItem(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (LifestealConfigManager.getInstance().dragonEggEnderChestDisabled
                && stack.is(Items.DRAGON_EGG)) {
            cir.setReturnValue(false);
        }
    }
}