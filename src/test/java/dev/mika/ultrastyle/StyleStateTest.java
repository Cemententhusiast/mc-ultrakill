package dev.mika.ultrastyle;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StyleStateTest {
    private void advance(StyleState state, int ticks) { for (int i = 0; i < ticks; i++) state.tick(); }

    @Test void miningCapLimitsStyleAndRenewsAfterTenSeconds() {
        var state = new StyleState();
        assertEquals(20, state.mine(20));
        assertEquals(20, state.mine(20));
        assertEquals(0, state.mine(20));
        assertEquals(40, state.total());
        advance(state, 200);
        assertEquals(20, state.mine(20));
        assertEquals(60, state.total());
    }

    @Test void groupedMiningNoticesNeverReawardPreviouslyEarnedStyle() {
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

    @Test void rankMultiplierRespondsToActionsDamageDecayAndDeath() {
        var state = new StyleState();
        assertEquals(1, state.multiplier());
        state.kill(100);
        assertEquals(2, state.multiplier());
        state.kill(100);
        assertEquals(3, state.multiplier());
        state.damage();
        assertEquals(2, state.multiplier());
        advance(state, 161);
        assertEquals(1, state.multiplier());
        for (int i = 0; i < 20; i++) state.kill(100);
        assertEquals(8, state.multiplier());
        state.death();
        assertEquals(1, state.multiplier());
    }

    @Test void incomingExperienceIsMultipliedInsteadOfAddingStylePoints() {
        assertEquals(5, StyleState.multiplyExperience(5, 1));
        assertEquals(20, StyleState.multiplyExperience(5, 4));
        assertEquals(40, StyleState.multiplyExperience(5, 8));
        assertEquals(0, StyleState.multiplyExperience(0, 8));
        assertEquals(-5, StyleState.multiplyExperience(-5, 8));
    }

    @Test void experienceMultiplierIsBoundedAndCannotOverflow() {
        assertEquals(5, StyleState.multiplyExperience(5, -1));
        assertEquals(40, StyleState.multiplyExperience(5, 100));
        assertEquals(Integer.MAX_VALUE, StyleState.multiplyExperience(Integer.MAX_VALUE, 8));
    }
}
