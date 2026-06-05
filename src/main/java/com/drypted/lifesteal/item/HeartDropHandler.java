package com.drypted.lifesteal.item;

import com.drypted.lifesteal.api.HeartManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class HeartDropHandler {
    public static void tryDropHeart(ServerPlayer victim, ServerPlayer killer) {
        // Check if the killer is already at or above maximum health (40.0)
        if (HeartManager.getMaxHealth(killer) >= HeartManager.MAX_MAX_HEALTH) {
            
            // 1. Create a standard vanilla Nether Star stack
            ItemStack heartStack = new ItemStack(Items.NETHER_STAR);
            
            // 2. Attach the literal custom name "Heart" so it behaves like one when right-clicked
            heartStack.set(DataComponents.CUSTOM_NAME, Component.literal("Heart"));
            
            // 3. Drop it at the victim's location
            ItemEntity drop = new ItemEntity(
                killer.level(),
                victim.getX(), victim.getY(), victim.getZ(),
                heartStack
            );
            killer.level().addFreshEntity(drop);
        }
    }
}