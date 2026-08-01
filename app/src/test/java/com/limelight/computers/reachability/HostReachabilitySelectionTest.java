package com.limelight.computers.reachability;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public final class HostReachabilitySelectionTest {
    @Test
    public void waitsIndefinitelyBeforeAnyEndpointSucceeds() {
        HostReachabilitySelection<String> selection =
                new HostReachabilitySelection<>(4, 200);

        assertDecision(
                selection.decide(1000),
                HostReachabilitySelection.Status.WAITING,
                HostReachabilitySelection.WAIT_INDEFINITELY);
    }

    @Test
    public void lowerPrioritySuccessStartsOneUpgradeWindow() {
        HostReachabilitySelection<String> selection =
                new HostReachabilitySelection<>(4, 200);
        selection.recordCompletion(2, "remote", 1000);

        assertDecision(
                selection.decide(1100),
                HostReachabilitySelection.Status.WAITING,
                100);

        selection.recordCompletion(1, "manual", 1150);
        assertDecision(
                selection.decide(1190),
                HostReachabilitySelection.Status.WAITING,
                10);

        HostReachabilitySelection.Decision<String> decision =
                selection.decide(1200);
        assertEquals(
                HostReachabilitySelection.Status.REACHABLE,
                decision.getStatus());
        assertEquals(1, decision.getWinnerIndex());
        assertEquals("manual", decision.getValue());
    }

    @Test
    public void publishesBeforeDeadlineWhenAllHigherPrioritiesFinish() {
        HostReachabilitySelection<String> selection =
                new HostReachabilitySelection<>(3, 200);
        selection.recordCompletion(2, "remote", 1000);
        selection.recordCompletion(0, null, 1010);
        selection.recordCompletion(1, null, 1020);

        HostReachabilitySelection.Decision<String> decision =
                selection.decide(1020);
        assertEquals(
                HostReachabilitySelection.Status.REACHABLE,
                decision.getStatus());
        assertEquals(2, decision.getWinnerIndex());
        assertEquals("remote", decision.getValue());
    }

    @Test
    public void highestPrioritySuccessPublishesImmediately() {
        HostReachabilitySelection<Object> selection =
                new HostReachabilitySelection<>(4, 200);
        Object local = new Object();
        selection.recordCompletion(0, local, 1000);

        HostReachabilitySelection.Decision<Object> decision =
                selection.decide(1000);
        assertEquals(
                HostReachabilitySelection.Status.REACHABLE,
                decision.getStatus());
        assertEquals(0, decision.getWinnerIndex());
        assertSame(local, decision.getValue());
    }

    @Test
    public void allFailuresPublishUnreachable() {
        HostReachabilitySelection<String> selection =
                new HostReachabilitySelection<>(2, 200);
        selection.recordCompletion(0, null, 1000);
        selection.recordCompletion(1, null, 1001);

        assertEquals(
                HostReachabilitySelection.Status.UNREACHABLE,
                selection.decide(1001).getStatus());
    }

    @Test
    public void duplicateCompletionIsRejected() {
        HostReachabilitySelection<String> selection =
                new HostReachabilitySelection<>(1, 200);
        selection.recordCompletion(0, null, 1000);

        assertThrows(
                IllegalStateException.class,
                () -> selection.recordCompletion(
                        0,
                        "late",
                        1001));
    }

    private static void assertDecision(
            HostReachabilitySelection.Decision<?> decision,
            HostReachabilitySelection.Status status,
            long waitMillis) {
        assertEquals(status, decision.getStatus());
        assertEquals(waitMillis, decision.getWaitMillis());
    }
}
