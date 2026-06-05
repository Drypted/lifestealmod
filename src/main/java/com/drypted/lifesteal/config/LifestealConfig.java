package com.drypted.lifesteal.config;

import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LifestealConfig {
    public static boolean banOnZeroHearts = true;
    public static boolean loseHeartsByNaturalCauses = false;
    public static double maxHearts = 40.0; // 20 hearts
    public static double reviveAtHearts = 10.0; // 5 hearts

    // --- NEW RECIPE & GUI CONFIGURATIONS ---
    public static boolean heartRecipeEnabled = true;
    public static boolean beaconRecipeEnabled = true;
    
    public static boolean limitHeartCraftingByHealth = false;
    public static double minHeartsToCraft = 2.0;
    public static double maxHeartsToCraft = 40.0;

    // 3x3 Crafting Grid Matrices (9 slots)
    public static List<ItemStack> heartRecipeMatrix = new ArrayList<>(Collections.nCopies(9, ItemStack.EMPTY));
    public static List<ItemStack> beaconRecipeMatrix = new ArrayList<>(Collections.nCopies(9, ItemStack.EMPTY));
}