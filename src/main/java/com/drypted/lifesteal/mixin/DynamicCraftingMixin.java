package com.drypted.lifesteal.mixin;

import com.drypted.lifesteal.api.HeartManager;
import com.drypted.lifesteal.api.ServerItemHelper;
import com.drypted.lifesteal.config.LifestealConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingMenu.class)
public class DynamicCraftingMixin {

    @Inject(
        method = "slotChangedCraftingGrid", 
        at = @At("TAIL")
    )
    private static void onMatrixUpdated(AbstractContainerMenu menu, Level level, Player player, 
                                        CraftingContainer craftingContainer, ResultContainer resultContainer, 
                                        RecipeHolder<CraftingRecipe> recipe, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // Check against dynamic Custom Heart Grid blueprint rules
        if (LifestealConfig.heartRecipeEnabled && lifesteal$matchesMatrix(craftingContainer, LifestealConfig.heartRecipeMatrix)) {
            if (LifestealConfig.limitHeartCraftingByHealth) {
                double currentHearts = HeartManager.getMaxHealth(serverPlayer);
                if (currentHearts < LifestealConfig.minHeartsToCraft || currentHearts > LifestealConfig.maxHeartsToCraft) {
                    serverPlayer.sendOverlayMessage(Component.literal("§cYour current health limits prevent crafting hearts!"));
                    resultContainer.setItem(0, ItemStack.EMPTY);
                    menu.broadcastChanges();
                    return;
                }
            }
            resultContainer.setItem(0, ServerItemHelper.createHeart());
            menu.broadcastChanges();
            return;
        }

        // Check against dynamic Custom Revive Beacon Grid blueprint rules
        if (LifestealConfig.beaconRecipeEnabled && lifesteal$matchesMatrix(craftingContainer, LifestealConfig.beaconRecipeMatrix)) {
            resultContainer.setItem(0, ServerItemHelper.createReviveBeacon());
            menu.broadcastChanges();
        }
    }

    @Unique
    private static boolean lifesteal$matchesMatrix(CraftingContainer matrix, ItemStack[] configMatrix) {
        boolean hasItems = false;
        for (int i = 0; i < 9; i++) {
            if (!configMatrix[i].isEmpty()) {
                hasItems = true;
                break;
            }
        }
        if (!hasItems) return false; // Block empty matrix setups from producing items

        for (int i = 0; i < 9; i++) {
            ItemStack gridItem = matrix.getItem(i);
            ItemStack targetItem = configMatrix[i];

            if (targetItem.isEmpty()) {
                if (!gridItem.isEmpty()) return false;
            } else {
                if (!ItemStack.matches(gridItem, targetItem) || gridItem.getCount() < targetItem.getCount()) {
                    return false;
                }
            }
        }
        return true;
    }
}