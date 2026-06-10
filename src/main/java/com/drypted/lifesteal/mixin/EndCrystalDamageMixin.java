package com.drypted.lifesteal.mixin;

import com.drypted.lifesteal.config.LifestealConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(EndCrystal.class)
public abstract class EndCrystalDamageMixin {

    /**
     * Replaces the default hurtServer logic.
     * The crystal still breaks, but the explosion is skipped if config disables it.
     */
    @Overwrite
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        EndCrystal crystal = (EndCrystal) (Object) this;
        Entity entity = crystal;
        
        // Invulnerability checks - use public/accessible methods instead
        if (entity.isRemoved() || entity.isInvulnerable() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        if (source.getEntity() instanceof EnderDragon) return false;

        if (!entity.isRemoved()) {
            entity.remove(Entity.RemovalReason.KILLED);

            // Only create an explosion if the source is NOT already an explosion,
            // and the config allows it.
            if (!source.is(DamageTypeTags.IS_EXPLOSION) && !LifestealConfig.endCrystalDamageDisabled) {
                DamageSource damageSource = source.getEntity() != null
                        ? entity.damageSources().explosion(entity, source.getEntity())
                        : null;
                
                level.explode(
                        entity,
                        damageSource,
                        (ExplosionDamageCalculator) null,
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        6.0F,
                        false,
                        ExplosionInteraction.BLOCK
                );
            }

            // Call onDestroyedBy logic directly
            EnderDragonFight fight = level.getDragonFight();
            if (fight != null) {
                fight.onCrystalDestroyed(crystal, source);
            }
        }

        return true;
    }
}