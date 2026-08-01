package com.limelight.transfer;

import java.util.Objects;

/**
 * Lifecycle state for one remote clipboard file pull at a time.
 *
 * <p>Generation tokens make late network callbacks harmless after retry or
 * Activity destruction.</p>
 */
public final class ClipboardFileTransferSession {
    private long generation;
    private boolean selectingDirectory;
    private boolean transferInProgress;
    private boolean destroyed;

    public boolean beginDirectorySelection() {
        if (destroyed || selectingDirectory || transferInProgress) {
            return false;
        }
        selectingDirectory = true;
        return true;
    }

    /** Begins selection and rolls it back atomically if the picker cannot launch. */
    public boolean launchDirectorySelection(Runnable launcher) {
        Objects.requireNonNull(launcher, "launcher");
        if (!beginDirectorySelection()) {
            return false;
        }
        try {
            launcher.run();
            return true;
        }
        catch (RuntimeException error) {
            endDirectorySelection();
            throw error;
        }
    }

    public void endDirectorySelection() {
        selectingDirectory = false;
    }

    public long beginTransfer() {
        if (destroyed || transferInProgress) {
            return 0;
        }
        selectingDirectory = false;
        transferInProgress = true;
        return ++generation;
    }

    public boolean finishTransfer(long transferGeneration) {
        if (!isCurrentTransfer(transferGeneration)) {
            return false;
        }
        transferInProgress = false;
        return true;
    }

    public boolean cancelTransfer(long transferGeneration) {
        if (!isCurrentTransfer(transferGeneration)) {
            return false;
        }
        transferInProgress = false;
        generation++;
        return true;
    }

    public boolean isCurrentTransfer(long transferGeneration) {
        return !destroyed &&
                transferInProgress &&
                transferGeneration == generation;
    }

    public boolean isSelectingDirectory() {
        return selectingDirectory;
    }

    public boolean isTransferInProgress() {
        return transferInProgress;
    }

    public void destroy() {
        destroyed = true;
        selectingDirectory = false;
        transferInProgress = false;
        generation++;
    }
}
