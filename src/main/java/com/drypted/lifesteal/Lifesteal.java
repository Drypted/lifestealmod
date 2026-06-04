package com.drypted.lifesteal;

import com.drypted.lifesteal.event.LifestealEvents;
import com.drypted.lifesteal.item.ModItems;
import com.mojang.serialization.Codec;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

public class Lifesteal implements ModInitializer {
    public static final String MOD_ID = "lifesteal";

    public static final AttachmentType<Double> LIFESTEAL_MAX_HEALTH = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(MOD_ID, "max_health"),
            builder -> builder
                    .initializer(() -> 20.0)
                    .persistent(Codec.DOUBLE)
                    .copyOnDeath()
    );

    @Override
    public void onInitialize() {
        ModItems.registerItems();
        LifestealEvents.register();
    }
}