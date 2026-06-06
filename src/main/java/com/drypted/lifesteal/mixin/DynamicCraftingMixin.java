package com.drypted.lifesteal.mixin;

import com.drypted.lifesteal.api.HeartManager;
import com.drypted.lifesteal.api.ServerItemHelper;
import com.drypted.lifesteal.config.LifestealConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingMenu.class)
public class DynamicCraftingMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("LifestealCrafting");

    @Inject(
        method = "slotChangedCraftingGrid",
        at = @At("TAIL")
    )
    private static void onMatrixUpdated(
            AbstractContainerMenu menu,
            ServerLevel level,               // ✅ Changed from Level to ServerLevel
            Player player,
            CraftingContainer craftingContainer,
            ResultContainer resultContainer,
            RecipeHolder<CraftingRecipe> recipe,
            CallbackInfo ci
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // Check heart recipe
        if (LifestealConfig.heartRecipeEnabled && matchesMatrix(craftingContainer, LifestealConfig.heartRecipeMatrix)) {
            double playerHearts = HeartManager.getMaxHealth(serverPlayer) / 2.0;
            double maxCraftHearts = LifestealConfig.maxHeartsToCraft / 2.0;
            if (playerHearts >= maxCraftHearts) {
                serverPlayer.sendOverlayMessage(Component.literal("§cCannot craft heart (you have " + playerHearts + " hearts, limit " + maxCraftHearts + ")"));
                resultContainer.setItem(0, ItemStack.EMPTY);
                menu.broadcastChanges();
                return;
            }
            resultContainer.setItem(0, ServerItemHelper.createHeart());
            menu.broadcastChanges();
            LOGGER.info("Heart crafted by {}", serverPlayer.getName().getString());
            return;
        }

        // Check beacon recipe
        if (LifestealConfig.beaconRecipeEnabled && matchesMatrix(craftingContainer, LifestealConfig.beaconRecipeMatrix)) {
            resultContainer.setItem(0, ServerItemHelper.createReviveBeacon());
            menu.broadcastChanges();
            LOGGER.info("Beacon crafted by {}", serverPlayer.getName().getString());
        }
    }

    @Unique
    private static boolean matchesMatrix(CraftingContainer grid, ItemStack[] recipeMatrix) {
        boolean hasItems = false;
        for (int i = 0; i < 9; i++) {
            if (!recipeMatrix[i].isEmpty()) {
                hasItems = true;
                break;
            }
        }
        if (!hasItems) return false;

        for (int i = 0; i < 9; i++) {
            ItemStack gridItem = grid.getItem(i);
            ItemStack targetItem = recipeMatrix[i];
            if (targetItem.isEmpty()) {
                if (!gridItem.isEmpty()) return false;
            } else {
                if (gridItem.isEmpty() || gridItem.getItem() != targetItem.getItem()) return false;
                if (gridItem.getCount() < targetItem.getCount()) return false;
            }
        }
        return true;
    }
}