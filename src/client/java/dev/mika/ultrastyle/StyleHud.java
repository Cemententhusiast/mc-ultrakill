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
        HudPerspective projection = new HudPerspective(width, height, config.hudYawDegrees);
        float projectedWidth = projection.right() - projection.left();
        float projectedHeight = projection.bottom() - projection.top();
        scale = Math.min(scale, Math.min(graphics.guiWidth() / projectedWidth, graphics.guiHeight() / projectedHeight));
        float originX = Math.clamp(graphics.guiWidth() - config.rightMargin - projection.right() * scale,
            -projection.left() * scale, Math.max(-projection.left() * scale, graphics.guiWidth() - projection.right() * scale));
        float originY = Math.clamp(graphics.guiHeight() / 2f + 25 * scale + config.verticalOffset,
            -projection.top() * scale, Math.max(-projection.top() * scale, graphics.guiHeight() - projection.bottom() * scale));
        PerspectiveHudCanvas canvas = new PerspectiveHudCanvas(graphics, projection, originX, originY, scale);
        int rank = state.rank();
        int color = COLORS[rank];
        canvas.fill(0, 0, width, 72, 0xc0141218);
        canvas.fill(0, 0, 3, 72, color);
        canvas.text(client.font, "S T Y L E", 10, 7, 0xffaaaaaf, false);

        float rankScale = rank == 7 ? 1.65f : 2.8f;
        if (!state.notices().isEmpty()) {
            float age = state.time() - state.notices().getFirst().tick() + delta.getGameTimeDeltaPartialTick(false);
            rankScale += Math.max(0, 1 - age / 6) * 0.15f;
        }
        canvas.pose().pushMatrix();
        canvas.pose().translate(10, 20).scale(rankScale, rankScale);
        canvas.text(client.font, StyleState.RANKS[rank], 0, 0, color, true);
        canvas.pose().popMatrix();
        canvas.text(client.font, StyleState.TITLES[rank], 10, 48, 0xffeeeeef, false);
        canvas.fill(10, 62, width - 10, 66, 0xff39343e);
        canvas.fill(10, 62, 10 + Math.round((width - 20) * state.progress()), 66, color);
        boolean appliesToXp = client.hasSingleplayerServer() && config.styleXpEnabled
            && client.player.isAlive() && !client.player.isCreative() && !client.player.isSpectator();
        canvas.text(client.font, (appliesToXp ? "XP x" : "STYLE x") + state.multiplier(), 5, 79, color, true);
        if (state.combo() > 1) {
            String combo = state.combo() + " KILLS";
            canvas.text(client.font, combo, width - client.font.width(combo) - 5, 79, color, true);
        }
        int row = 96;
        for (StyleState.Notice notice : state.notices()) {
            float age = state.time() - notice.tick() + delta.getGameTimeDeltaPartialTick(false);
            int alpha = Math.clamp((int) ((100 - age) / 20f * 255), 0, 255);
            if (alpha < 5) continue;
            int rgb = notice.points() < 0 ? 0xff5b59 : 0xe4e4e8;
            String text = (notice.points() < 0 ? "- " : "+ ") + notice.label();
            int slide = (int) (Math.max(0, 6 - age) * 2);
            canvas.text(client.font, text, 5 + slide, row, (alpha << 24) | rgb, true);
            row += 13;
        }
    }
}
