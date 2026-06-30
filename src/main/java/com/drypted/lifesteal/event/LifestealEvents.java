package com.drypted.lifesteal.event;

import com.drypted.lifesteal.Lifesteal;
import com.drypted.lifesteal.api.HeartManager;
import com.drypted.lifesteal.config.LifestealConfig;
import com.drypted.lifesteal.config.LifestealConfigManager;
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
            HeartDropHandler.tryDropHeart(serverVictim, serverKiller);
        }
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            HeartManager.updateHealthAttribute(handler.getPlayer());
        });

        ServerPlayerEvents.ALLOW_DEATH.register((player, damageSource, damageAmount) -> {
            if (LifestealConfigManager.getInstance().totemDisabled) {
                player.sendSystemMessage(Component.literal(LifestealConfig.messagePrefix + "§cTotem of Undying is disabled on this server!"));
                return true;
            }
            return true;
        });
        
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            HeartManager.updateHealthAttribute(newPlayer);
            newPlayer.setHealth(newPlayer.getMaxHealth());
        });

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            Double oldHealth = oldPlayer.getAttached(Lifesteal.LIFESTEAL_MAX_HEALTH);
            if (oldHealth != null) {
                newPlayer.setAttached(Lifesteal.LIFESTEAL_MAX_HEALTH, oldHealth);
            }
            HeartManager.updateHealthAttribute(newPlayer);
        });

        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> {
            HeartManager.updateHealthAttribute(player);
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((victim, source) -> {
            if (!(victim instanceof ServerPlayer serverVictim)) return;

            boolean killedByPlayer = source.getEntity() instanceof ServerPlayer;
            ServerPlayer serverKiller = killedByPlayer ? (ServerPlayer) source.getEntity() : null;

            if (killedByPlayer || LifestealConfig.loseHeartsByNaturalCauses) {
                double currentHealth = HeartManager.getMaxHealth(serverVictim);
                double newHealth = currentHealth - HeartManager.HEART_STEAL_AMOUNT;
                
                if (newHealth <= 0.0) {
                    HeartManager.setMaxHealth(serverVictim, HeartManager.MIN_MAX_HEALTH);
                    
                    if (serverKiller != null && serverVictim != serverKiller) {
                        handleKillerReward(serverVictim, serverKiller);
                    }
                    
                    eliminatePlayer(serverVictim);
                } else {
                    HeartManager.setMaxHealth(serverVictim, newHealth);
                    
                    if (serverKiller != null && serverVictim != serverKiller) {
                        handleKillerReward(serverVictim, serverKiller);
                    }
                }
            }
        });
    }

    private static void eliminatePlayer(ServerPlayer player) {
        if (LifestealConfig.broadcastElimination) {
            Component msg = Component.literal("§c" + player.getScoreboardName() + " has been eliminated (0 hearts)!");
            player.level().getServer().getPlayerList().broadcastSystemMessage(msg, false);
        }
        if (LifestealConfig.banOnZeroHearts) {
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
            player.setGameMode(GameType.SPECTATOR);
            player.sendOverlayMessage(Component.literal("§cYou ran out of hearts! You are now a spectator until revived."));
        }
    }
}