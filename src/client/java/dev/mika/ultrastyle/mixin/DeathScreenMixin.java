package dev.mika.ultrastyle.mixin;

import dev.mika.ultrastyle.FightResults;
import dev.mika.ultrastyle.ResultsPanel;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin extends Screen {
    @Shadow @Final private List<Button> exitButtons;
    @Shadow @Final private Component causeOfDeath;
    protected DeathScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void ultrastyle$layout(CallbackInfo ci) {
        if (!minecraft.hasSingleplayerServer()) FightResults.playerDied();
        for (int i = 0; i < exitButtons.size(); i++) {
            Button button = exitButtons.get(i);
            button.setWidth(Math.min(200, width - 16));
            button.setPosition((width - button.getWidth()) / 2, height - 48 + i * 24);
        }
    }
    @Inject(method = "visitText", at = @At("HEAD"), cancellable = true)
    private void ultrastyle$replaceText(ActiveTextCollector output, CallbackInfo ci) {
        if (FightResults.deathDisplay() != null) ci.cancel();
    }
    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void ultrastyle$background(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (FightResults.deathDisplay() == null) return;
        graphics.fill(0, 0, width, height, 0xe00c090e);
        ci.cancel();
    }
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void ultrastyle$results(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ResultsPanel.death(graphics, width, height, causeOfDeath);
    }
}
