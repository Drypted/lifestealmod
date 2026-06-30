package com.drypted.lifesteal;

import com.drypted.lifesteal.command.LifestealCommands;
import com.drypted.lifesteal.config.LifestealConfigManager;
import com.drypted.lifesteal.event.LifestealEvents;
import com.drypted.lifesteal.recipe.ModRecipes;
import com.mojang.serialization.Codec;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
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
        LifestealEvents.register();
        LifestealCommands.register();
        ModRecipes.register();
        

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            LifestealConfigManager.load(server);
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            LifestealConfigManager.save(server);
        });
    }
}