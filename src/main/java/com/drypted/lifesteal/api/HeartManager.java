package com.drypted.lifesteal.api;

import com.drypted.lifesteal.Lifesteal;
import com.drypted.lifesteal.config.LifestealConfig;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.util.Set;

public class HeartManager {

    public static final double DEFAULT_MAX_HEALTH = 20.0;
    public static final double MIN_MAX_HEALTH = 2.0;
    public static final double MAX_MAX_HEALTH = 40.0;
    public static final double HEART_STEAL_AMOUNT = 2.0;

    public static double getMaxHealth(ServerPlayer player) {
        return player.getAttachedOrCreate(Lifesteal.LIFESTEAL_MAX_HEALTH);
    }

    public static void setMaxHealth(ServerPlayer player, double newMax) {
        double clamped = Math.min(MAX_MAX_HEALTH, Math.max(MIN_MAX_HEALTH, newMax));
        player.setAttached(Lifesteal.LIFESTEAL_MAX_HEALTH, clamped);
        updateHealthAttribute(player);
    }

    public static void updateHealthAttribute(ServerPlayer player) {
        double target = getMaxHealth(player);
        double delta = target - DEFAULT_MAX_HEALTH;

        var attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) return;

        Identifier modifierId = Identifier.fromNamespaceAndPath(Lifesteal.MOD_ID, "max_health_modifier");
        attribute.removeModifier(modifierId);

        if (delta != 0) {
            attribute.addPermanentModifier(new AttributeModifier(
                    modifierId, delta, AttributeModifier.Operation.ADD_VALUE
            ));
        }

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    public static void transferHearts(ServerPlayer victim, ServerPlayer killer) {
        double victimHealth = getMaxHealth(victim);
        double killerHealth = getMaxHealth(killer);

        double newVictim = Math.max(MIN_MAX_HEALTH, victimHealth - HEART_STEAL_AMOUNT);
        double newKiller = Math.min(MAX_MAX_HEALTH, killerHealth + HEART_STEAL_AMOUNT);

        setMaxHealth(victim, newVictim);
        setMaxHealth(killer, newKiller);

        killer.heal((float) HEART_STEAL_AMOUNT);
    }

    public static boolean revivePlayer(MinecraftServer server, GameProfile profile) {
        boolean revived = false;

        NameAndId nameAndId = new NameAndId(profile.id(), profile.name());

        // 1. Unban if they are banned
        if (server.getPlayerList().getBans().isBanned(nameAndId)) {
            server.getPlayerList().getBans().remove(nameAndId);
            revived = true;
        }

        ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(profile.id());
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        BlockPos overworldSpawn = overworld.getLevelData().getRespawnData().pos();

        if (onlinePlayer != null) {
            // 2. Online Player Logic (Spectator handling)
            if (onlinePlayer.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                onlinePlayer.setGameMode(GameType.SURVIVAL);
            }
            setMaxHealth(onlinePlayer, LifestealConfig.reviveAtHearts);
            onlinePlayer.setHealth((float) LifestealConfig.reviveAtHearts);
            
            // Teleport securely to overworld spawn
            onlinePlayer.teleportTo(
                    overworld,
                    overworldSpawn.getX() + 0.5,
                    overworldSpawn.getY(),
                    overworldSpawn.getZ() + 0.5,
                    Set.of(),
                    0.0F,
                    0.0F,
                    true
            );
            onlinePlayer.sendOverlayMessage(Component.literal("§aYou have been revived!"));
            revived = true;
        } else {
            // 3. Offline Player Logic (Raw NBT manipulation)
            try {
                File playerDataDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();
                File playerFile = new File(playerDataDir, profile.id() + ".dat");
                
                if (playerFile.exists()) {
                    CompoundTag tag = NbtIo.readCompressed(playerFile.toPath(), NbtAccounter.unlimitedHeap());
                    
                    // Force Gamemode to Survival
                    tag.putInt("playerGameType", GameType.SURVIVAL.getId());
                    
                    // Set correct Health
                    tag.putFloat("Health", (float) LifestealConfig.reviveAtHearts);
                    
                    // Force position to Overworld Spawn
                    ListTag posTag = new ListTag();
                    posTag.add(DoubleTag.valueOf(overworldSpawn.getX() + 0.5));
                    posTag.add(DoubleTag.valueOf(overworldSpawn.getY()));
                    posTag.add(DoubleTag.valueOf(overworldSpawn.getZ() + 0.5));
                    tag.put("Pos", posTag);
                    tag.putString("Dimension", Level.OVERWORLD.identifier().toString());

                    // Modify Lifesteal specific max health attachment dynamically
                    CompoundTag fabricAttachments =
                        tag.getCompound("fabric:attachments")
                        .orElse(new CompoundTag());
                    fabricAttachments.putDouble("lifesteal:max_health", LifestealConfig.reviveAtHearts);
                    tag.put("fabric:attachments", fabricAttachments);

                    // Save modifications
                    NbtIo.writeCompressed(tag, playerFile.toPath());
                    revived = true;
                }
            } catch (Exception e) {
                
            }
        }

        return revived;
    }
}