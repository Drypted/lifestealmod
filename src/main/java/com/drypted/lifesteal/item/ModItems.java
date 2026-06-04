package com.drypted.lifesteal.item;

import com.drypted.lifesteal.Lifesteal;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class ModItems {
    public static final ResourceKey<Item> HEART_KEY = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(Lifesteal.MOD_ID, "heart")
    );

    public static final HeartItem HEART_ITEM = new HeartItem(
            HEART_KEY,
            new Item.Properties().setId(HEART_KEY) // This sets the ID before construction
    );

    public static void registerItems() {
        Registry.register(BuiltInRegistries.ITEM, HEART_KEY, HEART_ITEM);
    }
}