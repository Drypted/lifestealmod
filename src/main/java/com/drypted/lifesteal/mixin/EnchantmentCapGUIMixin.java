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
            if (slot >= 27 && slot < 54) {
                ItemStack clicked = chestMenu.getContainer().getItem(slot);
                if (clicked.is(Items.ENCHANTED_BOOK)) {
                    CompoundTag tag = clicked.get(DataComponents.CUSTOM_DATA).copyTag();
                    if (tag != null && tag.contains("enchant_id")) {
                        // getString returns Optional<String> in this version
                        String enchId = tag.getString("enchant_id").orElse("");
                        if (!enchId.isEmpty()) {
                            this.player.level().getServer().execute(() -> EnchantmentCapGUI.openAdjuster(this.player, enchId));
                        }
                    }
                }
            }
            return;
        }

        // Adjuster GUI
        if (chestMenu.getContainer() instanceof EnchantmentCapGUI.AdjusterContainer adjuster) {
            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);
            chestMenu.sendAllDataToRemote();

            int slot = packet.slotNum();
            String enchId = adjuster.getEnchantmentId();
            int current = LifestealConfigManager.getInstance().enchantmentCaps.getOrDefault(enchId, 0);
            int maxPossible = EnchantmentCapGUI.getMaxPossibleLevel(this.player, enchId);

            if (slot == 10) current = Math.max(0, current - 10);
            else if (slot == 11) current = Math.max(0, current - 1);
            else if (slot == 15) current = Math.min(maxPossible, current + 1);
            else if (slot == 16) current = Math.min(maxPossible, current + 10);
            else if (slot == 22) current = 0;
            else if (slot == 26) {
                this.player.level().getServer().execute(() -> EnchantmentCapGUI.openMainMenu(this.player));
                return;
            } else return;

            if (current == 0) {
                LifestealConfigManager.getInstance().enchantmentCaps.remove(enchId);
            } else {
                LifestealConfigManager.getInstance().enchantmentCaps.put(enchId, current);
            }
            LifestealConfigManager.save(this.player.level().getServer());
            this.player.sendSystemMessage(Component.literal("§aCap for " + enchId + " set to " + current));
            this.player.level().getServer().execute(() -> EnchantmentCapGUI.openAdjuster(this.player, enchId));
        }
    }
}