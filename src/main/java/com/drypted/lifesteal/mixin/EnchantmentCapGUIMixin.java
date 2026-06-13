package com.drypted.lifesteal.mixin;

import com.drypted.lifesteal.config.LifestealConfigManager;
import com.drypted.lifesteal.gui.EnchantmentCapGUI;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class EnchantmentCapGUIMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void onContainerClick(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (!(this.player.containerMenu instanceof ChestMenu chestMenu)) return;

        if (chestMenu.getContainer() instanceof EnchantmentCapGUI.CapListContainer container) {
            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);

            int slot = packet.slotNum();
            ItemStack clicked = container.getItem(slot);
            boolean updated = false;

            // Updated Layout Navigation Slots
            if (slot == 45) { EnchantmentCapGUI.changeLeftPage(this.player, -1, container); updated = true; }
            if (slot == 47) { EnchantmentCapGUI.changeLeftPage(this.player, +1, container); updated = true; }
            if (slot == 50) { EnchantmentCapGUI.changeRightPage(this.player, -1, container); updated = true; }
            if (slot == 52) { EnchantmentCapGUI.changeRightPage(this.player, +1, container); updated = true; }
            if (slot == 53) { EnchantmentCapGUI.changeCategory(this.player, +1, container); updated = true; } // Cycles forward

            if (updated) {
                chestMenu.sendAllDataToRemote(); // Sync to client without resetting mouse
                return;
            }

            // Handle clicks on enchantment items (both active caps and books)
            if (!clicked.isEmpty() && (clicked.is(Items.ENCHANTED_BOOK) || clicked.has(DataComponents.STORED_ENCHANTMENTS))) {
                var customData = clicked.get(DataComponents.CUSTOM_DATA);
                if (customData != null) {
                    CompoundTag tag = customData.copyTag();
                    if (tag != null && tag.contains("enchant_id")) {
                        String enchId = tag.getString("enchant_id").orElse("");
                        if (!enchId.isEmpty()) {
                            int currentCap = LifestealConfigManager.getInstance().enchantmentCaps.getOrDefault(enchId, 0);
                            int maxPossible = EnchantmentCapGUI.getMaxPossibleLevel(this.player, enchId);
                            this.player.level().getServer().execute(() ->
                                EnchantmentCapGUI.openAdjuster(this.player, enchId, currentCap, maxPossible)
                            );
                        }
                    }
                }
            }
            return;
        }

        // Adjuster GUI (unchanged)
        if (chestMenu.getContainer() instanceof EnchantmentCapGUI.AdjusterContainer adjuster) {
            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);

            int slot = packet.slotNum();
            String enchId = adjuster.getEnchantmentId();
            int current = adjuster.getCurrentCap();
            int maxPossible = adjuster.getMaxPossible();

            if (slot == 11) {
                current = Math.max(0, current - 1);
            } else if (slot == 15) {
                current = Math.min(maxPossible, current + 1);
            } else if (slot == 22) {
                current = 0;
            } else if (slot == 26) {
                this.player.level().getServer().execute(() -> EnchantmentCapGUI.openMainMenu(this.player));
                return;
            } else {
                return;
            }

            if (current == 0) {
                LifestealConfigManager.getInstance().enchantmentCaps.remove(enchId);
            } else {
                LifestealConfigManager.getInstance().enchantmentCaps.put(enchId, current);
            }
            LifestealConfigManager.save(this.player.level().getServer());

            adjuster.setCurrentCap(current);
            adjuster.updateDisplay();
            chestMenu.sendAllDataToRemote();
            this.player.sendSystemMessage(Component.literal("§aCap for " + enchId + " set to " + current));
        }
    }
}