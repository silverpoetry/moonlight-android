package com.limelight.binding.input;

import com.limelight.nvstream.jni.MoonBridge;

import java.util.Objects;

/** Resolves the protocol controller family from USB identity. */
final class ControllerTypeResolver {
    private static final int MICROSOFT_VENDOR_ID = 0x045e;
    private static final int SONY_VENDOR_ID = 0x054c;
    private static final int NINTENDO_VENDOR_ID = 0x057e;

    interface Fallback {
        byte guess(int vendorId, int productId);
    }

    private ControllerTypeResolver() {
    }

    static byte resolve(
            int vendorId,
            int productId,
            Fallback fallback) {
        switch (vendorId) {
            case MICROSOFT_VENDOR_ID:
                return MoonBridge.LI_CTYPE_XBOX;
            case SONY_VENDOR_ID:
                return MoonBridge.LI_CTYPE_PS;
            case NINTENDO_VENDOR_ID:
                return MoonBridge.LI_CTYPE_NINTENDO;
            default:
                return Objects.requireNonNull(
                        fallback,
                        "fallback").guess(vendorId, productId);
        }
    }
}
