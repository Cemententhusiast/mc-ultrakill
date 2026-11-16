package dev.mika.ultrastyle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FightResults {
    public record Display(CombatResults.Result result, long best, long shownAt) {}
    private record Boss(UUID id, String name, String type, String difficulty) {}
    private static CombatResults tracker = new CombatResults();
    private static final ArrayDeque<Display> completed = new ArrayDeque<>();
    private static final Map<UUID, Integer> observedBosses = new HashMap<>();
    private static final ResultRecords records = new ResultRecords();
    private static Object connection;
    private static ClientLevel level;
    private static Display death;
    private static Object deceasedPlayer;
    private static long displayClock;

    public static boolean isBoss(Entity entity) {
        return entity instanceof LivingEntity && UltraStyleClient.config.bossEntityTypes.contains(
            BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
    }
    private static Boss boss(Entity entity) {
        if (entity == null || !isBoss(entity)) return null;
        return new Boss(entity.getUUID(), entity.getName().getString(),
            BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(), entity.level().getDifficulty().name().toLowerCase(java.util.Locale.ROOT));
    }
    public static void ensure(Minecraft client) {
        if (connection != client.getConnection()) {
            connection = client.getConnection(); tracker = new CombatResults();
            completed.clear(); observedBosses.clear(); death = null; deceasedPlayer = null; displayClock = 0;
        }
        if (level != client.level) {
            level = client.level; tracker.abandonFights(); observedBosses.clear(); completed.clear();
        }
        if (client.player != null && client.player.isAlive() && client.player != deceasedPlayer && client.level != null && !tracker.hasLife()) {
            tracker.startLife(client.level.getDifficulty().name().toLowerCase(java.util.Locale.ROOT), client.hasSingleplayerServer(), UltraStyleClient.state);
            death = null;
        }
    }
    public static void tick(Minecraft client) {
        ensure(client);
        if (client.level == null || client.player == null || client.isPaused()) return;
        displayClock++;
        tracker.sample(UltraStyleClient.state);
        if (client.player.isDeadOrDying() && !client.hasSingleplayerServer()) playerDied();
        // Unloading/escaping never produces a victory. Existing life stats remain intact.
        observedBosses.entrySet().removeIf(entry -> {
            Entity entity = client.level.getEntity(entry.getValue());
            boolean gone = entity == null || !entity.getUUID().equals(entry.getKey())
                || entity.distanceToSqr(client.player) > 256 * 256;
            if (gone) tracker.abandonBoss(entry.getKey());
            return gone;
        });
        if (!completed.isEmpty() && displayClock - completed.peekFirst().shownAt() > 300) {
            completed.removeFirst();
            if (!completed.isEmpty()) {
                Display next = completed.removeFirst();
                completed.addFirst(new Display(next.result(), next.best(), displayClock));
            }
        }
    }
    private static void begin(Boss boss, boolean exact) {
        if (boss != null) tracker.beginBoss(boss.id(), boss.name(), boss.type(), boss.difficulty(), exact, UltraStyleClient.state);
    }

    /** Called immediately after actual health is removed, including the lethal hit, on the server thread. */
    public static void serverDamage(LivingEntity target, DamageSource source, float healthLost) {
        if (healthLost <= 0) return;
        Boss targetBoss = boss(target), attackerBoss = boss(source.getEntity());
        UUID targetId = target.getUUID();
        int targetEntityId = target.getId();
        if (source.getEntity() instanceof ServerPlayer player && !player.isCreative() && !player.isSpectator()) {
            UltraStyleClient.integrated(player, () -> {
                begin(targetBoss, true);
                if (targetBoss != null) observedBosses.put(targetId, targetEntityId);
                tracker.dealt(targetId, healthLost);
            });
        }
        if (target instanceof ServerPlayer player) {
            int attackerId = source.getEntity() == null ? -1 : source.getEntity().getId();
            UltraStyleClient.integrated(player, () -> {
                begin(attackerBoss, true);
                if (attackerBoss != null) observedBosses.put(attackerBoss.id(), attackerId);
                tracker.taken(healthLost);
            });
        }
    }

    /** Damage packets lack an amount. Use them only to establish contact; never invent damage totals. */
    public static void remoteContact(Entity target, Entity attacker) {
        Minecraft client = Minecraft.getInstance();
        if (client.hasSingleplayerServer() || client.player == null || client.player.isCreative() || client.player.isSpectator()) return;
        Entity candidate = attacker == client.player ? target : target == client.player ? attacker : null;
        Boss boss = boss(candidate);
        if (boss != null) { begin(boss, false); observedBosses.put(boss.id(), candidate.getId()); }
    }
    public static void killed(UUID id, boolean boss, boolean credited) {
        if (credited) tracker.kill(boss);
        tracker.sample(UltraStyleClient.state);
        CombatResults.Result result = tracker.finishBoss(id, UltraStyleClient.state);
        observedBosses.remove(id);
        if (result != null) {
            if (completed.size() == 4) completed.removeLast();
            completed.addLast(new Display(result, records.record(result), displayClock));
        }
    }
    public static void playerDied() {
        CombatResults.Result result = tracker.finishLife(UltraStyleClient.state);
        if (result != null) {
            death = new Display(result, records.record(result), displayClock);
            deceasedPlayer = Minecraft.getInstance().player;
        }
        completed.clear(); observedBosses.clear();
    }
    public static Display bossDisplay() { return tracker.fighting() ? null : completed.peekFirst(); }
    public static Display deathDisplay() { return death; }
    public static long clock() { return displayClock; }
    public static void dismiss() { completed.clear(); }
    public static void reset(Minecraft client) {
        tracker = new CombatResults(); completed.clear(); observedBosses.clear(); death = null; deceasedPlayer = null;
        ensure(client);
    }
}
