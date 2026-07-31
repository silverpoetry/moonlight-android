package com.limelight.binding.input;

import com.limelight.nvstream.jni.MoonBridge;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public final class ControllerTypeResolverTest {
    @Test
    public void knownVendorsBypassFallback() {
        AtomicInteger fallbackCalls = new AtomicInteger();
        ControllerTypeResolver.Fallback fallback =
                (vendorId, productId) -> {
                    fallbackCalls.incrementAndGet();
                    return MoonBridge.LI_CTYPE_UNKNOWN;
                };

        assertEquals(
                MoonBridge.LI_CTYPE_XBOX,
                ControllerTypeResolver.resolve(
                        0x045e,
                        1,
                        fallback));
        assertEquals(
                MoonBridge.LI_CTYPE_PS,
                ControllerTypeResolver.resolve(
                        0x054c,
                        2,
                        fallback));
        assertEquals(
                MoonBridge.LI_CTYPE_NINTENDO,
                ControllerTypeResolver.resolve(
                        0x057e,
                        3,
                        fallback));
        assertEquals(0, fallbackCalls.get());
    }

    @Test
    public void unknownIdentityDelegatesVendorAndProduct() {
        AtomicInteger observedVendor = new AtomicInteger();
        AtomicInteger observedProduct = new AtomicInteger();

        byte type = ControllerTypeResolver.resolve(
                0x1234,
                0xabcd,
                (vendorId, productId) -> {
                    observedVendor.set(vendorId);
                    observedProduct.set(productId);
                    return MoonBridge.LI_CTYPE_PS;
                });

        assertEquals(MoonBridge.LI_CTYPE_PS, type);
        assertEquals(0x1234, observedVendor.get());
        assertEquals(0xabcd, observedProduct.get());
    }
}
