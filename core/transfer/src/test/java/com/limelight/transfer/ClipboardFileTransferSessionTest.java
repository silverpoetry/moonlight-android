package com.limelight.transfer;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public final class ClipboardFileTransferSessionTest {
    @Test
    public void directorySelectionAndTransferAreMutuallyExclusive() {
        ClipboardFileTransferSession session =
                new ClipboardFileTransferSession();

        assertTrue(session.beginDirectorySelection());
        assertTrue(session.isSelectingDirectory());
        assertFalse(session.beginDirectorySelection());

        long generation = session.beginTransfer();
        assertNotEquals(0, generation);
        assertFalse(session.isSelectingDirectory());
        assertTrue(session.isCurrentTransfer(generation));
        assertFalse(session.beginDirectorySelection());

        assertTrue(session.finishTransfer(generation));
        assertFalse(session.isTransferInProgress());
    }

    @Test
    public void staleCallbacksCannotFinishRetriedTransfer() {
        ClipboardFileTransferSession session =
                new ClipboardFileTransferSession();

        long firstGeneration = session.beginTransfer();
        assertTrue(session.finishTransfer(firstGeneration));

        long secondGeneration = session.beginTransfer();
        assertNotEquals(firstGeneration, secondGeneration);
        assertFalse(session.finishTransfer(firstGeneration));
        assertTrue(session.isCurrentTransfer(secondGeneration));
    }

    @Test
    public void destroyInvalidatesSelectionAndTransferCallbacks() {
        ClipboardFileTransferSession session =
                new ClipboardFileTransferSession();

        assertTrue(session.beginDirectorySelection());
        long generation = session.beginTransfer();
        session.destroy();

        assertFalse(session.isSelectingDirectory());
        assertFalse(session.isTransferInProgress());
        assertFalse(session.isCurrentTransfer(generation));
        assertFalse(session.beginDirectorySelection());
        assertNotEquals(0, generation);
    }

    @Test
    public void cancellationInvalidatesLateCallbacksAndAllowsRetry() {
        ClipboardFileTransferSession session =
                new ClipboardFileTransferSession();

        long first = session.beginTransfer();
        assertNotEquals(0, first);
        assertTrue(session.cancelTransfer(first));
        assertFalse(session.isCurrentTransfer(first));
        assertFalse(session.finishTransfer(first));

        long retry = session.beginTransfer();
        assertNotEquals(0, retry);
        assertNotEquals(first, retry);
        assertTrue(session.finishTransfer(retry));
    }
}
