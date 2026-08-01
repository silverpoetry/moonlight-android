package com.limelight.stream.launch;

import java.util.Objects;

/** Activity-scoped admission gate for one outgoing stream launch. */
public final class StreamLaunchController {
    public enum Status {
        STARTED,
        ALREADY_STARTING,
        OWNER_DESTROYED
    }

    public interface LaunchAction {
        void launch();
    }

    private boolean starting;
    private boolean ownerPaused;
    private boolean destroyed;

    public Status launch(LaunchAction action) {
        Objects.requireNonNull(action, "action");
        synchronized (this) {
            if (destroyed) {
                return Status.OWNER_DESTROYED;
            }
            if (starting) {
                return Status.ALREADY_STARTING;
            }
            starting = true;
        }
        try {
            action.launch();
            return Status.STARTED;
        }
        catch (RuntimeException | Error failure) {
            synchronized (this) {
                if (!destroyed) {
                    starting = false;
                }
            }
            throw failure;
        }
    }

    /** Records that a launched destination has covered the owner. */
    public synchronized void onOwnerPaused() {
        if (!destroyed) {
            ownerPaused = true;
        }
    }

    /** Reopens admission only after a complete pause/resume round trip. */
    public synchronized void onOwnerResumed() {
        if (!destroyed && ownerPaused) {
            starting = false;
        }
        ownerPaused = false;
    }

    /** Permanently closes admission when the Activity owner is destroyed. */
    public synchronized void onOwnerDestroyed() {
        destroyed = true;
        starting = false;
        ownerPaused = false;
    }

    public synchronized boolean isStarting() {
        return starting;
    }

    public synchronized boolean isDestroyed() {
        return destroyed;
    }
}
