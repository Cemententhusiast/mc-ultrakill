package dev.mika.ultrastyle;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.GameType;

/** Reproducible README captures. All overlays are rendered in-game from real combat events. */
public class ShowcaseGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            var server = world.getServer();
            server.runCommand("difficulty normal");
            server.runCommand("time set noon");
            server.runCommand("weather clear");
            server.runCommand("execute at @p run fill ~-18 ~-1 ~-8 ~18 ~-1 ~30 minecraft:polished_deepslate");
            server.runCommand("execute at @p run fill ~-16 ~ ~28 ~16 ~8 ~29 minecraft:deepslate_bricks");
            for (int x : new int[]{-16, -8, 0, 8, 16}) {
                server.runCommand("execute at @p run fill ~" + x + " ~ ~27 ~" + x + " ~10 ~27 minecraft:chiseled_deepslate");
                server.runCommand("execute at @p run setblock ~" + x + " ~11 ~27 minecraft:sea_lantern");
            }
            for (int x : new int[]{-13, 13}) {
                server.runCommand("execute at @p run fill ~" + x + " ~-1 ~-5 ~" + x + " ~-1 ~25 minecraft:sea_lantern");
            }
            server.runCommand("give @p minecraft:diamond_sword");
            // Pre-grant advancements before the photo session so their toasts can clear.
            server.runCommand("advancement grant @p everything");
            server.runOnServer(s -> {
                var p = s.getPlayerList().getPlayers().getFirst();
                p.setGameMode(GameType.SURVIVAL);
                p.setHealth(p.getMaxHealth());
            });
            world.getConnection().waitForChunksRender();
            context.waitTicks(220);
            context.runOnClient(c -> {
                c.getWindow().setWindowed(1600, 900);
                c.options.guiScale().set(3);
                c.player.setYRot(0);
                c.player.setXRot(0);
                UltraStyleClient.state = new StyleState();
                FightResults.reset(c);
            });
            var id = server.computeOnServer(s -> {
                var p = s.getPlayerList().getPlayers().getFirst();
                var b = EntityTypes.WITHER.create(p.level(), EntitySpawnReason.COMMAND);
                b.setNoAi(true);
                b.setPos(p.getX(), p.getY() + 2, p.getZ() + 12);
                p.level().addFreshEntity(b);
                return b.getUUID();
            });
            context.waitTicks(30);
            server.runOnServer(s -> {
                var p = s.getPlayerList().getPlayers().getFirst();
                var b = (WitherBoss) p.level().getEntity(id);
                b.hurtServer(p.level(), p.damageSources().playerAttack(p), 80);
                p.hurtServer(p.level(), p.damageSources().generic(), 4);
            });
            context.waitTicks(25);
            // Kill actual mobs with player-attributed damage to build the style meter.
            for (int i = 0; i < 6; i++) {
                server.runOnServer(s -> {
                    var p = s.getPlayerList().getPlayers().getFirst();
                    var mob = EntityTypes.HUSK.create(p.level(), EntitySpawnReason.COMMAND);
                    mob.setNoAi(true);
                    mob.setPos(p.getX() - 5, p.getY(), p.getZ() + 6);
                    p.level().addFreshEntity(mob);
                    mob.hurtServer(p.level(), p.damageSources().playerAttack(p), 1000);
                });
                context.waitTicks(8);
            }
            context.waitTicks(22);
            shot(context, "showcase-style-boss");
            server.runOnServer(s -> {
                var p = s.getPlayerList().getPlayers().getFirst();
                var b = (WitherBoss) p.level().getEntity(id);
                b.hurtServer(p.level(), p.damageSources().playerAttack(p), 1000);
            });
            context.waitFor(c -> FightResults.bossDisplay() != null);
            context.waitTicks(160);
            shot(context, "showcase-boss-results");
            server.runOnServer(s -> {
                var p = s.getPlayerList().getPlayers().getFirst();
                p.hurtServer(p.level(), p.damageSources().genericKill(), 1000);
            });
            context.waitForScreen(DeathScreen.class);
            context.waitTicks(30);
            shot(context, "showcase-life-results");
        }
    }
    private static void shot(ClientGameTestContext context, String name) {
        context.runOnClient(c -> { c.gui.toastManager().clear(); c.gui.hud.getChat().clearMessages(true); });
        context.waitTicks(2);
        context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
    }
}
