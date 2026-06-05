package com.drypted.lifesteal.command;

import com.drypted.lifesteal.api.HeartManager;
import com.drypted.lifesteal.api.ServerItemHelper;
import com.drypted.lifesteal.config.LifestealConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public class LifestealCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerWithdraw(dispatcher);
            // Additional registers for /lifesteal tree would go here
        });
    }

    private static void registerWithdraw(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("withdraw")
            .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    int amountToWithdraw = IntegerArgumentType.getInteger(context, "amount");
                    
                    double healthCost = amountToWithdraw * 2.0; // 1 heart item = 2.0 health points
                    double currentMax = HeartManager.getMaxHealth(player);
                    
                    // GUARDRAILS: 
                    // 1. Prevent withdrawing if it would kill them or leave them at 0
                    if (currentMax - healthCost <= HeartManager.MIN_MAX_HEALTH) {
                        player.sendSystemMessage(Component.literal("§cYou do not have enough hearts to withdraw this amount!"));
                        return 0;
                    }
                    
                    // Deduct health
                    HeartManager.setMaxHealth(player, currentMax - healthCost);
                    
                    // DO NOT fully heal the player. Just clamp their current health to the new max.
                    if (player.getHealth() > player.getMaxHealth()) {
                        player.setHealth(player.getMaxHealth());
                    }

                    // Give authentic heart items
                    ItemStack hearts = ServerItemHelper.createHeart();
                    hearts.setCount(amountToWithdraw);
                    if (!player.getInventory().add(hearts)) {
                        // Drop on ground if inventory is full
                        player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), hearts));
                    }
                    
                    player.sendSystemMessage(Component.literal("§aSuccessfully withdrew " + amountToWithdraw + " hearts."));
                    return 1;
                })
            )
        );
    }
}