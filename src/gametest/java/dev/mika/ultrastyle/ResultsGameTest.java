package dev.mika.ultrastyle;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.GameType;

/** Exercises actual damage/death hooks and vanilla respawn controls in a disposable world. */
public class ResultsGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("difficulty normal");
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.SURVIVAL);
                player.setHealth(player.getMaxHealth());
            });
            context.waitTicks(10);
            context.runOnClient(client -> { UltraStyleClient.state = new StyleState(); FightResults.reset(client); });
            var id = world.getServer().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                WitherBoss boss = EntityTypes.WITHER.create(player.level(), EntitySpawnReason.COMMAND);
                if (boss == null) throw new AssertionError("Wither creation failed");
                boss.setNoAi(true);
                boss.setPos(player.getX() + 4, player.getY() + 2, player.getZ());
                boss.setHealth(40);
                player.level().addFreshEntity(boss);
                return boss.getUUID();
            });
            context.waitTicks(20);
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                var boss = (WitherBoss) player.level().getEntity(id);
                boss.hurtServer(player.level(), player.damageSources().playerAttack(player), 10);
                player.hurtServer(player.level(), player.damageSources().generic(), 4);
            });
            context.waitTicks(25);
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                var boss = (WitherBoss) player.level().getEntity(id);
                boss.hurtServer(player.level(), player.damageSources().playerAttack(player), 1000);
            });
            context.waitFor(client -> FightResults.bossDisplay() != null);
            context.runOnClient(client -> {
                var result = FightResults.bossDisplay().result();
                require(result.dealt() == 40, "Boss damage must include the lethal hit and exclude overkill: " + result.dealt());
                require(result.taken() == 4, "Actual player health loss was not recorded");
                require(result.score() >= 100 && result.kills() == 1, "Final kill award missing");
            });
            context.waitTicks(110); // Let death animations and advancement toasts clear for visual QA.
            context.takeScreenshot("boss-results");
            float fatalHealth = world.getServer().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                float health = player.getHealth();
                player.hurtServer(player.level(), player.damageSources().genericKill(), 1000);
                return health;
            });
            context.waitForScreen(DeathScreen.class);
            context.waitTicks(25);
            context.runOnClient(client -> {
                var result = FightResults.deathDisplay();
                require(result != null && result.result().life(), "Lifetime report missing");
                require(result.result().taken() == 4 + fatalHealth, "Death report must include fatal health loss: " + result.result().taken() + " expected " + (4 + fatalHealth));
                require(result.result().bosses() == 1, "Boss kill missing from lifetime stats");
            });
            context.takeScreenshot("life-results");
            context.clickScreenButton("Respawn");
            context.waitFor(client -> client.player != null && client.player.isAlive() && client.gui.screen() == null);
            context.waitTicks(5);
            context.runOnClient(client -> require(FightResults.deathDisplay() == null, "Old life report survived respawn"));
        }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
