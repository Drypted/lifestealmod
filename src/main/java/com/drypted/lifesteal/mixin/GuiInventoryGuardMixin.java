package com.drypted.lifesteal.mixin;

import com.drypted.lifesteal.gui.GuardedGuiContainer;
import com.drypted.lifesteal.gui.GuiInventoryGuard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalInt;

@Mixin(ServerPlayer.class)
public class GuiInventoryGuardMixin {

    // Snapshot the inventory whenever a guarded admin menu becomes the open container.
    @Inject(method = "openMenu", at = @At("RETURN"))
    private void lifesteal$captureBaseline(MenuProvider provider, CallbackInfoReturnable<OptionalInt> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (self.containerMenu instanceof ChestMenu chestMenu
                && chestMenu.getContainer() instanceof GuardedGuiContainer) {
            GuiInventoryGuard.capture(self);
        }
    }

    // Every close path (ESC, menu navigation, /command, disconnect) funnels through here. Validate
    // the inventory against the open-time snapshot before the menu is torn down.
    @Inject(method = "doCloseContainer", at = @At("HEAD"))
    private void lifesteal$validateOnClose(CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (self.containerMenu instanceof ChestMenu chestMenu
                && chestMenu.getContainer() instanceof GuardedGuiContainer) {
            GuiInventoryGuard.validateAndClear(self, chestMenu.getContainer());
        } else {
            GuiInventoryGuard.forget(self.getUUID());
        }
    }
}
