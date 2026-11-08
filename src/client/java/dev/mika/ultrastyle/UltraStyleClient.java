package dev.mika.ultrastyle;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.state.BlockState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.*;

public final class UltraStyleClient implements ClientModInitializer {
    public static StyleConfig config;
    public static StyleState state = new StyleState();
    private static Object connection;
    private static ClientLevel trackedLevel;
    private static final Map<Integer, Hit> hits = new HashMap<>();
    private static final Map<BlockPos, PendingBlock> blocks = new HashMap<>();
    private static boolean dead;
    // Immutable snapshot crosses from the client thread to the integrated server thread.
    private record XpSnapshot(Object server, UUID player, int multiplier) {}
    private static volatile XpSnapshot xpSnapshot = new XpSnapshot(null, null, 1);
    private record Hit(UUID uuid, long tick) {}
    private record PendingBlock(int points, long tick) {}

    @Override public void onInitializeClient() {
        config = StyleConfig.load();
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.fromNamespaceAndPath("ultrastyle", "style"), StyleHud::render);
        ClientTickEvents.END_CLIENT_TICK.register(UltraStyleClient::tick);
        ClientPlayerBlockBreakEvents.AFTER.register((level, player, pos, oldState) -> {
            if (Minecraft.getInstance().hasSingleplayerServer() || player.isCreative() || player.isSpectator()) return;
            if (blocks.size() < 128) blocks.put(pos.immutable(), new PendingBlock(blockPoints(oldState), state.time()));
        });
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, oldState, blockEntity) -> {
            if (player instanceof ServerPlayer serverPlayer && !player.isCreative() && !player.isSpectator()) {
                int points = blockPoints(oldState);
                integrated(serverPlayer, () -> state.mine(points));
            }
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) integrated(player, () -> state.death());
            else if (entity instanceof Mob) {
                // Vanilla kill credit includes player-owned projectiles and recent indirect damage.
                Entity credit = source.getEntity();
                if (!(credit instanceof ServerPlayer)) credit = entity.getKillCredit();
                if (credit instanceof ServerPlayer player && !player.isCreative() && !player.isSpectator()) {
                    int points = entity instanceof Enemy ? 100 : 25;
                    integrated(player, () -> state.kill(points));
                }
            }
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, amount, blocked) -> {
            if (entity instanceof ServerPlayer player && !blocked && amount > 0)
                integrated(player, () -> state.damage());
        });
        registerCommands();
    }

    private static int blockPoints(BlockState block) {
        String id = BuiltInRegistries.BLOCK.getKey(block.getBlock()).getPath();
        return id.endsWith("_ore") || id.equals("ancient_debris") ? 20 : 1;
    }

    /** Called on the integrated server thread; never trusts a multiplayer client to award XP. */
    private static void integrated(ServerPlayer player, Runnable action) {
        var server = player.level().getServer();
        UUID uuid = player.getUUID();
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (client.getSingleplayerServer() != server || client.player == null || !client.player.getUUID().equals(uuid)) return;
            ensureSession(client);
            action.run();
            publishMultiplier(client);
        });
    }

    private static void ensureSession(Minecraft client) {
        if (connection != client.getConnection()) {
            connection = client.getConnection();
            state = new StyleState();
            xpSnapshot = new XpSnapshot(null, null, 1);
            dead = false;
        }
        if (trackedLevel != client.level) {
            trackedLevel = client.level;
            hits.clear(); blocks.clear(); dead = false;
        }
    }

    private static void tick(Minecraft client) {
        ensureSession(client);
        publishMultiplier(client);
        if (client.player == null || client.level == null || client.isPaused()) return;
        state.tick();
        publishMultiplier(client);
        if (client.hasSingleplayerServer()) return;
        if (client.player.isDeadOrDying() && !dead) { state.death(); hits.clear(); blocks.clear(); }
        dead = client.player.isDeadOrDying();
        hits.entrySet().removeIf(entry -> state.time() - entry.getValue().tick() > 100);
        var iterator = blocks.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (state.time() - entry.getValue().tick() < 10) continue;
            // A short settling window rejects ordinary server rollbacks, but is not authoritative.
            BlockPos pos = entry.getKey();
            if (!dead && client.level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                    && client.level.getBlockState(pos).isAir())
                state.mine(entry.getValue().points());
            iterator.remove();
        }
    }

    public static void damagePacket(ClientboundDamageEventPacket packet) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.hasSingleplayerServer()) return;
        ensureSession(client);
        if (packet.entityId() == client.player.getId()) {
            if (!client.player.isDeadOrDying()) state.damage();
            return;
        }
        Entity target = client.level.getEntity(packet.entityId());
        if (!(target instanceof Mob) || client.player.isCreative() || client.player.isSpectator()) return;
        if (packet.sourceCauseId() == client.player.getId()) {
            if (hits.size() < 256) hits.put(target.getId(), new Hit(target.getUUID(), state.time()));
        } else if (packet.sourceCauseId() != -1) {
            // Another known attacker took over; do not claim their kill.
            hits.remove(target.getId());
        }
    }

    public static void observedDeath(Entity entity) {
        Minecraft client = Minecraft.getInstance();
        if (client.hasSingleplayerServer() || !(entity instanceof Mob) || client.player == null || client.player.isDeadOrDying()) return;
        Hit hit = hits.remove(entity.getId());
        if (hit != null && hit.uuid().equals(entity.getUUID()) && state.time() - hit.tick() <= 100)
            state.kill(entity instanceof Enemy ? 100 : 25);
    }

    private static void publishMultiplier(Minecraft client) {
        boolean active = config != null && config.styleXpEnabled && client.player != null
            && client.player.isAlive() && !client.player.isCreative() && !client.player.isSpectator();
        xpSnapshot = new XpSnapshot(client.getSingleplayerServer(),
            client.player == null ? null : client.player.getUUID(), active ? state.multiplier() : 1);
    }

    /** Runs on the server thread. Remote servers and other LAN players never match the snapshot. */
    public static int multiplyExperience(ServerPlayer player, int amount) {
        XpSnapshot snapshot = xpSnapshot;
        if (snapshot.server() != player.level().getServer() || !player.getUUID().equals(snapshot.player())
                || !player.isAlive() || player.isCreative() || player.isSpectator()) return amount;
        return StyleState.multiplyExperience(amount, snapshot.multiplier());
    }

    private static int setXpEnabled(boolean enabled, FabricClientCommandSource source) {
        config.styleXpEnabled = enabled;
        config.save();
        Minecraft client = Minecraft.getInstance();
        publishMultiplier(client);
        source.sendFeedback(Component.literal(
            "Style XP multiplier: " + (enabled ? "on (single-player only)" : "off")));
        return 1;
    }

    private static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> dispatcher.register(literal("ultrastyle")
            .executes(ctx -> {
                ctx.getSource().sendFeedback(Component.literal("UltraStyle | style x" + state.multiplier() + " | XP " + (config.styleXpEnabled ? "rank multiplier (single-player only)" : "off") + " | hud, tilt <-15–15>, bosses, xp on/off, reset, reload")); return 1;
            })
            .then(literal("hud").executes(ctx -> { config.hudEnabled = !config.hudEnabled; config.save(); ctx.getSource().sendFeedback(Component.literal("Style HUD: " + config.hudEnabled)); return 1; }))
            .then(literal("tilt").then(argument("degrees", DoubleArgumentType.doubleArg(-15, 15)).executes(ctx -> {
                config.hudTiltDegrees = (float) DoubleArgumentType.getDouble(ctx, "degrees"); config.save();
                ctx.getSource().sendFeedback(Component.literal("Style HUD tilt: " + config.hudTiltDegrees + " degrees")); return 1;
            })))
            .then(literal("bosses").executes(ctx -> { config.bossBarsEnabled = !config.bossBarsEnabled; config.save(); ctx.getSource().sendFeedback(Component.literal("Custom boss bars: " + config.bossBarsEnabled)); return 1; }))
            .then(literal("xp")
                .then(literal("on").executes(ctx -> setXpEnabled(true, ctx.getSource())))
                .then(literal("off").executes(ctx -> setXpEnabled(false, ctx.getSource()))))
            .then(literal("reset").executes(ctx -> { state = new StyleState(); hits.clear(); blocks.clear(); publishMultiplier(Minecraft.getInstance()); ctx.getSource().sendFeedback(Component.literal("Style session reset.")); return 1; }))
            .then(literal("reload").executes(ctx -> { config = StyleConfig.load(); publishMultiplier(Minecraft.getInstance()); ctx.getSource().sendFeedback(Component.literal("UltraStyle configuration reloaded.")); return 1; }))
        ));
    }
}
