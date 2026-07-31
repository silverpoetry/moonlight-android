package com.limelight.binding.video;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public final class CodecRecoveryCoordinatorTest {
    @Test
    public void strongerRequestsPromoteAndWeakerRequestsDoNotDemote() {
        CodecRecoveryCoordinator coordinator =
                new CodecRecoveryCoordinator();

        assertTrue(coordinator.request(
                CodecRecoveryCoordinator.RecoveryType.FLUSH));
        assertTrue(coordinator.request(
                CodecRecoveryCoordinator.RecoveryType.RESTART));
        assertFalse(coordinator.request(
                CodecRecoveryCoordinator.RecoveryType.FLUSH));
        assertTrue(coordinator.request(
                CodecRecoveryCoordinator.RecoveryType.RESET));
        assertFalse(coordinator.request(
                CodecRecoveryCoordinator.RecoveryType.RESTART));
        assertEquals(
                CodecRecoveryCoordinator.RecoveryType.RESET,
                coordinator.getRecoveryType());
    }

    @Test
    public void completionClearsPendingRecovery() {
        CodecRecoveryCoordinator coordinator =
                new CodecRecoveryCoordinator();
        coordinator.request(
                CodecRecoveryCoordinator.RecoveryType.RESTART);

        coordinator.completeRecovery();

        assertFalse(coordinator.hasPendingRecovery());
        assertEquals(
                CodecRecoveryCoordinator.RecoveryType.NONE,
                coordinator.getRecoveryType());
    }

    @Test
    public void noneCannotBeRequestedAsRecoveryWork() {
        CodecRecoveryCoordinator coordinator =
                new CodecRecoveryCoordinator();

        assertThrows(
                IllegalArgumentException.class,
                () -> coordinator.request(
                        CodecRecoveryCoordinator
                                .RecoveryType.NONE));
    }

    @Test
    public void absentChoreographerIsQuiescedImplicitly() {
        CodecRecoveryCoordinator coordinator =
                new CodecRecoveryCoordinator();

        coordinator.markThreadQuiesced(
                CodecRecoveryCoordinator.INPUT_THREAD,
                false);
        assertFalse(coordinator.areAllThreadsQuiesced());
        coordinator.markThreadQuiesced(
                CodecRecoveryCoordinator.RENDER_THREAD,
                false);

        assertTrue(coordinator.areAllThreadsQuiesced());
        coordinator.clearQuiescedThreads();
        assertEquals(0, coordinator.getQuiescedThreads());
    }

    @Test
    public void activeChoreographerMustQuiesceExplicitly() {
        CodecRecoveryCoordinator coordinator =
                new CodecRecoveryCoordinator();
        coordinator.markThreadQuiesced(
                CodecRecoveryCoordinator.INPUT_THREAD,
                true);
        coordinator.markThreadQuiesced(
                CodecRecoveryCoordinator.RENDER_THREAD,
                true);

        assertFalse(coordinator.areAllThreadsQuiesced());
        coordinator.markThreadQuiesced(
                CodecRecoveryCoordinator.CHOREOGRAPHER_THREAD,
                true);
        assertTrue(coordinator.areAllThreadsQuiesced());
    }

    @Test
    public void attemptBudgetCanBeResetForExpectedRestart() {
        CodecRecoveryCoordinator coordinator =
                new CodecRecoveryCoordinator();

        assertEquals(1, coordinator.beginRecoveryAttempt());
        assertFalse(coordinator.hasAttemptsRemaining(1));
        coordinator.resetAttempts();
        assertTrue(coordinator.hasAttemptsRemaining(1));
        assertEquals(1, coordinator.beginRecoveryAttempt());
    }
}
