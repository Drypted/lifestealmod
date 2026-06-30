package com.drypted.lifesteal.mixin;

import com.drypted.lifesteal.config.LifestealConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RespawnAnchorBlock.class)
public class RespawnAnchorMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void restrictCharging(ItemStack itemStack, BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hitResult,
                                  CallbackInfoReturnable<InteractionResult> cir) {
        if (!LifestealConfig.respawnAnchorNetherOnly) return;
        if (level.dimension() != Level.NETHER) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.literal("§cRespawn Anchors only work in the Nether!"));
            }
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void restrictSpawnSetting(BlockState state, Level level, BlockPos pos, Player player,
                                      BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (!LifestealConfig.respawnAnchorNetherOnly) return;
        if (level.dimension() != Level.NETHER) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.literal("§cRespawn Anchors only work in the Nether!"));
            }
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "canSetSpawn", at = @At("HEAD"), cancellable = true)
    private static void restrictSpawnCheck(ServerLevel level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!LifestealConfig.respawnAnchorNetherOnly) return;
        if (level.dimension() != Level.NETHER) {
            cir.setReturnValue(false);
        }
    }
}