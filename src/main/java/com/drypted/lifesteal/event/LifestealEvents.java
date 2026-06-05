package com.drypted.lifesteal.event;

import com.drypted.lifesteal.Lifesteal;
import com.drypted.lifesteal.api.HeartManager;
import com.drypted.lifesteal.config.LifestealConfig;
import com.drypted.lifesteal.item.HeartDropHandler;
import com.mojang.authlib.GameProfile;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.level.GameType;

import java.util.Date;

public class LifestealEvents {
    
    private static void handleKillerReward(ServerPlayer serverVictim, ServerPlayer serverKiller) {
        double killerHealth = HeartManager.getMaxHealth(serverKiller);
        if (killerHealth < LifestealConfig.maxHearts) {
            HeartManager.setMaxHealth(serverKiller, killerHealth + HeartManager.HEART_STEAL_AMOUNT);
            serverKiller.heal((float) HeartManager.HEART_STEAL_AMOUNT);
        } else {
            // Killer is at max max health, drop item on ground
            HeartDropHandler.tryDropHeart(serverVictim, serverKiller);
        }
    }

    public static void register() {
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

        ServerLivingEntityEvents.AFTER_DEATH.register((victim, source) -> {
            if (!(victim instanceof ServerPlayer serverVictim)) return;

            boolean killedByPlayer = source.getEntity() instanceof ServerPlayer;
            ServerPlayer serverKiller = killedByPlayer ? (ServerPlayer) source.getEntity() : null;

            // 1. Check if they should lose a heart
            if (killedByPlayer || LifestealConfig.loseHeartsByNaturalCauses) {
                double currentHealth = HeartManager.getMaxHealth(serverVictim);
                double newHealth = currentHealth - HeartManager.HEART_STEAL_AMOUNT;
                
                // 2. ELIMINATION CHECK (Evaluated on raw math before clamping occurs)
                if (newHealth <= 0.0) {
                    // Force their maximum health down to the baseline limit for safety, then execute elimination
                    HeartManager.setMaxHealth(serverVictim, HeartManager.MIN_MAX_HEALTH);
                    
                    // Reward the killer if applicable
                    if (serverKiller != null && serverVictim != serverKiller) {
                        handleKillerReward(serverVictim, serverKiller);
                    }
                    
                    eliminatePlayer(serverVictim);
                } else {
                    // Standard non-fatal heart loss transaction
                    HeartManager.setMaxHealth(serverVictim, newHealth);
                    
                    // Reward the killer if applicable
                    if (serverKiller != null && serverVictim != serverKiller) {
                        handleKillerReward(serverVictim, serverKiller);
                    }
                }
            }
        });
    }

    private static void eliminatePlayer(ServerPlayer player) {
        if (LifestealConfig.banOnZeroHearts) {
            // Option A: Permanent Ban
            GameProfile profile = player.getGameProfile();
            NameAndId user = new NameAndId(profile.id(), profile.name());
            UserBanListEntry banEntry = new UserBanListEntry(
                    user,
                    new Date(),
                    "LifestealMod",
                    null,
                    "You ran out of hearts! Wait to be revived."
            );
            player.level().getServer().getPlayerList().getBans().add(banEntry);
            player.connection.disconnect(Component.literal("§cYou ran out of hearts and have been eliminated!"));
        } else {
            // Option B: Spectator Mode
            player.setGameMode(GameType.SPECTATOR);
            player.sendSystemMessage(Component.literal("§cYou ran out of hearts! You are now a spectator until revived."));
        }
    }
}