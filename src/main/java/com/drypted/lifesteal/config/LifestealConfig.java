package com.drypted.lifesteal.config;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.item.ItemStack;

public class LifestealConfig {
    public static boolean banOnZeroHearts = true;
    public static boolean loseHeartsByNaturalCauses = false;
    
    // Absolute maximum hearts a player can have (hard cap)
    public static double maxHearts = 40.0;        // 20 hearts (40 HP)
    
    // Minimum hearts a player is revived with
    public static double reviveAtHearts = 20.0;   // 10 hearts (20 HP)
    
    // Recipe toggles
    public static boolean heartRecipeEnabled = true;
    public static boolean beaconRecipeEnabled = true;
    
    // Crafting limit - players with >= this many hearts cannot craft heart items
    public static double maxHeartsToCraft = 16.0; // 8 hearts (16 HP)
    
    public static boolean broadcastElimination = true;
    
    // Recipe matrices
    public static ItemStack[] heartRecipeMatrix = new ItemStack[9];
    public static ItemStack[] beaconRecipeMatrix = new ItemStack[9];

    // Enchantment
    public Map<String, Integer> enchantmentCaps = new HashMap<>();

    public static boolean totemDisabled = false;
    public static boolean endCrystalDamageDisabled = false;
    public static boolean respawnAnchorNetherOnly = true; // true = only Nether, false = vanilla
    public static boolean enderPearlDisabled = false;
    public static boolean dragonEggEnderChestDisabled = true;
    public static boolean maceCraftingEnabled = true;
    public static int maceCraftsRemaining = 1;
    public static boolean broadcastMaceCraft = true;
    
    static {
        for (int i = 0; i < 9; i++) {
            heartRecipeMatrix[i] = ItemStack.EMPTY;
            beaconRecipeMatrix[i] = ItemStack.EMPTY;
        }
    }
}