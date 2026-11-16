package dev.mika.ultrastyle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import java.util.Locale;

/** Shared compact ULTRAKILL-inspired results layout: title, rank, ruled stat rows and total. */
public final class ResultsPanel {
    public static final int HEIGHT = 248;
    private static final int RED = 0xffff5147, GOLD = 0xffffd15c, WHITE = 0xfff1eee8, MUTED = 0xffa29c9e;

    public static void paint(PerspectiveHudCanvas canvas, FightResults.Display display, int width, String cause) {
        var font = Minecraft.getInstance().font;
        var result = display.result();
        canvas.fill(0, 0, width, HEIGHT, 0xf0100d12);
        canvas.fill(0, 0, width, 3, RED);
        canvas.text(font, result.life() ? "LIFE TERMINATED" : "BOSS DESTROYED", 10, 12, RED, false);
        canvas.text(font, font.plainSubstrByWidth(result.name(), width - 20), 10, 27, WHITE, true);
        canvas.text(font, "[ " + result.difficulty().toUpperCase(Locale.ROOT) + " ]", 10, 41, MUTED, false);
        if (cause != null) canvas.text(font, font.plainSubstrByWidth(cause, width - 20), 10, 54, MUTED, false);
        canvas.fill(10, 68, width - 10, 69, 0xff756c70);
        canvas.text(font, "PEAK RANK", 10, 80, MUTED, false);
        String rank = StyleState.RANKS[result.peakRank()];
        float rankScale = result.peakRank() == 7 ? 1.2f : 2f;
        canvas.pose().pushMatrix();
        canvas.pose().translate(width - 10 - font.width(rank) * rankScale, 74).scale(rankScale, rankScale);
        canvas.text(font, rank, 0, 0, GOLD, true);
        canvas.pose().popMatrix();
        row(canvas, width, 100, result.life() ? "TIME ALIVE" : "FIGHT TIME", time(result.ticks()), WHITE);
        row(canvas, width, 114, "STYLE EARNED", Long.toString(result.score()), GOLD);
        row(canvas, width, 128, "BEST SCORE", Long.toString(display.best()), WHITE);
        row(canvas, width, 142, "PEAK STYLE", Long.toString(Math.round(result.peakStyle())), WHITE);
        row(canvas, width, 156, "DAMAGE DEALT", result.exact() ? number(result.dealt()) : "N/A", WHITE);
        row(canvas, width, 170, "DAMAGE TAKEN", result.exact() ? number(result.taken()) : "N/A", WHITE);
        row(canvas, width, 184, "KILLS", Integer.toString(result.kills()), WHITE);
        row(canvas, width, 198, result.life() ? "BOSSES" : "BEST COMBO",
            result.life() ? Integer.toString(result.bosses()) : "x" + result.peakCombo(), WHITE);
        canvas.fill(10, 215, width - 10, 216, 0xff756c70);
        String footer = result.exact() ? "DAMAGE IN HP / 2 = HEARTS" : "OBSERVED / DAMAGE UNKNOWN";
        canvas.pose().pushMatrix();
        canvas.pose().translate(10, 225).scale(0.75f, 0.75f);
        canvas.text(font, footer, 0, 0, MUTED, false);
        if (result.life()) canvas.text(font, "SINCE JOIN / RESPAWN", 0, 14, MUTED, false);
        canvas.pose().popMatrix();
    }
    private static void row(PerspectiveHudCanvas canvas, int width, int y, String label, String value, int color) {
        var font = Minecraft.getInstance().font;
        // Fit large totals without letting values collide with labels.
        float factor = Math.min(1f, (width - 20f) / (font.width(label) + font.width(value) + 8));
        canvas.pose().pushMatrix();
        canvas.pose().translate(10, y).scale(factor, factor);
        canvas.text(font, label, 0, 0, MUTED, false);
        canvas.text(font, value, Math.round((width - 20) / factor) - font.width(value), 0, color, true);
        canvas.pose().popMatrix();
    }
    private static String number(double value) { return String.format(Locale.ROOT, "%.1f", value); }
    private static String time(long ticks) { return String.format(Locale.ROOT, "%02d:%02d.%02d", ticks / 1200, ticks / 20 % 60, ticks % 20 * 5); }

    public static void death(GuiGraphicsExtractor graphics, int width, int height, Component cause) {
        FightResults.Display result = FightResults.deathDisplay();
        if (result == null) return;
        float scale = Math.max(0.1f, Math.min(1f, Math.min((width - 20f) / 260, (height - 64f) / HEIGHT)));
        float top = Math.max(4, (height - 56 - HEIGHT * scale) / 2);
        var canvas = new PerspectiveHudCanvas(graphics, new HudPerspective(260, HEIGHT, 0),
            width / 2f, top + HEIGHT * scale / 2, scale);
        paint(canvas, result, 260, cause == null ? null : cause.getString());
    }
}
