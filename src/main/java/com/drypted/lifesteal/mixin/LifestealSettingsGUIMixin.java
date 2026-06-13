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

        // ---------- MAIN MENU ----------
        if (chestMenu.getContainer() instanceof LifestealSettingsGUI.MainMenuContainer) {
            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);
            int slot = packet.slotNum();
            if (slot == 10) {
                LifestealSettingsGUI.openMaceSettings(this.player);
            } else if (slot == 12) {
                LifestealSettingsGUI.openHeartSettings(this.player);
            } else if (slot == 14) {
                LifestealSettingsGUI.openBeaconSettings(this.player);
            } else if (slot == 16) {
                LifestealSettingsGUI.openMiscSettings(this.player);
            } else {
                // Click on a glass pane – just refresh to avoid any client desync
                chestMenu.sendAllDataToRemote();
            }
            return;
        }

        // ---------- MACE SETTINGS ----------
        if (chestMenu.getContainer() instanceof LifestealSettingsGUI.MaceSettingsContainer) {
            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);
            int slot = packet.slotNum();

            if (slot == 10) {
                toggleValue("maceCraftingEnabled");
                LifestealSettingsGUI.openMaceSettings(this.player);
            } else if (slot == 14) {
                adjustMaceLimit(-1);
                LifestealSettingsGUI.openMaceSettings(this.player);
            } else if (slot == 15) {
                adjustMaceLimit(-5);
                LifestealSettingsGUI.openMaceSettings(this.player);
            } else if (slot == 16) {
                adjustMaceLimit(1);
                LifestealSettingsGUI.openMaceSettings(this.player);
            } else if (slot == 17) {
                adjustMaceLimit(5);
                LifestealSettingsGUI.openMaceSettings(this.player);
            } else if (slot == 22) {
                toggleValue("broadcastMaceCraft");
                LifestealSettingsGUI.openMaceSettings(this.player);
            } else if (slot == 26) {
                // Back to main menu – do NOT reopen mace settings
                LifestealSettingsGUI.openMainMenu(this.player);
            } else {
                // Click on glass pane – just refresh the current GUI
                LifestealSettingsGUI.openMaceSettings(this.player);
            }
            return;
        }

        // ---------- HEART SETTINGS ----------
        if (chestMenu.getContainer() instanceof LifestealSettingsGUI.HeartSettingsContainer) {
            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);
            int slot = packet.slotNum();

            if (slot == 10) {
                toggleValue("heartRecipeEnabled");
                LifestealSettingsGUI.openHeartSettings(this.player);
            } else if (slot == 11) {
                openAdjuster("maxHeartsToCraft", 2.0, LifestealConfig.maxHearts / 2.0);
            } else if (slot == 12) {
                openAdjuster("maxHearts", 2.0, 1024.0);
            } else if (slot == 13) {
                openAdjuster("reviveAtHearts", 2.0, LifestealConfig.maxHearts / 2.0);
            } else if (slot == 15) {
                toggleValue("banOnZeroHearts");
                LifestealSettingsGUI.openHeartSettings(this.player);
            } else if (slot == 16) {
                toggleValue("loseHeartsByNaturalCauses");
                LifestealSettingsGUI.openHeartSettings(this.player);
            } else if (slot == 17) {
                toggleValue("broadcastElimination");
                LifestealSettingsGUI.openHeartSettings(this.player);
            } else if (slot == 26) {
                LifestealSettingsGUI.openMainMenu(this.player);
            } else {
                LifestealSettingsGUI.openHeartSettings(this.player);
            }
            return;
        }

        // ---------- BEACON SETTINGS ----------
        if (chestMenu.getContainer() instanceof LifestealSettingsGUI.BeaconSettingsContainer) {
            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);
            int slot = packet.slotNum();

            if (slot == 13) {
                toggleValue("beaconRecipeEnabled");
                LifestealSettingsGUI.openBeaconSettings(this.player);
            } else if (slot == 26) {
                LifestealSettingsGUI.openMainMenu(this.player);
            } else {
                LifestealSettingsGUI.openBeaconSettings(this.player);
            }
            return;
        }

        // ---------- MISC SETTINGS ----------
        if (chestMenu.getContainer() instanceof LifestealSettingsGUI.MiscSettingsContainer) {
            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);
            int slot = packet.slotNum();

            if (slot == 10) {
                toggleValue("totemDisabled");
                LifestealSettingsGUI.openMiscSettings(this.player);
            } else if (slot == 11) {
                toggleValue("endCrystalDamageDisabled");
                LifestealSettingsGUI.openMiscSettings(this.player);
            } else if (slot == 12) {
                toggleValue("respawnAnchorNetherOnly");
                LifestealSettingsGUI.openMiscSettings(this.player);
            } else if (slot == 13) {
                toggleValue("enderPearlDisabled");
                LifestealSettingsGUI.openMiscSettings(this.player);
            } else if (slot == 14) {
                toggleValue("dragonEggEnderChestDisabled");
                LifestealSettingsGUI.openMiscSettings(this.player);
            } else if (slot == 26) {
                LifestealSettingsGUI.openMainMenu(this.player);
            } else {
                LifestealSettingsGUI.openMiscSettings(this.player);
            }
            return;
        }

        // ---------- DOUBLE ADJUSTER ----------
        if (chestMenu.getContainer() instanceof LifestealSettingsGUI.DoubleAdjusterContainer adjContainer) {
            ci.cancel();
            chestMenu.setCarried(ItemStack.EMPTY);
            int slot = packet.slotNum();
            String field = adjContainer.getTargetField();
            double current = LifestealSettingsGUI.getCurrentFieldValueStatic(field);
            double min = (field.equals("maxHearts") ? 2.0 : 2.0);
            double max = (field.equals("maxHearts") ? 1024.0 : LifestealConfig.maxHearts / 2.0);
            double newVal = current;

            if (slot == 10) newVal = Math.max(min, current - 1.0);
            else if (slot == 11) newVal = Math.max(min, current - 0.5);
            else if (slot == 15) newVal = Math.min(max, current + 0.5);
            else if (slot == 16) newVal = Math.min(max, current + 1.0);
            else if (slot == 22) {
                LifestealSettingsGUI.openHeartSettings(this.player);
                return;
            } else {
                // Click on glass – refresh adjuster
                LifestealSettingsGUI.openDoubleAdjuster(this.player, field, min, max);
                return;
            }

            if (newVal != current) {
                setFieldValue(field, newVal);
                LifestealConfigManager.save(this.player.level().getServer());
                this.player.sendSystemMessage(Component.literal(LifestealConfig.messagePrefix + "§a" + field + " set to " + newVal + " hearts"));
            }
            LifestealSettingsGUI.openDoubleAdjuster(this.player, field, min, max);
        }
    }

    // ---------- HELPER METHODS ----------
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

    private void setFieldValue(String fieldName, double hearts) {
        double hp = hearts * 2.0;
        switch (fieldName) {
            case "maxHeartsToCraft": LifestealConfig.maxHeartsToCraft = hp; break;
            case "reviveAtHearts":  LifestealConfig.reviveAtHearts = hp; break;
            case "maxHearts":       LifestealConfig.maxHearts = hp; break;
        }
        if (LifestealConfig.maxHeartsToCraft > LifestealConfig.maxHearts)
            LifestealConfig.maxHeartsToCraft = LifestealConfig.maxHearts;
        LifestealConfigManager.save(this.player.level().getServer());
    }

    private void openAdjuster(String field, double min, double max) {
        this.player.level().getServer().execute(() ->
            LifestealSettingsGUI.openDoubleAdjuster(this.player, field, min, max));
    }
}