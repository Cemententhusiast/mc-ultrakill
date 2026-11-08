package dev.mika.ultrastyle;

import java.util.ArrayDeque;
import java.util.List;

/** Pure scoring rules. All times are game ticks; rendering never changes the score. */
public final class StyleState {
    public record Notice(String label, int points, long tick) {}
    public static final int[] THRESHOLDS = {0, 100, 250, 450, 700, 1000, 1400, 1800};
    public static final String[] RANKS = {"D", "C", "B", "A", "S", "SS", "SSS", "ULTRAKILL"};
    public static final String[] TITLES = {"DESTRUCTIVE", "CHAOTIC", "BRUTAL", "ANARCHIC", "SUPREME", "SSADISTIC", "SSSHITSTORM", "MAXIMUM STYLE"};
    private final ArrayDeque<Notice> notices = new ArrayDeque<>();
    private long tick, total, lastAction = -1000, lastKill = -1000, miningWindow;
    private int combo, miningPoints;
    private double style;

    public void tick() {
        tick++;
        if (tick - lastAction > 60) style = Math.max(0, style - 0.5);
        if (tick - lastKill > 80) combo = 0;
        while (!notices.isEmpty() && tick - notices.peekLast().tick() > 100) notices.removeLast();
    }

    public int kill(int base) {
        combo = tick - lastKill <= 80 ? combo + 1 : 1;
        lastKill = tick;
        int bonus = Math.min(4, combo - 1) * 50;
        return award(combo > 1 ? "MULTIKILL x" + combo : "KILL", base + bonus);
    }

    public int mine(int base) {
        if (tick - miningWindow >= 200) { miningWindow = tick; miningPoints = 0; }
        int accepted = Math.min(base, 40 - miningPoints);
        if (accepted <= 0) return 0;
        miningPoints += accepted;
        return award(base > 1 ? "ORE MINED" : "BLOCK BREAK", accepted);
    }

    private int award(String label, int points) {
        int awarded = points;
        total += points;
        style = Math.min(2200, style + points);
        lastAction = tick;
        // Collapse rapid mining entries instead of filling the screen with one-point notices.
        if (label.equals("BLOCK BREAK") && !notices.isEmpty() && notices.peekFirst().label().equals(label)
                && tick - notices.peekFirst().tick() < 20) points += notices.removeFirst().points();
        push(label, points);
        return awarded;
    }

    public void damage() { style = Math.max(0, style - 100); combo = 0; lastKill = -1000; push("DAMAGE", -100); }
    public void death() { style = 0; combo = 0; lastKill = -1000; push("COMBO LOST", 0); }
    private void push(String label, int points) {
        notices.addFirst(new Notice(label, points, tick));
        while (notices.size() > 5) notices.removeLast();
    }
    public int rank() {
        for (int i = THRESHOLDS.length - 1; i >= 0; i--) if (style >= THRESHOLDS[i]) return i;
        return 0;
    }
    public float progress() {
        int rank = rank();
        return rank == THRESHOLDS.length - 1 ? 1 : (float) ((style - THRESHOLDS[rank]) / (THRESHOLDS[rank + 1] - THRESHOLDS[rank]));
    }
    public long time() { return tick; }
    public long total() { return total; }
    public double style() { return style; }
    public int combo() { return combo; }
    public List<Notice> notices() { return List.copyOf(notices); }
}
