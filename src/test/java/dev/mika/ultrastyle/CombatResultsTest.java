package dev.mika.ultrastyle;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class CombatResultsTest {
    @Test void fightIncludesFinalHitKillAwardAndFrozenPeak() {
        var style = new StyleState(); var results = new CombatResults(); var boss = UUID.randomUUID();
        style.kill(100); // Earlier session score is not part of this encounter.
        results.startLife("hard", true, style);
        results.beginBoss(boss, "Wither", "minecraft:wither", "hard", true, style);
        results.dealt(boss, 15); results.taken(4);
        for (int i = 0; i < 40; i++) style.tick();
        style.kill(100); results.dealt(boss, 5); results.kill(true);
        var report = results.finishBoss(boss, style);
        assertEquals(150, report.score()); assertEquals(40, report.ticks());
        assertEquals(20, report.dealt()); assertEquals(4, report.taken());
        assertEquals(2, report.peakRank()); assertEquals(1, report.kills());
        style.death(); results.taken(20);
        assertEquals(250, report.peakStyle()); assertEquals(4, report.taken());
        assertNull(results.finishBoss(boss, style));
    }
    @Test void duplicateContactsDoNotResetTimerOrDamage() {
        var style = new StyleState(); var results = new CombatResults(); var boss = UUID.randomUUID();
        results.startLife("normal", true, style);
        results.beginBoss(boss, "Wither", "wither", "normal", true, style);
        results.dealt(boss, 8); style.tick();
        results.beginBoss(boss, "Wither", "wither", "normal", true, style);
        var report = results.finishBoss(boss, style);
        assertEquals(1, report.ticks()); assertEquals(8, report.dealt());
    }
    @Test void abandonedAndUnknownBossesNeverProduceVictory() {
        var style = new StyleState(); var results = new CombatResults(); var boss = UUID.randomUUID();
        results.startLife("normal", false, style);
        assertNull(results.finishBoss(boss, style));
        results.beginBoss(boss, "Wither", "wither", "normal", false, style);
        results.abandonFights();
        assertNull(results.finishBoss(boss, style));
        assertTrue(results.hasLife());
    }
    @Test void overlappingBossesKeepDamageDealtSeparate() {
        var style = new StyleState(); var results = new CombatResults();
        var a = UUID.randomUUID(); var b = UUID.randomUUID();
        results.startLife("hard", true, style);
        results.beginBoss(a, "A", "wither", "hard", true, style);
        results.beginBoss(b, "B", "wither", "hard", true, style);
        results.dealt(a, 10); results.dealt(b, 20); results.taken(3);
        assertEquals(10, results.finishBoss(a, style).dealt());
        var second = results.finishBoss(b, style);
        assertEquals(20, second.dealt()); assertEquals(3, second.taken());
        assertEquals(30, results.finishLife(style).dealt());
    }
    @Test void deathSnapshotsOnceAndRespawnStartsFreshLife() {
        var style = new StyleState(); var results = new CombatResults();
        results.startLife("hard", true, style);
        style.kill(100); results.kill(false); results.sample(style);
        results.taken(20);
        var death = results.finishLife(style);
        assertEquals(100, death.score()); assertEquals(20, death.taken());
        assertNull(results.finishLife(style));
        assertFalse(results.hasLife());
        style.death(); results.startLife("hard", true, style);
        var next = results.finishLife(style);
        assertEquals(0, next.score()); assertEquals(0, next.kills()); assertEquals(0, next.taken());
        assertEquals(1, death.kills());
    }
    @Test void unknownDamageStaysFlaggedAndInvalidSamplesAreIgnored() {
        var style = new StyleState(); var results = new CombatResults();
        results.startLife("normal", false, style);
        results.taken(Double.NaN); results.taken(-4); results.dealt(UUID.randomUUID(), Double.POSITIVE_INFINITY);
        var report = results.finishLife(style);
        assertFalse(report.exact()); assertEquals(0, report.taken()); assertEquals(0, report.dealt());
    }
}
