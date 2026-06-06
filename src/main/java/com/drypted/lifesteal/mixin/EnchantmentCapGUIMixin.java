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

        // Main cap list GUI
        if (chestMenu.getContainer() instanceof EnchantmentCapGUI.CapListContainer) {
            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);
            chestMenu.sendAllDataToRemote();

            int slot = packet.slotNum();
            ItemStack clicked = chestMenu.getContainer().getItem(slot);
            
            // Handle pagination clicks
            if (slot == 45) { // Left Previous
                this.player.level().getServer().execute(() -> changeLeftPage(this.player, -1));
                return;
            } else if (slot == 47) { // Left Next
                this.player.level().getServer().execute(() -> changeLeftPage(this.player, +1));
                return;
            } else if (slot == 50) { // Right Previous
                this.player.level().getServer().execute(() -> changeRightPage(this.player, -1));
                return;
            } else if (slot == 52) { // Right Next
                this.player.level().getServer().execute(() -> changeRightPage(this.player, +1));
                return;
            }
            
            // Handle clicks on items (either active cap or enchantment book)
            if (!clicked.isEmpty()) {
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

    // Helper methods to call the static pagination methods in EnchantmentCapGUI
    private static void changeLeftPage(ServerPlayer player, int delta) {
        // We'll add these methods to EnchantmentCapGUI (they are already there)
        // Actually we need to make them accessible. We'll implement them directly here or add public static methods.
        // For simplicity, we'll call a new method in EnchantmentCapGUI.
        EnchantmentCapGUI.changeLeftPage(player, delta);
    }

    private static void changeRightPage(ServerPlayer player, int delta) {
        EnchantmentCapGUI.changeRightPage(player, delta);
    }
}