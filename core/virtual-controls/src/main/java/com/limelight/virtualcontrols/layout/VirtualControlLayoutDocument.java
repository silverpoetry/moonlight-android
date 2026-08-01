package com.limelight.virtualcontrols.layout;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Serialized virtual-control layout with a bounded persistence contract.
 */
public final class VirtualControlLayoutDocument {
    public static final int MAX_UTF8_BYTES = 1024 * 1024;

    private final String json;

    private VirtualControlLayoutDocument(String json) {
        this.json = json;
    }

    public static VirtualControlLayoutDocument fromJson(String json) {
        Objects.requireNonNull(json, "json");
        int byteCount = json.getBytes(StandardCharsets.UTF_8).length;
        if (byteCount > MAX_UTF8_BYTES) {
            throw new IllegalArgumentException(
                    "Virtual-control layout exceeds " +
                            MAX_UTF8_BYTES +
                            " UTF-8 bytes");
        }
        return new VirtualControlLayoutDocument(json);
    }

    public String getJson() {
        return json;
    }
}
