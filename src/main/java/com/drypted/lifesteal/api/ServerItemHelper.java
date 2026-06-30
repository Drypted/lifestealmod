package com.drypted.lifesteal.api;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

public class ServerItemHelper {
    public static ItemStack createHeart() {
        ItemStack heart = new ItemStack(Items.NETHER_STAR);
        heart.set(DataComponents.CUSTOM_NAME, Component.literal("§cHeart").withStyle(style -> style.withItalic(false)));
        
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("lifesteal_heart", true);
        heart.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        
        return heart;
    }

    public static ItemStack createReviveBeacon() {
        ItemStack beacon = new ItemStack(Items.BEACON);
        beacon.set(DataComponents.CUSTOM_NAME, Component.literal("§bRevive Beacon").withStyle(style -> style.withItalic(false)));
        
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("lifesteal_beacon", true);
        beacon.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        
        return beacon;
    }

    public static boolean isAuthenticHeart(ItemStack stack) {
        if (!stack.is(Items.NETHER_STAR)) return false;

        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return false;

        CompoundTag expected = new CompoundTag();
        expected.putBoolean("lifesteal_heart", true);

        return data.matchedBy(expected);
    }

    public static boolean isAuthenticBeacon(ItemStack stack) {
        if (!stack.is(Items.BEACON)) return false;

        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return false;

        CompoundTag expected = new CompoundTag();
        expected.putBoolean("lifesteal_beacon", true);

        return data.matchedBy(expected);
    }
}