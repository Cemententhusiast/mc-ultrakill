package dev.mika.ultrastyle;

/** Perspective projection of a panel yawed about its vertical center axis. No screen-plane roll. */
public final class HudPerspective {
    private final float width, height, cosine, sine;
    private static final float CAMERA_DISTANCE = 260;
    public HudPerspective(float width, float height, float degrees) {
        this.width = width;
        this.height = height;
        double angle = Math.toRadians(Math.clamp(degrees, -45, 45));
        cosine = (float) Math.cos(angle);
        sine = (float) Math.sin(angle);
    }
    private float depthScale(float x) {
        return CAMERA_DISTANCE / (CAMERA_DISTANCE - (x - width / 2) * sine);
    }
    public float x(float x) { return (x - width / 2) * cosine * depthScale(x); }
    public float y(float x, float y) { return (y - height / 2) * depthScale(x); }
    public float left() { return x(0); }
    public float right() { return x(width); }
    public float top() { return Math.min(y(0, 0), y(width, 0)); }
    public float bottom() { return Math.max(y(0, height), y(width, height)); }
}
