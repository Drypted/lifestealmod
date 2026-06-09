package com.drypted.lifesteal.mixin;

import com.drypted.lifesteal.config.LifestealConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.RespawnAnchorBlock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RespawnAnchorBlock.class)
public class RespawnAnchorMixin {

    @Inject(
        method = "useItemOn",
        at = @At("HEAD"),
        cancellable = true
    )
    private void lifesteal$restrictRespawnAnchor(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        var config = LifestealConfigManager.getInstance();

        if (!config.respawnAnchorNetherOnly) return;

        // allow vanilla behavior only in Nether
        if (level.dimension() != Level.NETHER) {
            player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                    "§cRespawn Anchors are disabled in this dimension!"
                )
            );
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}