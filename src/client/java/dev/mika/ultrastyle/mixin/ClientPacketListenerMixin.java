package dev.mika.ultrastyle.mixin;

import dev.mika.ultrastyle.UltraStyleClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    // TAIL runs after vanilla's ensureRunningOnSameThread handoff.
    @Inject(method = "handleDamageEvent", at = @At("TAIL"))
    private void ultrastyle$damage(ClientboundDamageEventPacket packet, CallbackInfo ci) {
        UltraStyleClient.damagePacket(packet);
    }
    @Inject(method = "handleEntityEvent", at = @At("TAIL"))
    private void ultrastyle$death(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        var level = Minecraft.getInstance().level;
        if (packet.getEventId() == 3 && level != null) UltraStyleClient.observedDeath(packet.getEntity(level));
    }
}
