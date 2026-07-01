package com.drypted.lifesteal.mixin;

import com.drypted.lifesteal.api.HeartManager;
import com.drypted.lifesteal.config.LifestealConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingMenu.class)
public class DynamicCraftingMixin {

    @Inject(method = "slotChangedCraftingGrid", at = @At("TAIL"))
    private static void onMatrixUpdated(
            AbstractContainerMenu menu, ServerLevel level, Player player,
            CraftingContainer craftingContainer, ResultContainer resultContainer,
            RecipeHolder<CraftingRecipe> recipe, CallbackInfo ci
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)) return; //

        ItemStack resultSlotItem = resultContainer.getItem(0); //
        if (resultSlotItem.isEmpty()) return; //

        if (resultSlotItem.is(Items.MACE)) { //
            if (!LifestealConfig.maceCraftingEnabled || LifestealConfig.maceCraftsRemaining <= 0) { //
                resultContainer.setItem(0, ItemStack.EMPTY); //
                menu.broadcastChanges(); //
            }
            return;
        }

        if (resultSlotItem.getComponents().toString().contains("lifesteal:heart") || resultSlotItem.getHoverName().getString().contains("Heart")) {  //
            double playerHearts = HeartManager.getMaxHealth(serverPlayer) / 2.0; //
            double maxCraftHearts = LifestealConfig.maxHeartsToCraft / 2.0; //
            
            if (playerHearts >= maxCraftHearts) { //
                serverPlayer.sendSystemMessage(Component.literal("§cCannot craft heart (you have " + playerHearts + " hearts, limit " + maxCraftHearts + ")")); //
                resultContainer.setItem(0, ItemStack.EMPTY); //
                menu.broadcastChanges(); //
            }
        }
    }
}