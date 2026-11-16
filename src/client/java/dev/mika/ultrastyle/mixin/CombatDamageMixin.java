package dev.mika.ultrastyle.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.mika.ultrastyle.FightResults;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

// Player overrides actuallyHurt without calling super, so both implementations need the hook.
@Mixin({LivingEntity.class, Player.class})
public abstract class CombatDamageMixin {
    @WrapMethod(method = "actuallyHurt")
    private void ultrastyle$recordHealthLoss(ServerLevel level, DamageSource source, float amount, Operation<Void> original) {
        LivingEntity entity = (LivingEntity) (Object) this;
        float before = entity.getHealth();
        original.call(level, source, amount);
        FightResults.serverDamage(entity, source, Math.max(0, before - entity.getHealth()));
    }
}
