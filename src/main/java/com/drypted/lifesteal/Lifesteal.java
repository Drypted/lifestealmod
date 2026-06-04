package com.drypted.lifesteal;

import com.mojang.serialization.Codec;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Lifesteal implements ModInitializer {
    public static final String MOD_ID = "lifesteal";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final AttachmentType<Double> LIFESTEAL_MAX_HEALTH = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(MOD_ID, "max_health"),
            builder -> builder
                    .initializer(() -> 20.0)
                    .persistent(Codec.DOUBLE)
                    .copyOnDeath()
    );

    @Override
    public void onInitialize() {
        LOGGER.info("Lifesteal mod initializing...");

        // Event 1: Player joins the server
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            LOGGER.info("JOIN event: Player {} joined", handler.getPlayer().getName().getString());
            printPlayerHealthState(handler.getPlayer(), "Before JOIN update");
            updatePlayerMaxHealth(handler.getPlayer());
            printPlayerHealthState(handler.getPlayer(), "After JOIN update");
        });

        // Event 2: Player respawns after death - MOST IMPORTANT
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            LOGGER.info("AFTER_RESPAWN event: Player {} respawned (alive={})", 
                newPlayer.getName().getString(), alive);
            
            // Log old player's attachment value
            Double oldHealth = oldPlayer.getAttached(LIFESTEAL_MAX_HEALTH);
            LOGGER.info("  Old player attachment value: {}", oldHealth);
            
            // Log new player's attachment value BEFORE we do anything
            Double newHealthBefore = newPlayer.getAttached(LIFESTEAL_MAX_HEALTH);
            LOGGER.info("  New player attachment BEFORE update: {}", newHealthBefore);
            
            updatePlayerMaxHealth(newPlayer);
            
            // Log after update
            Double newHealthAfter = newPlayer.getAttached(LIFESTEAL_MAX_HEALTH);
            LOGGER.info("  New player attachment AFTER update: {}", newHealthAfter);
            printPlayerHealthState(newPlayer, "After respawn update");
        });

        // Event 3: Data is copied from old player to new player (clone operation)
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            LOGGER.info("COPY_FROM event: Copying data from {} to {} (alive={})", 
                oldPlayer.getName().getString(), newPlayer.getName().getString(), alive);
            
            // Manually copy the attachment - this is CRITICAL
            Double oldHealthValue = oldPlayer.getAttached(LIFESTEAL_MAX_HEALTH);
            if (oldHealthValue != null) {
                LOGGER.info("  Manually copying attachment value: {} -> {}", oldHealthValue, newPlayer.getName().getString());
                newPlayer.setAttached(LIFESTEAL_MAX_HEALTH, oldHealthValue);
            } else {
                LOGGER.warn("  Old player had NULL attachment value!");
            }
            
            // Also copy the attribute modifier directly if needed
            updatePlayerMaxHealth(newPlayer);
        });

        // Event 4: Player changes dimension (Nether, End, etc.)
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> {
            LOGGER.info("WORLD CHANGE event");
            printPlayerHealthState(player, "Before world change");
            updatePlayerMaxHealth(player);
            printPlayerHealthState(player, "After world change");
        });

        // Event 5: On death - the lifesteal transaction
        ServerLivingEntityEvents.AFTER_DEATH.register((victim, damageSource) -> {
            if (victim instanceof ServerPlayer serverVictim && 
                damageSource.getEntity() instanceof ServerPlayer serverKiller &&
                serverVictim != serverKiller) {
                
                LOGGER.info("DEATH event: {} killed by {}", 
                    serverVictim.getName().getString(), serverKiller.getName().getString());
                
                double victimHealth = serverVictim.getAttachedOrCreate(LIFESTEAL_MAX_HEALTH);
                double killerHealth = serverKiller.getAttachedOrCreate(LIFESTEAL_MAX_HEALTH);
                
                LOGGER.info("  Before: Victim={}, Killer={}", victimHealth, killerHealth);

                double newVictimHealth = Math.max(2.0, victimHealth - 2.0);
                double newKillerHealth = Math.min(40.0, killerHealth + 2.0);

                serverVictim.setAttached(LIFESTEAL_MAX_HEALTH, newVictimHealth);
                serverKiller.setAttached(LIFESTEAL_MAX_HEALTH, newKillerHealth);
                
                LOGGER.info("  After: Victim={}, Killer={}", newVictimHealth, newKillerHealth);

                updatePlayerMaxHealth(serverVictim);
                updatePlayerMaxHealth(serverKiller);
                serverKiller.heal(2.0f);
            }
        });
        
        LOGGER.info("Lifesteal mod initialization complete!");
    }

    public static void updatePlayerMaxHealth(ServerPlayer player) {
        double targetHealth = player.getAttachedOrCreate(LIFESTEAL_MAX_HEALTH);
        double delta = targetHealth - 20.0;
        
        LOGGER.debug("updatePlayerMaxHealth for {}: target={}, delta={}", 
            player.getName().getString(), targetHealth, delta);

        var maxHealthAttribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttribute != null) {
            Identifier modifierId = Identifier.fromNamespaceAndPath(MOD_ID, "max_health_modifier");
            maxHealthAttribute.removeModifier(modifierId);
            
            if (delta != 0) {
                maxHealthAttribute.addPermanentModifier(new AttributeModifier(
                        modifierId,
                        delta,
                        AttributeModifier.Operation.ADD_VALUE
                ));
                LOGGER.debug("  Added modifier with delta={}", delta);
            } else {
                LOGGER.debug("  No modifier needed (delta=0)");
            }
        } else {
            LOGGER.warn("  maxHealthAttribute was NULL for {}", player.getName().getString());
        }
        
        // Log the actual max health after modification
        LOGGER.debug("  Player max health after update: {}", player.getMaxHealth());
        
        // Sync current health if it exceeds new max
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }
    
    private static void printPlayerHealthState(ServerPlayer player, String context) {
        Double attachment = player.getAttached(LIFESTEAL_MAX_HEALTH);
        double maxHealth = player.getMaxHealth();
        double currentHealth = player.getHealth();
        
        LOGGER.info("{} - Player {}: attachment={}, maxHealth={}, currentHealth={}", 
            context, player.getName().getString(), attachment, maxHealth, currentHealth);
    }
}