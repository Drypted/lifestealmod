package com.drypted.lifesteal.command;

import com.drypted.lifesteal.api.HeartManager;
import com.drypted.lifesteal.api.ServerItemHelper;
import com.drypted.lifesteal.config.LifestealConfig;
import com.drypted.lifesteal.config.LifestealConfigManager;
import com.drypted.lifesteal.gui.RecipeGUI;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;

public class LifestealCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerWithdraw(dispatcher);
            registerRevive(dispatcher);
            registerLifestealBase(dispatcher);
        });
    }

    // --- /withdraw AMOUNT ---
    private static void registerWithdraw(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("withdraw")
            .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    int amountToWithdraw = IntegerArgumentType.getInteger(context, "amount");
                    
                    double healthCost = amountToWithdraw * 2.0; 
                    double currentMax = HeartManager.getMaxHealth(player); //[cite: 20]
                    
                    if (currentMax - healthCost < HeartManager.MIN_MAX_HEALTH) {
                        player.sendSystemMessage(Component.literal("§cYou cannot withdraw your last heart!"));
                        return 0;
                    }
                    
                    HeartManager.setMaxHealth(player, currentMax - healthCost); //[cite: 20]
                    
                    if (player.getHealth() > player.getMaxHealth()) { //[cite: 20]
                        player.setHealth(player.getMaxHealth()); //[cite: 20]
                    }

                    ItemStack hearts = ServerItemHelper.createHeart(); //[cite: 20]
                    hearts.setCount(amountToWithdraw); //[cite: 20]
                    if (!player.getInventory().add(hearts)) { //[cite: 20]
                        player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), hearts)); //[cite: 20]
                    }
                    
                    player.sendSystemMessage(Component.literal("§aSuccessfully withdrew " + amountToWithdraw + " hearts.")); //[cite: 20]
                    return 1;
                })
            )
        );
    }

    // --- /revive PLAYER ---
    private static void registerRevive(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("revive")
            .then(Commands.argument("target", GameProfileArgument.gameProfile())
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    Collection<NameAndId> profiles =
                            GameProfileArgument.getGameProfiles(context, "target");

                        for (NameAndId profile : profiles) {
                            GameProfile gameProfile =
                                new GameProfile(profile.id(), profile.name());

                            boolean success =
                                HeartManager.revivePlayer(source.getServer(), gameProfile);

                            if (success) {
                                source.sendSuccess(
                                    () -> Component.literal(
                                        "§aSuccessfully revived " + profile.name()
                                    ),
                                    true
                                );
                            } else {
                                source.sendFailure(
                                    Component.literal(
                                        "§cCould not revive " + profile.name()
                                        + " (Player may not be eliminated)."
                                    )
                                );
                            }
                        }
                    return 1;
                })
            )
        );
    }

    // --- /lifesteal CONTROLLER BRANCH ---
    private static void registerLifestealBase(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("lifesteal")
            .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR)) // Require Operator privileges for admin commands
            
            // /lifesteal debug give_revive_item
            .then(Commands.literal("debug")
                .then(Commands.literal("give_revive_item")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        ItemStack beacon = ServerItemHelper.createReviveBeacon();
                        if (!player.getInventory().add(beacon)) {
                            player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), beacon));
                        }
                        player.sendSystemMessage(Component.literal("§aGranted authentic Revive Beacon."));
                        return 1;
                    })
                )
            )

            // /lifesteal revive <player>
            // .then(Commands.literal("revive")
            //     .then(Commands.argument("target", GameProfileArgument.gameProfile())
            //         .executes(context -> {
            //             Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(context, "target");
            //             for (GameProfile profile : profiles) {
            //                 HeartManager.revivePlayer(context.getSource().getServer(), profile);
            //             }
            //             return 1;
            //         })
            //     )
            // )

            // /lifesteal set_hearts <player> <amount>
            .then(Commands.literal("set_hearts")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(HeartManager.MIN_MAX_HEALTH, HeartManager.MAX_MAX_HEALTH))
                        .executes(context -> {
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            double amount = DoubleArgumentType.getDouble(context, "amount");
                            HeartManager.setMaxHealth(target, amount);
                            context.getSource().sendSuccess(() -> Component.literal("§aSet " + target.getScoreboardName() + "'s max health value to " + amount), true);
                            return 1;
                        })
                    )
                )
            )

            // /lifesteal reset_hearts <player>
            .then(Commands.literal("reset_hearts")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayer target = EntityArgument.getPlayer(context, "player");
                        HeartManager.setMaxHealth(target, HeartManager.DEFAULT_MAX_HEALTH);
                        context.getSource().sendSuccess(() -> Component.literal("§aReset " + target.getScoreboardName() + " to baseline defaults."), true);
                        return 1;
                    })
                )
            )

            // /lifesteal give <player> <amount>
            .then(Commands.literal("give")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(2.0))
                        .executes(context -> {
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            double current = HeartManager.getMaxHealth(target);
                            double amount = DoubleArgumentType.getDouble(context, "amount");
                            HeartManager.setMaxHealth(target, current + amount);
                            return 1;
                        })
                    )
                )
            )

            // /lifesteal take <player> <amount>
            .then(Commands.literal("take")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(2.0))
                        .executes(context -> {
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            double current = HeartManager.getMaxHealth(target);
                            double amount = DoubleArgumentType.getDouble(context, "amount");
                            HeartManager.setMaxHealth(target, current - amount);
                            return 1;
                        })
                    )
                )
            )
            
            .then(Commands.literal("recipe")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    RecipeGUI.openMainMenu(player);
                    return 1;
                })
            )

            // /lifesteal settings <property> <value>
            .then(Commands.literal("settings")
                // banOnZeroHearts
                .then(Commands.literal("banOnZeroHearts")
                    .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> {
                            LifestealConfig.banOnZeroHearts = BoolArgumentType.getBool(context, "value");
                            context.getSource().sendSuccess(() -> Component.literal("§aConfig altered: banOnZeroHearts set to " + LifestealConfig.banOnZeroHearts), true);
                            return 1;
                        })
                    )
                )
                // loseHeartsByNaturalCauses
                .then(Commands.literal("loseHeartsByNaturalCauses")
                    .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> {
                            LifestealConfig.loseHeartsByNaturalCauses = BoolArgumentType.getBool(context, "value");
                            context.getSource().sendSuccess(() -> Component.literal("§aConfig altered: loseHeartsByNaturalCauses set to " + LifestealConfig.loseHeartsByNaturalCauses), true);
                            return 1;
                        })
                    )
                )
                // maxHearts (absolute limit)
                .then(Commands.literal("maxHearts")
                    .then(Commands.argument("value", DoubleArgumentType.doubleArg(2.0, 200.0))
                        .executes(context -> {
                            double hearts = DoubleArgumentType.getDouble(context, "value");
                            LifestealConfig.maxHearts = hearts * 2.0; // store as HP
                            LifestealConfigManager.save(context.getSource().getServer());
                            context.getSource().sendSuccess(() -> Component.literal("§aConfig saved: absolute max hearts = " + hearts + " hearts"), true);
                            return 1;
                        })
                    )
                )

                // maxHeartsToCraft (crafting limit)
                .then(Commands.literal("maxHeartsToCraft")
                    .then(Commands.argument("value", DoubleArgumentType.doubleArg(2.0, 20.0))
                        .executes(context -> {
                            double hearts = DoubleArgumentType.getDouble(context, "value");
                            LifestealConfig.maxHeartsToCraft = hearts * 2.0;
                            LifestealConfigManager.save(context.getSource().getServer());
                            context.getSource().sendSuccess(() -> Component.literal("§aConfig saved: max hearts to craft = " + hearts + " hearts"), true);
                            return 1;
                        })
                    )
                )
                // reviveAtHearts
                .then(Commands.literal("reviveAtHearts")
                    .then(Commands.argument("value", DoubleArgumentType.doubleArg(2.0, 40.0))
                        .executes(context -> {
                            LifestealConfig.reviveAtHearts = DoubleArgumentType.getDouble(context, "value");
                            context.getSource().sendSuccess(() -> Component.literal("§aConfig altered: reviveAtHearts set to " + LifestealConfig.reviveAtHearts), true);
                            return 1;
                        })
                    )
                )
            )
        );
    }
}