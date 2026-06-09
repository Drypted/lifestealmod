package com.drypted.lifesteal.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.At;

import com.drypted.lifesteal.config.LifestealConfigManager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RespawnAnchorBlock;

@Mixin(RespawnAnchorBlock.class)
public class RespawnAnchorMixin {
    
    @ModifyVariable(method = "isChargeable", at = @At("HEAD"), argsOnly = true)
    private static boolean restrictRespawnAnchor(boolean original, Level level, BlockPos pos) {
        LifestealConfigManager config = LifestealConfigManager.getInstance();
        if (!config.respawnAnchorNetherOnly) return original;
        
        // Only allow charging in THE_NETHER
        return level.dimension() == Level.NETHER;
    }
}