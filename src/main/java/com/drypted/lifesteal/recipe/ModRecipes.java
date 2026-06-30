package com.drypted.lifesteal.recipe;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class ModRecipes {
    
    public static final RecipeSerializer<ConfigurableMatrixRecipe> HEART_RECIPE_SERIALIZER = 
        Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            Identifier.fromNamespaceAndPath("lifesteal", "heart_matrix"),
            ConfigurableMatrixRecipe.HEART_SERIALIZER
        );

    public static final RecipeSerializer<ConfigurableMatrixRecipe> BEACON_RECIPE_SERIALIZER = 
        Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            Identifier.fromNamespaceAndPath("lifesteal", "beacon_matrix"),
            ConfigurableMatrixRecipe.BEACON_SERIALIZER
        );

    public static void register() {
    }
}