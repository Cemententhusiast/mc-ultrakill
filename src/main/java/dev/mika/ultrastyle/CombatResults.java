package dev.mika.ultrastyle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Client-thread combat accounting. Snapshots are immutable and survive score decay/reset. */
public final class CombatResults {
    public record Result(String name, String key, String difficulty, boolean life, boolean exact,
                         long ticks, long score, double peakStyle, int peakRank, int peakCombo,
                         double dealt, double taken, int kills, int bosses) {}
    private static final class Run {
        final String name, key, difficulty;
        final long start, baseline;
        final boolean exact;
        double dealt, taken, peakStyle;
        int peakRank, peakCombo, kills, bosses;
        Run(String name, String key, String difficulty, long tick, long score, boolean exact) {
            this.name = name; this.key = key; this.difficulty = difficulty;
            start = tick; baseline = score; this.exact = exact;
        }
        void sample(StyleState style) {
            peakStyle = Math.max(peakStyle, style.style());
            peakRank = Math.max(peakRank, style.rank());
            peakCombo = Math.max(peakCombo, style.combo());
        }
        Result finish(StyleState style, boolean life) {
            sample(style);
            return new Result(name, key, difficulty, life, exact, Math.max(0, style.time() - start),
                Math.max(0, style.total() - baseline), peakStyle, peakRank, peakCombo, dealt, taken, kills, bosses);
        }
    }
    private Run life;
    private final Map<UUID, Run> fights = new HashMap<>();

    public void startLife(String difficulty, boolean exact, StyleState style) {
        fights.clear();
        life = new Run("YOUR LIFE", "life", difficulty, style.time(), style.total(), exact);
        life.sample(style);
    }
    public void beginBoss(UUID id, String name, String type, String difficulty, boolean exact, StyleState style) {
        if (life == null || fights.containsKey(id)) return;
        if (fights.size() >= 16) return;
        Run run = new Run(name, type, difficulty, style.time(), style.total(), exact);
        run.sample(style);
        fights.put(id, run);
    }
    public void sample(StyleState style) {
        if (life != null) life.sample(style);
        fights.values().forEach(run -> run.sample(style));
    }
    public void dealt(UUID target, double amount) {
        if (life == null || !Double.isFinite(amount) || amount <= 0) return;
        life.dealt += amount;
        Run fight = fights.get(target);
        if (fight != null) fight.dealt += amount;
    }
    public void taken(double amount) {
        if (life == null || !Double.isFinite(amount) || amount <= 0) return;
        life.taken += amount;
        fights.values().forEach(run -> run.taken += amount);
    }
    public void kill(boolean boss) {
        if (life == null) return;
        life.kills++;
        if (boss) life.bosses++;
        fights.values().forEach(run -> { run.kills++; if (boss) run.bosses++; });
    }
    public Result finishBoss(UUID id, StyleState style) {
        Run fight = fights.remove(id);
        return fight == null ? null : fight.finish(style, false);
    }
    public Result finishLife(StyleState style) {
        if (life == null) return null;
        Result result = life.finish(style, true);
        life = null;
        fights.clear();
        return result;
    }
    public void abandonFights() { fights.clear(); }
    public void abandonBoss(UUID id) { fights.remove(id); }
    public boolean hasLife() { return life != null; }
    public boolean fighting() { return !fights.isEmpty(); }
}
