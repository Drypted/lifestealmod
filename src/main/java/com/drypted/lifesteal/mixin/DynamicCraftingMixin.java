package com.drypted.lifesteal.mixin;

import com.drypted.lifesteal.api.HeartManager;
import com.drypted.lifesteal.api.ServerItemHelper;
import com.drypted.lifesteal.config.LifestealConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(CraftingMenu.class)
public class DynamicCraftingMixin {

    @Shadow @Final private CraftingContainer craftSlots;
    @Shadow @Final private ResultContainer resultSlots;
    
    @Unique private Player lifesteal$player;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
    private void capturePlayerInstance(int id, Inventory inv, ContainerLevelAccess access, CallbackInfo ci) {
        this.lifesteal$player = inv.player;
    }

    @Inject(method = "slotsChanged", at = @At("TAIL"))
    private void injectCustomMatrixChecking(Container container, CallbackInfo ci) {
        if (!(this.lifesteal$player instanceof ServerPlayer serverPlayer)) return;

        // Check against dynamic Heart Custom Recipe Matrix configuration rules
        if (LifestealConfig.heartRecipeEnabled && lifesteal$matches(this.craftSlots, LifestealConfig.heartRecipeMatrix)) {
            if (LifestealConfig.limitHeartCraftingByHealth) {
                double currentHearts = HeartManager.getMaxHealth(serverPlayer);
                if (currentHearts < LifestealConfig.minHeartsToCraft || currentHearts > LifestealConfig.maxHeartsToCraft) {
                    serverPlayer.sendOverlayMessage(Component.literal("§cYour current health limits prevent crafting hearts!"));
                    this.resultSlots.setItem(0, ItemStack.EMPTY);
                    return;
                }
            }
            this.resultSlots.setItem(0, ServerItemHelper.createHeart());
            return;
        }

        // Check against dynamic Revive Beacon Custom Recipe Matrix configuration rules
        if (LifestealConfig.beaconRecipeEnabled && lifesteal$matches(this.craftSlots, LifestealConfig.beaconRecipeMatrix)) {
            this.resultSlots.setItem(0, ServerItemHelper.createReviveBeacon());
        }
    }

    @Unique
    private boolean lifesteal$matches(CraftingContainer inv, List<ItemStack> configMatrix) {
        boolean isEmpty = true;
        for (int i = 0; i < 9; i++) {
            if (!configMatrix.get(i).isEmpty()) {
                isEmpty = false;
                break;
            }
        }
        if (isEmpty) return false; // Prevent empty grids producing outcomes

        for (int i = 0; i < 9; i++) {
            ItemStack itemInGrid = inv.getItem(i);
            ItemStack targetItem = configMatrix.get(i);

            if (targetItem.isEmpty()) {
                if (!itemInGrid.isEmpty()) return false;
            } else {
                if (!ItemStack.isSameItemSameComponents(itemInGrid, targetItem) || itemInGrid.getCount() < targetItem.getCount()) {
                    return false;
                }
            }
        }
        return true;
    }
}