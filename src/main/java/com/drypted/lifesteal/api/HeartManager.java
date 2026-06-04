package com.drypted.lifesteal.api;

import com.drypted.lifesteal.Lifesteal;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class HeartManager {

    public static final double DEFAULT_MAX_HEALTH = 20.0;
    public static final double MIN_MAX_HEALTH = 2.0;
    public static final double MAX_MAX_HEALTH = 40.0;
    public static final double HEART_STEAL_AMOUNT = 2.0;

    // Get player's stored max health
    public static double getMaxHealth(ServerPlayer player) {
        return player.getAttachedOrCreate(Lifesteal.LIFESTEAL_MAX_HEALTH);
    }

    // Set player's stored max health and update attribute
    public static void setMaxHealth(ServerPlayer player, double newMax) {
        double clamped = Math.min(MAX_MAX_HEALTH, Math.max(MIN_MAX_HEALTH, newMax));
        player.setAttached(Lifesteal.LIFESTEAL_MAX_HEALTH, clamped);
        updateHealthAttribute(player);
    }

    // Apply or remove attribute modifier based on stored value
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

        // Ensure current health does not exceed new max
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    // Process lifesteal transaction
    public static void transferHearts(ServerPlayer victim, ServerPlayer killer) {
        double victimHealth = getMaxHealth(victim);
        double killerHealth = getMaxHealth(killer);

        double newVictim = Math.max(MIN_MAX_HEALTH, victimHealth - HEART_STEAL_AMOUNT);
        double newKiller = Math.min(MAX_MAX_HEALTH, killerHealth + HEART_STEAL_AMOUNT);

        setMaxHealth(victim, newVictim);
        setMaxHealth(killer, newKiller);

        // Heal killer by the same amount (optional)
        killer.heal((float) HEART_STEAL_AMOUNT);
    }
}