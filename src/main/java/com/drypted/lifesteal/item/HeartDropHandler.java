package com.drypted.lifesteal.item;

import com.drypted.lifesteal.api.HeartManager;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public class HeartDropHandler {
    public static void tryDropHeart(ServerPlayer victim, ServerPlayer killer) {
        // The logic to check if the killer has the maximum health (40.0)
        if (HeartManager.getMaxHealth(killer) >= HeartManager.MAX_MAX_HEALTH) {
            ItemStack heartStack = new ItemStack(ModItems.HEART_ITEM);
            ItemEntity drop = new ItemEntity(
                killer.level(),
                victim.getX(), victim.getY(), victim.getZ(),
                heartStack
            );
            killer.level().addFreshEntity(drop);
        }
    }
}