package dev.mika.ultrastyle.mixin;

import dev.mika.ultrastyle.UltraStyleClient;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerXpMixin {
    // Replace the incoming amount once; never call giveExperiencePoints recursively.
    @ModifyVariable(method = "giveExperiencePoints", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int ultrastyle$multiplyExperience(int amount) {
        return UltraStyleClient.multiplyExperience((ServerPlayer) (Object) this, amount);
    }
}
