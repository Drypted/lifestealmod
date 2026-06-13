package com.drypted.lifesteal.item;

import com.drypted.lifesteal.api.HeartManager;
import com.drypted.lifesteal.api.ServerItemHelper;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public class HeartDropHandler {
    public static void tryDropHeart(ServerPlayer victim, ServerPlayer killer) {
        if (HeartManager.getMaxHealth(killer) >= HeartManager.DEFAULT_MAX_HEALTH) {
            
            // FIX: Generate an authentic heart using your helper class
            ItemStack heartStack = ServerItemHelper.createHeart();
            
            ItemEntity drop = new ItemEntity(
                killer.level(),
                victim.getX(), victim.getY(), victim.getZ(),
                heartStack
            );
            killer.level().addFreshEntity(drop);
        }
    }
}