package com.limelight.stream.launch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public final class StreamLaunchUseCaseTest {
    @Test
    public void successfulGatewayLaunchIsPersistedAfterGatewayReturns() {
        FakeRecentSessions recent = new FakeRecentSessions();
        StreamLaunchUseCase useCase = new StreamLaunchUseCase(recent);

        StreamLaunchUseCase.Result result = useCase.launch(
                request(),
                ignored -> assertNull(recent.saved));

        assertEquals(
                StreamLaunchUseCase.Outcome.STARTED,
                result.getOutcome());
        assertEquals(
                new RecentStreamSession("Desktop", 7, true),
                recent.saved);
    }

    @Test
    public void failedGatewayDoesNotPersistAndAdmissionCanRetry() {
        FakeRecentSessions recent = new FakeRecentSessions();
        StreamLaunchUseCase useCase = new StreamLaunchUseCase(recent);
        RuntimeException failure = new RuntimeException("launch");

        StreamLaunchUseCase.Result failed = useCase.launch(
                request(),
                ignored -> {
                    throw failure;
                });
        StreamLaunchUseCase.Result retried = useCase.launch(
                request(),
                ignored -> { });

        assertEquals(
                StreamLaunchUseCase.Outcome.LAUNCH_FAILED,
                failed.getOutcome());
        assertSame(failure, failed.getLaunchFailure());
        assertEquals(
                StreamLaunchUseCase.Outcome.STARTED,
                retried.getOutcome());
    }

    @Test
    public void duplicateLaunchDoesNotReachGatewayOrPersistence() {
        FakeRecentSessions recent = new FakeRecentSessions();
        StreamLaunchUseCase useCase = new StreamLaunchUseCase(recent);
        AtomicInteger gatewayCalls = new AtomicInteger();
        useCase.launch(request(), ignored -> gatewayCalls.incrementAndGet());
        recent.saveCalls = 0;

        StreamLaunchUseCase.Result duplicate = useCase.launch(
                request(),
                ignored -> gatewayCalls.incrementAndGet());

        assertEquals(
                StreamLaunchUseCase.Outcome.ALREADY_STARTING,
                duplicate.getOutcome());
        assertEquals(1, gatewayCalls.get());
        assertEquals(0, recent.saveCalls);
    }

    @Test
    public void persistenceFailureCannotMisreportAcceptedLaunch() {
        RuntimeException failure = new RuntimeException("persist");
        FakeRecentSessions recent = new FakeRecentSessions();
        recent.saveFailure = failure;
        StreamLaunchUseCase useCase = new StreamLaunchUseCase(recent);

        StreamLaunchUseCase.Result result = useCase.launch(
                request(),
                ignored -> { });

        assertEquals(
                StreamLaunchUseCase.Outcome.STARTED,
                result.getOutcome());
        assertSame(failure, result.getPersistenceFailure());
        assertEquals(
                StreamLaunchUseCase.Outcome.ALREADY_STARTING,
                useCase.launch(request(), ignored -> { }).getOutcome());
    }

    @Test
    public void launchWithoutStableHostDoesNotWriteRecentSession() {
        FakeRecentSessions recent = new FakeRecentSessions();
        StreamLaunchUseCase useCase = new StreamLaunchUseCase(recent);
        StreamLaunchRequest request = new StreamLaunchRequest(
                "host",
                47989,
                0,
                "Desktop",
                7,
                false,
                "client",
                null,
                null,
                null);

        StreamLaunchUseCase.Result result = useCase.launch(
                request,
                ignored -> { });

        assertEquals(
                StreamLaunchUseCase.Outcome.STARTED,
                result.getOutcome());
        assertEquals(0, recent.saveCalls);
    }

    private static StreamLaunchRequest request() {
        return new StreamLaunchRequest(
                "host",
                47989,
                47984,
                "Desktop",
                7,
                true,
                "client",
                "host-id",
                "host-name",
                null);
    }

    private static final class FakeRecentSessions
            implements RecentStreamSessionRepository {
        private RecentStreamSession saved;
        private int saveCalls;
        private RuntimeException saveFailure;

        @Override
        public RecentStreamSession find(String hostId) {
            return saved;
        }

        @Override
        public void save(
                String hostId,
                RecentStreamSession session) {
            saveCalls++;
            if (saveFailure != null) {
                throw saveFailure;
            }
            saved = session;
        }
    }
}
