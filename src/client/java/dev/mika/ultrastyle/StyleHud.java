package dev.mika.ultrastyle;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class StyleHud {
    private static final int[] COLORS = {0xff8fb9e8, 0xff65dca0, 0xfff4d664, 0xffffa74f, 0xffff654b, 0xffff4e70, 0xffe568ff, 0xffff4545};

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        var client = Minecraft.getInstance();
        var config = UltraStyleClient.config;
        if (!config.hudEnabled || client.player == null || client.options.hideGui) return;
        StyleState state = UltraStyleClient.state;
        float scale = Math.min(config.hudScale, Math.min(graphics.guiWidth() / 180f, graphics.guiHeight() / 200f));
        int width = 150;
        int x = Math.max(0, Math.round(graphics.guiWidth() - width * scale - config.rightMargin));
        int y = Math.clamp(graphics.guiHeight() / 2 - Math.round(65 * scale) + config.verticalOffset, 0,
            Math.max(0, graphics.guiHeight() - Math.round(180 * scale)));
        int rank = state.rank();
        int color = COLORS[rank];
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y).scale(scale, scale);
        graphics.fill(0, 0, width, 72, 0xc0141218);
        graphics.fill(0, 0, 3, 72, color);
        graphics.text(client.font, "S T Y L E", 10, 7, 0xffaaaaaf, false);

        float rankScale = rank == 7 ? 1.65f : 2.8f;
        if (!state.notices().isEmpty()) {
            float age = state.time() - state.notices().getFirst().tick() + delta.getGameTimeDeltaPartialTick(false);
            rankScale += Math.max(0, 1 - age / 6) * 0.15f;
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(10, 20).scale(rankScale, rankScale);
        graphics.text(client.font, StyleState.RANKS[rank], 0, 0, color, true);
        graphics.pose().popMatrix();
        graphics.text(client.font, StyleState.TITLES[rank], 10, 48, 0xffeeeeef, false);
        graphics.fill(10, 62, width - 10, 66, 0xff39343e);
        graphics.fill(10, 62, 10 + Math.round((width - 20) * state.progress()), 66, color);
        graphics.text(client.font, "SCORE " + state.total(), 5, 79, 0xffeeeeef, true);
        if (state.combo() > 1) {
            String combo = "x" + state.combo();
            graphics.text(client.font, combo, width - client.font.width(combo) - 5, 79, color, true);
        }
        int row = 96;
        for (StyleState.Notice notice : state.notices()) {
            float age = state.time() - notice.tick() + delta.getGameTimeDeltaPartialTick(false);
            int alpha = Math.clamp((int) ((100 - age) / 20f * 255), 0, 255);
            if (alpha < 5) continue;
            int rgb = notice.points() < 0 ? 0xff5b59 : 0xe4e4e8;
            String text = (notice.points() < 0 ? "- " : "+ ") + notice.label();
            int slide = (int) (Math.max(0, 6 - age) * 2);
            graphics.text(client.font, text, 5 + slide, row, (alpha << 24) | rgb, true);
            row += 13;
        }
        graphics.pose().popMatrix();
    }
}
