package com.drypted.lifesteal.mixin;

import com.drypted.lifesteal.config.LifestealConfigManager;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(Inventory.class)
public class InventoryEnforceMixin {

    @Shadow public net.minecraft.world.entity.player.Player player;

    @Inject(method = "add", at = @At("HEAD"))
    private void onAdd(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        enforceCap(stack);
    }

    @Inject(method = "setItem", at = @At("HEAD"))
    private void onSetItem(int slot, ItemStack stack, CallbackInfo ci) {
        enforceCap(stack);
    }

    private void enforceCap(ItemStack stack) {
        if (stack.isEmpty() || this.player.level().isClientSide()) return;
        Map<String, Integer> caps = LifestealConfigManager.getInstance().enchantmentCaps;
        if (caps.isEmpty()) return;

        ItemEnchantments enchantments = stack.getEnchantments();
        if (enchantments.isEmpty()) return;

        boolean changed = false;
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(enchantments);
        Registry<Enchantment> registry = this.player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        for (Map.Entry<Holder<Enchantment>, Integer> entry : enchantments.entrySet()) {
            Holder<Enchantment> enchHolder = entry.getKey();
            int currentLevel = entry.getValue();
            Identifier enchId = registry.getKey(enchHolder.value());
            if (enchId == null) continue;
            int cap = caps.getOrDefault(enchId.toString(), -1);
            if (cap != -1 && currentLevel > cap) {
                mutable.set(enchHolder, cap);
                changed = true;
            }
        }
        if (changed) {
            stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
        }
    }
}