package com.drypted.lifesteal.mixin;

import com.drypted.lifesteal.config.LifestealConfig;
import com.drypted.lifesteal.config.LifestealConfigManager;
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
        if (chestMenu.getContainer() instanceof LifestealSettingsGUI.MainSettingsContainer container) {
            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);
            int slot = packet.slotNum();

            // Handle toggles (core, recipe, restrictions, mace)
            if (slot == 10) toggleValue("banOnZeroHearts");
            else if (slot == 11) toggleValue("loseHeartsByNaturalCauses");
            else if (slot == 12) toggleValue("broadcastElimination");
            else if (slot == 19) toggleValue("heartRecipeEnabled");
            else if (slot == 20) toggleValue("beaconRecipeEnabled");
            else if (slot == 28) toggleValue("totemDisabled");
            else if (slot == 29) toggleValue("endCrystalDamageDisabled");
            else if (slot == 30) toggleValue("respawnAnchorNetherOnly");
            else if (slot == 32) toggleValue("enderPearlDisabled");
            else if (slot == 33) toggleValue("dragonEggEnderChestDisabled");
            else if (slot == 37) toggleValue("maceCraftingEnabled");
            else if (slot == 43) toggleValue("broadcastMaceCraft");
            // Adjustable items (open double adjuster)
            else if (slot == 21) openAdjuster("maxHeartsToCraft", 2.0, LifestealConfig.maxHearts / 2.0);
            else if (slot == 22) openAdjuster("reviveAtHearts", 2.0, LifestealConfig.maxHearts / 2.0);
            else if (slot == 31) openAdjuster("maxHearts", 2.0, 1024.0);
            // Mace limit adjustments
            else if (slot == 39) adjustMaceLimit(-1);
            else if (slot == 40) adjustMaceLimit(-5);
            else if (slot == 41) adjustMaceLimit(1);
            else if (slot == 42) adjustMaceLimit(5);
            // Close
            else if (slot == 44) {
                this.player.closeContainer();
                return;
            }

            // Refresh main GUI if a toggle or mace limit changed
            LifestealSettingsGUI.updateMainSettings(container);
            chestMenu.sendAllDataToRemote();
            return;
        }

        // ----- Double Adjuster GUI -----
        if (chestMenu.getContainer() instanceof LifestealSettingsGUI.DoubleAdjusterContainer adjContainer) {
            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);
            int slot = packet.slotNum();
            String field = adjContainer.getTargetField();
            double current = getCurrentFieldValue(field);
            double min = (field.equals("maxHearts") ? 2.0 : 2.0);
            double max = (field.equals("maxHearts") ? 1024.0 : LifestealConfig.maxHearts / 2.0);
            double newVal = current;

            if (slot == 10) newVal = Math.max(min, current - 1.0);
            else if (slot == 11) newVal = Math.max(min, current - 0.5);
            else if (slot == 15) newVal = Math.min(max, current + 0.5);
            else if (slot == 16) newVal = Math.min(max, current + 1.0);
            else if (slot == 22) {
                this.player.level().getServer().execute(() -> LifestealSettingsGUI.openMainSettings(this.player));
                return;
            }

            if (newVal != current) {
                setFieldValue(field, newVal);
                LifestealConfigManager.save(this.player.level().getServer());
                this.player.sendSystemMessage(Component.literal(LifestealConfig.messagePrefix +
                        "§a" + field + " set to " + newVal + " hearts"));
                LifestealSettingsGUI.updateDoubleAdjuster(adjContainer, field, min, max);
            }
            chestMenu.sendAllDataToRemote();
        }
    }

    // Helper methods
    private void toggleValue(String fieldName) {
        try {
            java.lang.reflect.Field field = LifestealConfig.class.getField(fieldName);
            boolean current = field.getBoolean(null);
            field.setBoolean(null, !current);
            LifestealConfigManager.save(this.player.level().getServer());
            this.player.sendSystemMessage(Component.literal(LifestealConfig.messagePrefix + "§a" + fieldName + " set to " + !current));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void adjustMaceLimit(int delta) {
        int newLimit = LifestealConfig.maceCraftsRemaining + delta;
        if (newLimit < 0) newLimit = 0;
        LifestealConfig.maceCraftsRemaining = newLimit;
        LifestealConfigManager.save(this.player.level().getServer());
        this.player.sendSystemMessage(Component.literal(LifestealConfig.messagePrefix + "§aMace crafts remaining: " + newLimit));
    }

    private double getCurrentFieldValue(String fieldName) {
        return LifestealSettingsGUI.getCurrentFieldValueStatic(fieldName);
    }

    private void setFieldValue(String fieldName, double hearts) {
        double halfHearts = hearts * 2.0;
        switch (fieldName) {
            case "maxHeartsToCraft":
                LifestealConfig.maxHeartsToCraft = halfHearts;
                break;
            case "reviveAtHearts":
                LifestealConfig.reviveAtHearts = halfHearts;
                break;
            case "maxHearts":
                LifestealConfig.maxHearts = halfHearts;
                break;
        }
        // Enforce craft limit <= absolute max
        if (LifestealConfig.maxHeartsToCraft > LifestealConfig.maxHearts) {
            LifestealConfig.maxHeartsToCraft = LifestealConfig.maxHearts;
        }
    }

    private void openAdjuster(String field, double min, double max) {
        this.player.level().getServer().execute(() ->
                LifestealSettingsGUI.openDoubleAdjuster(this.player, field, min, max));
    }
}