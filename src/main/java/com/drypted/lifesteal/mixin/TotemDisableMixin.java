package com.drypted.lifesteal.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.drypted.lifesteal.config.LifestealConfigManager;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

@Mixin(LivingEntity.class)
public class TotemDisableMixin {
    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
    private void onCheckTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayer player &&
            LifestealConfigManager.getInstance().totemDisabled) {
            // Totem will not save the player
            cir.setReturnValue(false);
        }
    }
}