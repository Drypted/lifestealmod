package com.drypted.lifesteal.item;

import com.drypted.lifesteal.api.HeartManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class HeartItem extends Item {
    public HeartItem(ResourceKey<Item> key, Properties properties) {
        super(properties); // The key will be used by the super constructor
        // In 1.21.5, Item automatically uses the key from properties,
        // but we need to ensure the key is set in properties.
        // Actually, we'll set the key in the properties before calling super.
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (user instanceof ServerPlayer serverPlayer) {
            ItemStack heartStack = user.getItemInHand(hand);
            double currentMax = HeartManager.getMaxHealth(serverPlayer);

            if (currentMax < HeartManager.MAX_MAX_HEALTH) {
                HeartManager.setMaxHealth(serverPlayer, currentMax + 2);
                ItemStack newStack = heartStack.copy();
                newStack.shrink(1);
                return InteractionResult.SUCCESS_SERVER.heldItemTransformedTo(newStack);
            } else {
                serverPlayer.sendOverlayMessage(
                        net.minecraft.network.chat.Component.literal("§cYou already have the maximum amount of health!")
                );
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }
}