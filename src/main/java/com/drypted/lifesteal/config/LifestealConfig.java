package com.drypted.lifesteal.config;

public class LifestealConfig {
    public static boolean banOnZeroHearts = true;
    public static boolean loseHeartsByNaturalCauses = false;
    public static double maxHearts = 40.0; // 20 hearts
    public static double reviveAtHearts = 10.0; // 5 hearts
    
    // In a full implementation, you would use Google Gson here to serialize 
    // these static variables to a lifesteal_config.json file in the config/ folder.
}