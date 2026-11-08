package dev.mika.ultrastyle;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudPerspectiveTest {
    @Test void zeroYawIsFlatAndCentered() {
        var projection = new HudPerspective(150, 180, 0);
        assertEquals(-75, projection.x(0), 0.001f);
        assertEquals(75, projection.x(150), 0.001f);
        assertEquals(-90, projection.y(0, 0), 0.001f);
        assertEquals(-90, projection.y(150, 0), 0.001f);
    }

    @Test void rightEdgeIsNearerAndTallerWithoutRotatingThePanel() {
        var projection = new HudPerspective(150, 180, 30);
        float leftHeight = projection.y(0, 180) - projection.y(0, 0);
        float rightHeight = projection.y(150, 180) - projection.y(150, 0);
        assertTrue(rightHeight > leftHeight);
        // Top rises toward the right, bottom falls: perspective, not a rolled rectangle.
        assertTrue(projection.y(150, 0) < projection.y(0, 0));
        assertTrue(projection.y(150, 180) > projection.y(0, 180));
        assertEquals(0, projection.y(0, 90), 0.001f);
        assertEquals(0, projection.y(150, 90), 0.001f);
    }

    @Test void negativeYawReversesDepth() {
        var projection = new HudPerspective(150, 180, -30);
        assertTrue(projection.y(0, 180) > projection.y(150, 180));
    }

    @Test void projectedBoundsContainPanelAtSupportedAngles() {
        for (int angle = -45; angle <= 45; angle++) {
            var projection = new HudPerspective(150, 180, angle);
            for (int x = 0; x <= 150; x += 5) {
                assertTrue(Float.isFinite(projection.x(x)));
                assertTrue(projection.x(x) >= projection.left() - 0.001f);
                assertTrue(projection.x(x) <= projection.right() + 0.001f);
                assertTrue(projection.y(x, 0) >= projection.top() - 0.001f);
                assertTrue(projection.y(x, 180) <= projection.bottom() + 0.001f);
            }
        }
    }
}
