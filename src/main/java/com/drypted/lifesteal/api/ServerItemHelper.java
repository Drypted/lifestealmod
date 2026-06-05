package com.drypted.lifesteal.api;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

public class ServerItemHelper {

    // --- CREATE ITEMS ---
    
    public static ItemStack createHeart() {
        ItemStack heart = new ItemStack(Items.NETHER_STAR);
        heart.set(DataComponents.CUSTOM_NAME, Component.literal("§cHeart").withStyle(style -> style.withItalic(false)));
        
        // Inject a secure, hidden tag that cannot be forged in an anvil
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

    // --- VERIFY ITEMS ---

    public static boolean isAuthenticHeart(ItemStack stack) {
        if (!stack.is(Items.NETHER_STAR)) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.equals("lifesteal_heart");
    }

    public static boolean isAuthenticBeacon(ItemStack stack) {
        if (!stack.is(Items.BEACON)) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.equals("lifesteal_beacon");
    }
}