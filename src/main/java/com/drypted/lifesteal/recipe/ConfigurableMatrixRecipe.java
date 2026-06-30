package com.drypted.lifesteal.recipe;

import com.drypted.lifesteal.api.ServerItemHelper;
import com.drypted.lifesteal.config.LifestealConfig;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public class ConfigurableMatrixRecipe implements CraftingRecipe {
    private final String kind; // "heart" or "beacon"

    public static final MapCodec<ConfigurableMatrixRecipe> HEART_CODEC = MapCodec.unit(() -> new ConfigurableMatrixRecipe("heart"));
    public static final MapCodec<ConfigurableMatrixRecipe> BEACON_CODEC = MapCodec.unit(() -> new ConfigurableMatrixRecipe("beacon"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigurableMatrixRecipe> HEART_STREAM_CODEC = StreamCodec.unit(new ConfigurableMatrixRecipe("heart"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigurableMatrixRecipe> BEACON_STREAM_CODEC = StreamCodec.unit(new ConfigurableMatrixRecipe("beacon"));

    public static final RecipeSerializer<ConfigurableMatrixRecipe> HEART_SERIALIZER = new RecipeSerializer<>(HEART_CODEC, HEART_STREAM_CODEC);
    public static final RecipeSerializer<ConfigurableMatrixRecipe> BEACON_SERIALIZER = new RecipeSerializer<>(BEACON_CODEC, BEACON_STREAM_CODEC);

    public ConfigurableMatrixRecipe(String kind) {
        this.kind = kind;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if ("heart".equals(kind) && !LifestealConfig.heartRecipeEnabled) return false;
        if ("beacon".equals(kind) && !LifestealConfig.beaconRecipeEnabled) return false;

        ItemStack[] targetMatrix = "heart".equals(kind) 
                ? LifestealConfig.heartRecipeMatrix 
                : LifestealConfig.beaconRecipeMatrix;

        int minX = 3, maxX = 0, minY = 3, maxY = 0;
        boolean targetEmpty = true;

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                ItemStack stack = targetMatrix[y * 3 + x];
                if (stack != null && !stack.isEmpty()) {
                    targetEmpty = false;
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (targetEmpty) return input.isEmpty();

        int targetWidth = maxX - minX + 1;
        int targetHeight = maxY - minY + 1;

        if (input.width() != targetWidth || input.height() != targetHeight) return false;

        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                ItemStack gridItem = input.getItem(y * targetWidth + x);
                ItemStack targetItem = targetMatrix[(minY + y) * 3 + (minX + x)];

                if (!ItemStack.isSameItem(gridItem, targetItem)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return "heart".equals(kind) ? ServerItemHelper.createHeart() : ServerItemHelper.createReviveBeacon();
    }

    @Override
    public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
        return "heart".equals(kind) ? HEART_SERIALIZER : BEACON_SERIALIZER;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    @Override
    public boolean isSpecial() {
        return true; 
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }
}