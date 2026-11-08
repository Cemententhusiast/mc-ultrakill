package dev.mika.ultrastyle.mixin;

import dev.mika.ultrastyle.BossBars;
import dev.mika.ultrastyle.UltraStyleClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;
import java.util.UUID;

@Mixin(BossHealthOverlay.class)
public abstract class BossHealthOverlayMixin {
    @Shadow @Final private Map<UUID, LerpingBossEvent> events;
    @Unique private final BossBars ultrastyle$bars = new BossBars();

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void ultrastyle$render(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (UltraStyleClient.config != null && UltraStyleClient.config.bossBarsEnabled) {
            ultrastyle$bars.render(graphics, events);
            ci.cancel();
        }
    }
}
