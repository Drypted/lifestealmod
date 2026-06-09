package com.drypted.lifesteal.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.drypted.lifesteal.config.LifestealConfigManager;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

// mixin/EndCrystalDamageMixin.java
@Mixin(EndCrystal.class)
public class EndCrystalDamageMixin {
    
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (LifestealConfigManager.getInstance().endCrystalDamageDisabled) {
            cir.setReturnValue(false);
        }
    }
    
    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void onExplode(CallbackInfo ci) {
        if (LifestealConfigManager.getInstance().endCrystalDamageDisabled) {
            ci.cancel();
        }
    }
}