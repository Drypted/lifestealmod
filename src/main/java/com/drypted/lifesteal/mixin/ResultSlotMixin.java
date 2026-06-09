package com.drypted.lifesteal.mixin;

import com.drypted.lifesteal.config.LifestealConfig;
import com.drypted.lifesteal.config.LifestealConfigManager;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public class ResultSlotMixin {

    @Inject(
        method = "onTake",
        at = @At("HEAD"),
        cancellable = true
    )
    private void lifesteal$onTake(
            Player player,
            ItemStack carried,
            CallbackInfo ci) {

        if (!carried.is(Items.MACE)) {
            return;
        }

        if (!LifestealConfig.maceCraftingEnabled) {
            return;
        }

        if (LifestealConfig.maceCraftsRemaining <= 0) {

            player.sendSystemMessage(
                Component.literal("§cThe mace limit for this server has been reached!")
            );

            ci.cancel();
            return;
        }

        LifestealConfig.maceCraftsRemaining--;

        if (player.level() instanceof ServerLevel level) {

            LifestealConfigManager.save(level.getServer());

            if (LifestealConfig.broadcastMaceCraft) {

                level.getServer()
                    .getPlayerList()
                    .broadcastSystemMessage(
                        Component.literal(
                            "§a" + player.getName().getString()
                            + " has crafted a Mace!"
                        ),
                        false
                    );
            }
        }
    }
}