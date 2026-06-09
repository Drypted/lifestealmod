package com.drypted.lifesteal.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;

import com.drypted.lifesteal.config.LifestealConfigManager;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.PlayerEnderChestContainer;

@Mixin(PlayerEnderChestContainer.class)
public class EnderChestMixin {

    @Inject(
        method = "setItem",
        at = @At("HEAD"),
        cancellable = true
    )
    private void lifesteal$preventDragonEgg(
            int slot,
            ItemStack stack,
            CallbackInfo ci) {

        if (LifestealConfigManager.getInstance().dragonEggEnderChestDisabled
                && stack.is(Items.DRAGON_EGG)) {

            ci.cancel();
        }
    }
}