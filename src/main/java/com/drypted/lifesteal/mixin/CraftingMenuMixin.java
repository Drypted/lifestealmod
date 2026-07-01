package com.drypted.lifesteal.mixin;

import com.drypted.lifesteal.config.LifestealConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public class CraftingMenuMixin {

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void onSlotClicked(int slotId, int button, ContainerInput ContainerInput, Player player, CallbackInfo ci) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;

        if (!(menu instanceof CraftingMenu)) return;

        if (slotId < 0 || slotId >= menu.slots.size()) return;

        Slot slot = menu.getSlot(slotId);
        
        if (slotId == 0 && slot.hasItem()) {
            ItemStack craftedItem = slot.getItem();

            if (craftedItem.is(Items.MACE)) {
                if (!LifestealConfig.maceCraftingEnabled || LifestealConfig.maceCraftsRemaining <= 0) {
                    String message = !LifestealConfig.maceCraftingEnabled ? 
                        "§cMace crafting is disabled!" : "§cAll the maces have been crafted";
                    
                    player.sendOverlayMessage(Component.literal(LifestealConfig.messagePrefix + message));
                    slot.set(ItemStack.EMPTY);
                    menu.broadcastChanges();
                    ci.cancel();
                    return;
                }

                if (ContainerInput == ContainerInput.QUICK_MOVE) {
                    player.sendOverlayMessage(Component.literal("§cYou cannot shift click the Mace."));
                    ci.cancel();
                    return;
                }

                if (ContainerInput == ContainerInput.THROW) {
                    player.sendOverlayMessage(Component.literal("§cYou cannot drop the Mace directly from the crafting table."));
                    ci.cancel();
                    return;
                }
                
                if (ContainerInput == ContainerInput.SWAP) {
                    player.sendOverlayMessage(Component.literal("§cYou cannot use hotkeys to grab the Mace."));
                    ci.cancel();
                    return;
                }
            }
        }
    }
}