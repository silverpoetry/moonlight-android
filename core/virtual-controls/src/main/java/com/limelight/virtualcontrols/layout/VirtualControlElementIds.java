package com.limelight.virtualcontrols.layout;

import java.util.UUID;

/**
 * Identity policy for newly created virtual-control layout elements.
 *
 * <p>Existing layout IDs are opaque and remain unchanged. The historical
 * prefix is retained for compatibility, while UUID suffixes avoid the
 * millisecond collisions possible in the former UI-local generator.</p>
 */
public final class VirtualControlElementIds {
    private static final String HISTORICAL_PREFIX =
            "assemble_key_";

    private VirtualControlElementIds() {
    }

    public static String newId() {
        return HISTORICAL_PREFIX + UUID.randomUUID();
    }
}
