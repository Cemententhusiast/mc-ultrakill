package dev.mika.ultrastyle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.world.BossEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Uses vanilla boss data, preserving its music, fog and server-driven membership. */
public final class BossBars {
    private final Map<UUID, Trail> trails = new HashMap<>();
    private static final class Trail {
        float value;
        float previous;
        long lastFrame;
        long holdUntil;
        Trail(float value, long now) { this.value = value; previous = value; lastFrame = now; }
    }

    public void render(GuiGraphicsExtractor graphics, Map<UUID, LerpingBossEvent> events) {
        trails.keySet().retainAll(events.keySet());
        var client = Minecraft.getInstance();
        if (client.options.hideGui) return;
        int width = Math.min(300, graphics.guiWidth() - 32);
        if (width < 40) return;
        int x = (graphics.guiWidth() - width) / 2;
        int y = 14;
        int shown = 0;
        long now = System.nanoTime();
        for (var event : events.values()) {
            if (y + 32 > graphics.guiHeight() / 3) break;
            float progress = Math.clamp(event.getProgress(), 0, 1);
            Trail trail = trails.computeIfAbsent(event.getId(), ignored -> new Trail(progress, now));
            float elapsed = Math.min(0.1f, (now - trail.lastFrame) / 1_000_000_000f);
            if (progress < trail.previous - 0.0001f) trail.holdUntil = now + 350_000_000L;
            if (progress >= trail.value) trail.value = progress;
            else if (now > trail.holdUntil) trail.value = Math.max(progress, trail.value - elapsed * 0.45f);
            trail.previous = progress;
            trail.lastFrame = now;
            int color = color(event.getColor());
            graphics.fill(x - 3, y - 3, x + width + 3, y + 28, 0xe0121015);
            String percent = Math.round(progress * 100) + "%";
            String name = client.font.plainSubstrByWidth(event.getName().getString(), width - client.font.width(percent) - 12);
            graphics.text(client.font, name, x + 2, y, 0xfff7efeb, true);
            graphics.text(client.font, percent, x + width - client.font.width(percent) - 2, y, color, true);
            graphics.fill(x, y + 12, x + width, y + 25, 0xffe5d7cc);
            graphics.fill(x + 1, y + 13, x + width - 1, y + 24, 0xff2a1920);
            int inner = width - 4;
            graphics.fill(x + 2, y + 14, x + 2 + Math.round(inner * trail.value), y + 23, 0xffffd99b);
            graphics.fill(x + 2, y + 14, x + 2 + Math.round(inner * progress), y + 23, color);
            graphics.fill(x + 2, y + 14, x + 2 + Math.round(inner * progress), y + 16, 0x55ffffff);
            int segments = switch (event.getOverlay()) {
                case NOTCHED_6 -> 6; case NOTCHED_10 -> 10; case NOTCHED_12 -> 12; case NOTCHED_20 -> 20; default -> 0;
            };
            for (int i = 1; i < segments; i++) {
                int sx = x + 2 + inner * i / segments;
                graphics.fill(sx, y + 14, sx + 1, y + 23, 0xbb24161c);
            }
            y += 38;
            shown++;
        }
        if (shown < events.size()) graphics.text(client.font, "+ " + (events.size() - shown) + " boss bars", x, y, 0xffcccccc, true);
    }

    private static int color(BossEvent.BossBarColor color) {
        return switch (color) {
            case BLUE -> 0xff538edb; case GREEN -> 0xff6eb54e; case YELLOW -> 0xffe5bd48;
            case PURPLE -> 0xffa85bdb; case WHITE -> 0xffddd9d4; case PINK -> 0xffe65e97;
            default -> 0xffd63732;
        };
    }
}
