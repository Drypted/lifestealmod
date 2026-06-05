package com.drypted.lifesteal.event;

import com.drypted.lifesteal.Lifesteal;
import com.drypted.lifesteal.api.HeartManager;
import com.drypted.lifesteal.config.LifestealConfig;
import com.drypted.lifesteal.item.HeartDropHandler;
import com.mojang.authlib.GameProfile;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.level.GameType;

import java.util.Date;

public class LifestealEvents {

    public static void register() {
        // ... (Keep your JOIN, RESPAWN, COPY_FROM, WORLD CHANGE events here) ...

        ServerLivingEntityEvents.AFTER_DEATH.register((victim, source) -> {
            if (!(victim instanceof ServerPlayer serverVictim)) return;

            boolean killedByPlayer = source.getEntity() instanceof ServerPlayer;
            ServerPlayer serverKiller = killedByPlayer ? (ServerPlayer) source.getEntity() : null;

            // 1. Check if they should lose a heart
            if (killedByPlayer || LifestealConfig.loseHeartsByNaturalCauses) {
                double currentHealth = HeartManager.getMaxHealth(serverVictim);
                double newHealth = currentHealth - HeartManager.HEART_STEAL_AMOUNT;
                
                // Set the victim's new health (allowing it to hit 0 for the check below)
                HeartManager.setMaxHealth(serverVictim, Math.max(0, newHealth));

                // If killed by a player, handle the killer's rewards
                if (serverKiller != null && serverVictim != serverKiller) {
                    double killerHealth = HeartManager.getMaxHealth(serverKiller);
                    if (killerHealth < LifestealConfig.maxHearts) {
                        HeartManager.setMaxHealth(serverKiller, killerHealth + HeartManager.HEART_STEAL_AMOUNT);
                        serverKiller.heal((float) HeartManager.HEART_STEAL_AMOUNT);
                    } else {
                        // Killer is at max, drop a heart item
                        HeartDropHandler.tryDropHeart(serverVictim, serverKiller);
                    }
                }
            }

            // 2. ELIMINATION CHECK (0 Hearts)
            if (HeartManager.getMaxHealth(serverVictim) <= 0.0) {
                eliminatePlayer(serverVictim);
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