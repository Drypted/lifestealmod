package com.drypted.lifesteal.config;

import com.google.gson.*;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LifestealConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("LifestealConfig");
    private static MinecraftServer currentServer;
    
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    // ---- Config fields (all values are in HEARTS) ----
    public boolean banOnZeroHearts = true;
    public boolean loseHeartsByNaturalCauses = false;
    public double maxHearts = 20.0;          // hearts (20 = 40 HP)
    public double reviveAtHearts = 10.0;     // hearts
    public boolean heartRecipeEnabled = true;
    public boolean beaconRecipeEnabled = true;
    public double maxHeartsToCraft = 8.0;    // hearts (crafting limit)
    public boolean broadcastElimination = true;

    // Store recipes as String IDs instead of ItemStacks
    public String[] heartRecipeIds = new String[9];
    public String[] beaconRecipeIds = new String[9];

    // Inside LifestealConfigManager class, add:
    public Map<String, Integer> enchantmentCaps = new HashMap<>();

    // Singleton instance
    private static LifestealConfigManager INSTANCE = null;

    private LifestealConfigManager() {
        Arrays.fill(heartRecipeIds, "");
        Arrays.fill(beaconRecipeIds, "");
        enchantmentCaps = new HashMap<>();
    }

    public static LifestealConfigManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LifestealConfigManager();
        }
        return INSTANCE;
    }

     public static void load(MinecraftServer server) {
        // Ensure instance exists
        if (INSTANCE == null) {
            INSTANCE = new LifestealConfigManager();
        }
        currentServer = server;
        
        Path configPath = server.getWorldPath(LevelResource.ROOT).resolve("lifesteal_config.json");
        
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                LifestealConfigManager loaded = GSON.fromJson(reader, LifestealConfigManager.class);
                if (loaded != null) {
                    INSTANCE = loaded;
                }
                LOGGER.info("Loaded Lifesteal config from {}", configPath);
            } catch (Exception e) {
                LOGGER.error("Failed to load config, using defaults", e);
                // Keep the default INSTANCE (already created)
            }
        }
        
        // Post‑load safety: ensure maps and arrays are not null
        if (INSTANCE.enchantmentCaps == null) {
            INSTANCE.enchantmentCaps = new HashMap<>();
        }
        if (INSTANCE.heartRecipeIds == null) {
            INSTANCE.heartRecipeIds = new String[9];
        }
        if (INSTANCE.beaconRecipeIds == null) {
            INSTANCE.beaconRecipeIds = new String[9];
        }
        for (int i = 0; i < 9; i++) {
            if (INSTANCE.heartRecipeIds[i] == null) INSTANCE.heartRecipeIds[i] = "";
            if (i >= INSTANCE.beaconRecipeIds.length) {
                String[] newArray = new String[9];
                System.arraycopy(INSTANCE.beaconRecipeIds, 0, newArray, 0, Math.min(INSTANCE.beaconRecipeIds.length, 9));
                INSTANCE.beaconRecipeIds = newArray;
            }
            if (INSTANCE.beaconRecipeIds[i] == null) INSTANCE.beaconRecipeIds[i] = "";
        }
        
        // Convert ID strings to ItemStacks
        convertIdsToItemStacks();
        
        // Copy to legacy static fields
        LifestealConfig.banOnZeroHearts = INSTANCE.banOnZeroHearts;
        LifestealConfig.loseHeartsByNaturalCauses = INSTANCE.loseHeartsByNaturalCauses;
        LifestealConfig.maxHearts = INSTANCE.maxHearts * 2;
        LifestealConfig.reviveAtHearts = INSTANCE.reviveAtHearts * 2;
        LifestealConfig.heartRecipeEnabled = INSTANCE.heartRecipeEnabled;
        LifestealConfig.beaconRecipeEnabled = INSTANCE.beaconRecipeEnabled;
        LifestealConfig.maxHeartsToCraft = INSTANCE.maxHeartsToCraft * 2;
        LifestealConfig.broadcastElimination = INSTANCE.broadcastElimination;
        
        // Save default if file didn't exist
        if (!Files.exists(configPath)) {
            save(server);
        }
    }
    
    private static void convertIdsToItemStacks() {
        if (INSTANCE == null) return;
        
        // Convert heart recipe IDs to ItemStacks
        for (int i = 0; i < 9; i++) {
            String heartId = (i < INSTANCE.heartRecipeIds.length) ? INSTANCE.heartRecipeIds[i] : "";
            String beaconId = (i < INSTANCE.beaconRecipeIds.length) ? INSTANCE.beaconRecipeIds[i] : "";
            
            LifestealConfig.heartRecipeMatrix[i] = idToItemStack(heartId);
            LifestealConfig.beaconRecipeMatrix[i] = idToItemStack(beaconId);
        }
    }
    
    private static void convertItemStacksToIds() {
        if (INSTANCE == null) return;
        
        // Ensure arrays have correct size
        if (INSTANCE.heartRecipeIds == null || INSTANCE.heartRecipeIds.length != 9) {
            INSTANCE.heartRecipeIds = new String[9];
        }
        if (INSTANCE.beaconRecipeIds == null || INSTANCE.beaconRecipeIds.length != 9) {
            INSTANCE.beaconRecipeIds = new String[9];
        }
        
        // Convert ItemStacks back to ID strings
        for (int i = 0; i < 9; i++) {
            INSTANCE.heartRecipeIds[i] = itemStackToId(LifestealConfig.heartRecipeMatrix[i]);
            INSTANCE.beaconRecipeIds[i] = itemStackToId(LifestealConfig.beaconRecipeMatrix[i]);
        }
    }
    
    private static String itemStackToId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        Identifier key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key.toString();
    }
    
    private static ItemStack idToItemStack(String id) {
        if (id == null || id.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Identifier key = Identifier.tryParse(id);
        if (key == null) {
            return ItemStack.EMPTY;
        }
        // Registry.get(Identifier) returns Optional<Holder.Reference<Item>>
        Optional<Holder.Reference<Item>> optionalItem = BuiltInRegistries.ITEM.get(key);
        if (optionalItem.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Item item = optionalItem.get().value();
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, 1);
    }

    public static void save(MinecraftServer server) {
        if (INSTANCE == null) {
            INSTANCE = new LifestealConfigManager();
        }
        currentServer = server;
        
        // Convert current ItemStacks to IDs before saving
        convertItemStacksToIds();
        
        INSTANCE.banOnZeroHearts = LifestealConfig.banOnZeroHearts;
        INSTANCE.loseHeartsByNaturalCauses = LifestealConfig.loseHeartsByNaturalCauses;
        INSTANCE.maxHearts = LifestealConfig.maxHearts / 2;
        INSTANCE.reviveAtHearts = LifestealConfig.reviveAtHearts / 2;
        INSTANCE.heartRecipeEnabled = LifestealConfig.heartRecipeEnabled;
        INSTANCE.beaconRecipeEnabled = LifestealConfig.beaconRecipeEnabled;
        INSTANCE.maxHeartsToCraft = LifestealConfig.maxHeartsToCraft / 2;
        INSTANCE.broadcastElimination = LifestealConfig.broadcastElimination;

        Path configPath = server.getWorldPath(LevelResource.ROOT).resolve("lifesteal_config.json");
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
        
        // Convert back to ItemStacks after saving
        convertIdsToItemStacks();
    }
}