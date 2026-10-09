package com.limelight.ui.floatingview;

/** Bounds for translating content into the mouse hotspot's reachable rectangle. */
final class FloatingMousePanGeometry {
    private FloatingMousePanGeometry() { }

    static float directionalVelocity(float candidate, float movement, float previous) {
        // Viewport motion is opposite to the outward finger motion.
        if (candidate == 0 || candidate * movement > 0) return 0;
        if (movement == 0 && candidate * previous <= 0) return 0;
        return candidate;
    }

    static float edgeVelocity(float position, float travel, float zone, float maximumSpeed) {
        zone = Math.min(zone, travel / 2f);
        if (zone <= 0) return 0;
        float strength = position < zone ? 1 - position / zone
                : position > travel - zone ? -(1 - (travel - position) / zone) : 0;
        float magnitude = Math.min(1f, Math.abs(strength));
        return Math.copySign(maximumSpeed * magnitude * magnitude * (3 - 2 * magnitude), strength);
    }

    static float clamp(float translation, float start, float size, float pivot,
            float scale, float reachableStart, float reachableEnd) {
        float contentStart = start + pivot * (1f - scale);
        float contentEnd = contentStart + (size - 1f) * scale;
        // Keep zero legal: entering mouse mode must not jump a letterboxed image.
        float minimum = Math.min(0f, reachableEnd - contentEnd);
        float maximum = Math.max(0f, reachableStart - contentStart);
        return Math.max(minimum, Math.min(translation, maximum));
    }
}
