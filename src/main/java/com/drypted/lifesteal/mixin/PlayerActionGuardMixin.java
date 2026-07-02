package com.drypted.lifesteal.mixin;

import com.drypted.lifesteal.gui.GuardedGuiContainer;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ChestMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * The {@code handleContainerClick} GUI mixins only see container-click packets. Several other packet
 * handlers can still move items in and out of the player's real inventory (including the off-hand and
 * the 2x2 crafting grid) while an admin panel is open, sidestepping the click cancellation:
 * <ul>
 *   <li>{@link ServerboundPlayerActionPacket}: off-hand swap (F) and keybind drops (Q).</li>
 *   <li>{@link ServerboundSetCreativeModeSlotPacket}: in creative, writes a client-supplied stack
 *       straight into any inventory slot (1-45, i.e. crafting grid, armor, main, hotbar, off-hand),
 *       bypassing the open container entirely.</li>
 * </ul>
 * A player clicking buttons in an admin panel never legitimately needs any of these, so they are
 * refused while a guarded menu is open and the client is resynced to drop any predicted ghost item.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PlayerActionGuardMixin {

    @Shadow public ServerPlayer player;

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void lifesteal$blockInventoryActionsInGui(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (!lifesteal$inGuardedGui()) return;

        switch (packet.getAction()) {
            case SWAP_ITEM_WITH_OFFHAND, DROP_ITEM, DROP_ALL_ITEMS -> {
                ci.cancel();
                lifesteal$resync();
            }
            default -> {
            }
        }
    }

    @Inject(method = "handleSetCreativeModeSlot", at = @At("HEAD"), cancellable = true)
    private void lifesteal$blockCreativeSlotInGui(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
        if (!lifesteal$inGuardedGui()) return;
        ci.cancel();
        lifesteal$resync();
    }

    private boolean lifesteal$inGuardedGui() {
        return this.player.containerMenu instanceof ChestMenu chestMenu
                && chestMenu.getContainer() instanceof GuardedGuiContainer;
    }

    private void lifesteal$resync() {
        this.player.containerMenu.sendAllDataToRemote();
        this.player.inventoryMenu.sendAllDataToRemote();
        this.player.connection.send(new ClientboundSetEquipmentPacket(
                this.player.getId(),
                List.of(
                        Pair.of(EquipmentSlot.MAINHAND, this.player.getItemInHand(InteractionHand.MAIN_HAND)),
                        Pair.of(EquipmentSlot.OFFHAND, this.player.getItemInHand(InteractionHand.OFF_HAND))
                )
        ));
    }
}
