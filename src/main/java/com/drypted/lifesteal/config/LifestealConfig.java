package com.drypted.lifesteal.config;

import net.minecraft.world.item.ItemStack;

public class LifestealConfig {
    public static boolean banOnZeroHearts = true;
    public static boolean loseHeartsByNaturalCauses = false;
    public static double maxHearts = 40.0; 
    public static double reviveAtHearts = 10.0; 

    // --- RECIPE & GUI CONFIGURATIONS ---
    public static boolean heartRecipeEnabled = true;
    public static boolean beaconRecipeEnabled = true;
    
    public static boolean limitHeartCraftingByHealth = false;
    public static double minHeartsToCraft = 2.0;
    public static double maxHeartsToCraft = 40.0;

    // Fixed arrays for 3x3 matrices
    public static ItemStack[] heartRecipeMatrix = new ItemStack[9];
    public static ItemStack[] beaconRecipeMatrix = new ItemStack[9];

    static {
        for (int i = 0; i < 9; i++) {
            heartRecipeMatrix[i] = ItemStack.EMPTY;
            beaconRecipeMatrix[i] = ItemStack.EMPTY;
        }
    }
}