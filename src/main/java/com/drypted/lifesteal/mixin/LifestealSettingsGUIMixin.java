package com.drypted.lifesteal.mixin;

import com.drypted.lifesteal.config.LifestealConfig;
import com.drypted.lifesteal.gui.LifestealSettingsGUI;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class LifestealSettingsGUIMixin {

    @Shadow public ServerPlayer player;

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void onSettingsGuiClick(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (!(this.player.containerMenu instanceof ChestMenu chestMenu)) return;

        // ----- Main Settings GUI -----
        if (chestMenu.getContainer() instanceof LifestealSettingsGUI.SettingsMainContainer container) {
            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);

            int slot = packet.slotNum();
            switch (slot) {
                case 10 -> toggleValue("totemDisabled");
                case 11 -> toggleValue("endCrystalDamageDisabled");
                case 12 -> toggleValue("respawnAnchorNetherOnly");
                case 13 -> toggleValue("enderPearlDisabled");
                case 14 -> toggleValue("dragonEggEnderChestDisabled");
                case 16 -> {
                    this.player.level().getServer().execute(() -> LifestealSettingsGUI.openMaceLimitAdjuster(this.player));
                    return;
                }
                case 22 -> {
                    this.player.closeContainer();
                    return;
                }
            }
            
            // Refresh GUI in-place if an actionable button was clicked
            if (slot >= 10 && slot <= 14) {
                LifestealSettingsGUI.updateMainSettings(container);
            }
            
            // ALWAYS sync to clear glass pane ghost items
            chestMenu.sendAllDataToRemote();
            return;
        }

        // ----- Mace Limit Adjuster GUI -----
        if (chestMenu.getContainer() instanceof LifestealSettingsGUI.MaceLimitContainer container) {
            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);

            int slot = packet.slotNum();
            boolean updated = false;

            switch (slot) {
                case 10 -> { toggleValue("maceCraftingEnabled"); updated = true; }
                case 11 -> { adjustMaceLimit(-1); updated = true; }
                case 12 -> { adjustMaceLimit(-5); updated = true; }
                case 14 -> { adjustMaceLimit(1); updated = true; }
                case 15 -> { adjustMaceLimit(5); updated = true; }
                case 16 -> { toggleValue("broadcastMaceCraft"); updated = true; }
                case 22 -> {
                    this.player.level().getServer().execute(() -> LifestealSettingsGUI.openMainSettings(this.player));
                    return;
                }
            }
            
            // Refresh GUI in-place
            if (updated) {
                LifestealSettingsGUI.updateMaceLimitAdjuster(container);
            }

            // ALWAYS sync to clear ghost items
            chestMenu.sendAllDataToRemote();
        }
    }

    private void toggleValue(String fieldName) {
        try {
            java.lang.reflect.Field field = LifestealConfig.class.getField(fieldName);
            boolean current = field.getBoolean(null);
            field.setBoolean(null, !current);
            com.drypted.lifesteal.config.LifestealConfigManager.save(this.player.level().getServer());
            this.player.sendSystemMessage(Component.literal("§a" + fieldName + " set to " + !current));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void adjustMaceLimit(int delta) {
        int newLimit = LifestealConfig.maceCraftsRemaining + delta;
        if (newLimit < 0) newLimit = 0;
        LifestealConfig.maceCraftsRemaining = newLimit;
        com.drypted.lifesteal.config.LifestealConfigManager.save(this.player.level().getServer());
        this.player.sendSystemMessage(Component.literal("§aMace crafts remaining: " + newLimit));
    }
}