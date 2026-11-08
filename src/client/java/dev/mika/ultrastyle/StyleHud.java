package dev.mika.ultrastyle;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class StyleHud {
    private static final int[] COLORS = {0xff8fb9e8, 0xff65dca0, 0xfff4d664, 0xffffa74f, 0xffff654b, 0xffff4e70, 0xffe568ff, 0xffff4545};

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        var client = Minecraft.getInstance();
        var config = UltraStyleClient.config;
        if (!config.hudEnabled || client.player == null || client.gui.hud.isHidden()) return;
        StyleState state = UltraStyleClient.state;
        float scale = Math.min(config.hudScale, Math.min(graphics.guiWidth() / 180f, graphics.guiHeight() / 200f));
        int width = 150;
        int height = 180; // Reserve space for all five feed entries, even while empty.
        float angle = (float) Math.toRadians(config.hudTiltDegrees);
        float cos = Math.abs((float) Math.cos(angle));
        float sin = Math.abs((float) Math.sin(angle));
        float rotatedWidth = width * cos + height * sin;
        float rotatedHeight = width * sin + height * cos;
        // Fit the rotated rectangle, not just the original panel, inside the viewport.
        scale = Math.min(scale, Math.min(graphics.guiWidth() / rotatedWidth, graphics.guiHeight() / rotatedHeight));
        float halfWidth = rotatedWidth * scale / 2;
        float halfHeight = rotatedHeight * scale / 2;
        float centerX = Math.clamp(graphics.guiWidth() - config.rightMargin - halfWidth,
            halfWidth, Math.max(halfWidth, graphics.guiWidth() - halfWidth));
        float centerY = Math.clamp(graphics.guiHeight() / 2f + 25 * scale + config.verticalOffset,
            halfHeight, Math.max(halfHeight, graphics.guiHeight() - halfHeight));
        int rank = state.rank();
        int color = COLORS[rank];
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY).scale(scale, scale)
            .rotate(angle).translate(-width / 2f, -height / 2f);
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
        boolean appliesToXp = client.hasSingleplayerServer() && config.styleXpEnabled
            && client.player.isAlive() && !client.player.isCreative() && !client.player.isSpectator();
        graphics.text(client.font, (appliesToXp ? "XP x" : "STYLE x") + state.multiplier(), 5, 79, color, true);
        if (state.combo() > 1) {
            String combo = state.combo() + " KILLS";
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
