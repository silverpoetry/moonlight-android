package com.limelight.ui.filepush;

import java.util.Objects;

/** Explicit result of selecting a usable upload target. */
public final class FilePushTargetSelection {
    private static final FilePushTargetSelection MISSING =
            new FilePushTargetSelection(null);

    private final FilePushTarget target;

    private FilePushTargetSelection(FilePushTarget target) {
        this.target = target;
    }

    public static FilePushTargetSelection missing() {
        return MISSING;
    }

    public static FilePushTargetSelection found(
            FilePushTarget target) {
        return new FilePushTargetSelection(
                Objects.requireNonNull(target, "target"));
    }

    public boolean isFound() {
        return target != null;
    }

    public FilePushTarget getTarget() {
        if (target == null) {
            throw new IllegalStateException(
                    "Missing selection has no target");
        }
        return target;
    }
}
