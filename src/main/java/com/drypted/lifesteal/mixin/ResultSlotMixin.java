package com.drypted.lifesteal.mixin;

import com.drypted.lifesteal.config.LifestealConfig;
import com.drypted.lifesteal.config.LifestealConfigManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public class ResultSlotMixin {

    @Inject(method = "onTake", at = @At("HEAD"), cancellable = true)
    private void lifesteal$onTake(Player player, ItemStack carried, CallbackInfo ci) {
        if (!carried.is(Items.MACE)) return;

        if (player.level().isClientSide()) return;

        if (!LifestealConfig.maceCraftingEnabled || LifestealConfig.maceCraftsRemaining <= 0) { //
            String message = !LifestealConfig.maceCraftingEnabled ? 
                "§cMace crafting is disabled!" : "§cAll the maces have been crafted!";
            
            player.sendSystemMessage(Component.literal(LifestealConfig.messagePrefix + message));
            
            ci.cancel();

            if (player.containerMenu instanceof CraftingMenu menu) { 
                menu.slots.get(0).set(ItemStack.EMPTY); 
                menu.broadcastChanges(); 
            }
            return;
        }

        LifestealConfig.maceCraftsRemaining--;
        
        if (player.level() instanceof ServerLevel level) {
            LifestealConfigManager.save(level.getServer());
            if (LifestealConfig.broadcastMaceCraft) { //
                level.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("§a" + player.getName().getString() + " has crafted a Mace!"),
                    false
                );
            }
        }

        if (LifestealConfig.maceCraftsRemaining <= 0 && player.containerMenu instanceof CraftingMenu menu) {
            menu.slots.get(0).set(ItemStack.EMPTY);
            menu.broadcastChanges();
        }
    }
}