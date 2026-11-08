package dev.mika.ultrastyle;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StyleStateTest {
    private void advance(StyleState state, int ticks) { for (int i = 0; i < ticks; i++) state.tick(); }

    @Test void miningCapLimitsScoreAndXpAwardAndRenewsAfterTenSeconds() {
        var state = new StyleState();
        assertEquals(20, state.mine(20));
        assertEquals(20, state.mine(20));
        assertEquals(0, state.mine(20));
        assertEquals(40, state.total());
        advance(state, 200);
        assertEquals(20, state.mine(20));
        assertEquals(60, state.total());
    }

    @Test void groupedMiningNoticesNeverReawardPreviouslyEarnedXp() {
        var state = new StyleState();
        for (int i = 0; i < 10; i++) assertEquals(1, state.mine(1));
        assertEquals(10, state.total());
        assertEquals(1, state.notices().size());
        assertEquals(10, state.notices().getFirst().points());
    }

    @Test void partialMiningAwardHonorsRemainingBudget() {
        var state = new StyleState();
        state.mine(20); state.mine(1);
        assertEquals(19, state.mine(20));
        assertEquals(40, state.total());
    }

    @Test void multikillsExpireAndTheirBonusIsBounded() {
        var state = new StyleState();
        assertEquals(100, state.kill(100));
        assertEquals(150, state.kill(100));
        assertEquals(200, state.kill(100));
        assertEquals(250, state.kill(100));
        assertEquals(300, state.kill(100));
        assertEquals(300, state.kill(100));
        advance(state, 81);
        assertEquals(100, state.kill(100));
        assertEquals(1, state.combo());
    }

    @Test void damageAndDeathClearComboButPreserveEarnedScore() {
        var state = new StyleState();
        state.kill(100); state.kill(100);
        state.damage();
        assertEquals(150, state.style());
        assertEquals(0, state.combo());
        assertEquals(100, state.kill(100));
        state.death();
        assertEquals(0, state.style());
        assertEquals(0, state.combo());
        assertEquals(350, state.total());
    }

    @Test void styleDecaysAfterGracePeriodButSessionTotalDoesNot() {
        var state = new StyleState();
        state.kill(100);
        advance(state, 60);
        assertEquals(100, state.style());
        advance(state, 20);
        assertEquals(90, state.style());
        assertEquals(100, state.total());
        advance(state, 500);
        assertEquals(0, state.style());
        assertTrue(state.notices().isEmpty());
    }

    @Test void ranksStayInBoundsAtMaximumStyle() {
        var state = new StyleState();
        assertEquals(0, state.rank());
        for (int i = 0; i < 30; i++) state.kill(100);
        assertEquals(7, state.rank());
        assertEquals(1, state.progress());
        assertEquals(2200, state.style());
        assertEquals(5, state.notices().size());
    }
}
