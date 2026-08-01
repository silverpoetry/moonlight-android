package com.limelight.virtualcontrols.layout;

import java.io.IOException;

/**
 * Persistence port for editable virtual-control layout documents.
 */
public interface VirtualControlLayoutRepository {
    VirtualControlLayoutReadResult load(
            VirtualControlLayoutKey key) throws IOException;

    void save(
            VirtualControlLayoutKey key,
            VirtualControlLayoutDocument document) throws IOException;
}
