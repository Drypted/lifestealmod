package com.drypted.lifesteal.command;

import com.drypted.lifesteal.api.HeartManager;
import com.drypted.lifesteal.api.ServerItemHelper;
import com.drypted.lifesteal.config.LifestealConfig;
import com.drypted.lifesteal.config.LifestealConfigManager;
import com.drypted.lifesteal.gui.EnchantmentCapGUI;
import com.drypted.lifesteal.gui.LifestealSettingsGUI;
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
import net.minecraft.world.entity.player.Inventory;
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

    private static void removeItems(Inventory inv, ItemStack stackToRemove) {
        int remaining = stackToRemove.getCount();

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack slot = inv.getItem(i);

            if (!ItemStack.isSameItemSameComponents(slot, stackToRemove)) continue;

            int remove = Math.min(slot.getCount(), remaining);
            slot.shrink(remove);
            remaining -= remove;

            if (remaining <= 0) break;
        }
    }

    // --- /withdraw AMOUNT ---
    private static void registerWithdraw(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("withdraw")
            .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    int amountToWithdraw = IntegerArgumentType.getInteger(context, "amount");

                    double healthCost = amountToWithdraw * 2.0;
                    double currentMax = HeartManager.getMaxHealth(player);

                    if (currentMax - healthCost < HeartManager.MIN_MAX_HEALTH) {
                        player.sendOverlayMessage(
                            Component.literal("§cYou cannot withdraw your last heart!")
                        );
                        return 0;
                    }

                    ItemStack hearts = ServerItemHelper.createHeart();
                    hearts.setCount(amountToWithdraw);

                    ItemStack test = hearts.copy();

                    if (!player.getInventory().add(test)) {
                        player.sendOverlayMessage(
                            Component.literal("§cYour inventory is full.")
                        );
                        return 0;
                    }

                    removeItems(player.getInventory(), hearts);

                    HeartManager.setMaxHealth(player, currentMax - healthCost);

                    if (player.getHealth() > player.getMaxHealth()) {
                        player.setHealth(player.getMaxHealth());
                    }

                    player.getInventory().add(hearts);

                    player.sendOverlayMessage(
                        Component.literal("§aSuccessfully withdrew " + amountToWithdraw + " hearts.")
                    );

                    return 1;
                })
            )
        );
    }

    // --- /revive PLAYER ---
    private static void registerRevive(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("revive")
            .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_OWNER))
            .then(Commands.argument("target", GameProfileArgument.gameProfile())
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    Collection<NameAndId> profiles = GameProfileArgument.getGameProfiles(context, "target");

                    for (NameAndId profile : profiles) {
                        GameProfile gameProfile = new GameProfile(profile.id(), profile.name());
                        boolean success = HeartManager.revivePlayer(source.getServer(), gameProfile);

                        if (success) {
                            source.sendSuccess(
                                () -> Component.literal("§aSuccessfully revived " + profile.name()),
                                true
                            );
                        } else {
                            source.sendFailure(
                                Component.literal("§cCould not revive " + profile.name() + " (Player may not be eliminated).")
                            );
                        }
                    }
                    return 1;
                })
            )
        );
    }

    private static void registerLifestealBase(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("lifesteal")
            .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_OWNER)) // Operator only
            
            // /lifesteal item
            .then(Commands.literal("item")
                .then(Commands.literal("give_revive_item")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        ItemStack beacon = ServerItemHelper.createReviveBeacon();
                        if (!player.getInventory().add(beacon)) {
                            player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), beacon));
                        }
                        player.sendOverlayMessage(Component.literal("§aGranted Revive Beacon."));
                        return 1;
                    })
                )
                .then(Commands.literal("give_heart_item")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        ItemStack heart = ServerItemHelper.createHeart();
                        if (!player.getInventory().add(heart)) {
                            player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), heart));
                        }
                        player.sendOverlayMessage(Component.literal("§aGranted Heart."));
                        return 1;
                    })
                )
            )
            
            .then(Commands.literal("enchantment_cap")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    EnchantmentCapGUI.openMainMenu(player);
                    return 1;
                })
            )

            // /lifesteal set_hearts <player> <amount in hearts>
            .then(Commands.literal("set_hearts")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(HeartManager.MIN_MAX_HEALTH / 2.0, 1024.0))
                        .executes(context -> {
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            double requestedHearts = DoubleArgumentType.getDouble(context, "amount");
                            double maxAllowedHearts = LifestealConfig.maxHearts / 2.0;
                            double finalHearts = Math.min(requestedHearts, maxAllowedHearts);
                            HeartManager.setMaxHealth(target, finalHearts * 2.0);
                            context.getSource().sendSuccess(
                                () -> Component.literal("§aSet " + target.getScoreboardName() + "'s max health to " + finalHearts + " hearts"),
                                true
                            );
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

            // /lifesteal give <player> <amount in hearts>
            .then(Commands.literal("give")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                        .executes(context -> {
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            double heartsToAdd = DoubleArgumentType.getDouble(context, "amount");
                            double currentHP = HeartManager.getMaxHealth(target);
                            double newHP = currentHP + (heartsToAdd * 2.0);
                            double maxAllowedHP = LifestealConfig.maxHearts;
                            double minHP = HeartManager.MIN_MAX_HEALTH;
                            double clampedHP = Math.min(maxAllowedHP, Math.max(minHP, newHP));
                            HeartManager.setMaxHealth(target, clampedHP);
                            double actualHeartsAdded = (clampedHP - currentHP) / 2.0;
                            context.getSource().sendSuccess(
                                () -> Component.literal("§aAdded " + actualHeartsAdded + " hearts to " + target.getScoreboardName() + ". Now at " + (clampedHP/2.0) + " hearts."),
                                true
                            );
                            return 1;
                        })
                    )
                )
            )

            // /lifesteal take <player> <amount in hearts>
            .then(Commands.literal("take")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                        .executes(context -> {
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            double heartsToTake = DoubleArgumentType.getDouble(context, "amount");
                            double currentHP = HeartManager.getMaxHealth(target);
                            double newHP = currentHP - (heartsToTake * 2.0);
                            double minHP = HeartManager.MIN_MAX_HEALTH;
                            double clampedHP = Math.max(minHP, newHP);
                            HeartManager.setMaxHealth(target, clampedHP);
                            double actualHeartsTaken = (currentHP - clampedHP) / 2.0;
                            context.getSource().sendSuccess(
                                () -> Component.literal("§aTook " + actualHeartsTaken + " hearts from " + target.getScoreboardName() + ". Now at " + (clampedHP/2.0) + " hearts."),
                                true
                            );
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

            // /lifesteal settings
            .then(Commands.literal("settings")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    LifestealSettingsGUI.openMainMenu(player);
                    return 1;
                })
                // banOnZeroHearts
                .then(Commands.literal("banOnZeroHearts")
                    .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> {
                            LifestealConfig.banOnZeroHearts = BoolArgumentType.getBool(context, "value");
                            LifestealConfigManager.save(context.getSource().getServer());
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
                            LifestealConfigManager.save(context.getSource().getServer());
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
                            LifestealConfig.maxHearts = hearts * 2.0;
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
                            LifestealConfigManager.save(context.getSource().getServer());
                            context.getSource().sendSuccess(() -> Component.literal("§aConfig altered: reviveAtHearts set to " + LifestealConfig.reviveAtHearts), true);
                            return 1;
                        })
                    )
                )
            )
        );
    }
}