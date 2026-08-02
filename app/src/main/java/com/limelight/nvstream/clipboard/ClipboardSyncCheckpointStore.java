package com.limelight.nvstream.clipboard;

/** Persistence port for the clipboard loop-suppression checkpoint. */
public interface ClipboardSyncCheckpointStore {
    ClipboardSyncCheckpoint read();

    void write(ClipboardSyncCheckpoint checkpoint);
}
