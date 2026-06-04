package com.drypted.lifesteal.event;

import com.drypted.lifesteal.Lifesteal;
import com.drypted.lifesteal.api.HeartManager;
import com.drypted.lifesteal.item.HeartDropHandler;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

public class LifestealEvents {

    public static void register() {
        // JOIN
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            HeartManager.updateHealthAttribute(handler.getPlayer());
        });

        // RESPAWN
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // Data should have been copied via COPY_FROM; just reapply attribute
            HeartManager.updateHealthAttribute(newPlayer);
        });

        // COPY_FROM (critical for persistence)
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            Double oldHealth = oldPlayer.getAttached(Lifesteal.LIFESTEAL_MAX_HEALTH);
            if (oldHealth != null) {
                newPlayer.setAttached(Lifesteal.LIFESTEAL_MAX_HEALTH, oldHealth);
            }
            HeartManager.updateHealthAttribute(newPlayer);
        });

        // WORLD CHANGE (dimension travel)
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> {
            HeartManager.updateHealthAttribute(player);
        });

        // DEATH – lifesteal + heart drop
        ServerLivingEntityEvents.AFTER_DEATH.register((victim, source) -> {
            if (victim instanceof ServerPlayer serverVictim &&
                source.getEntity() instanceof ServerPlayer serverKiller &&
                serverVictim != serverKiller) {

                // Perform the heart transfer
                HeartManager.transferHearts(serverVictim, serverKiller);

                // Drop a heart item if killer is at max hearts
                HeartDropHandler.tryDropHeart(serverVictim, serverKiller);
            }
        });
    }
}