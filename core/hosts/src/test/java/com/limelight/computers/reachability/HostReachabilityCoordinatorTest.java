package com.limelight.computers.reachability;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.limelight.computers.model.HostEndpoint;

import org.junit.After;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class HostReachabilityCoordinatorTest {
    @Test
    public void canceledBatchReleasesTransportBeforeNextForegroundProbe() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch transportCanceled = new CountDownLatch(1);
        Future<?> first = callerExecutor.submit(() -> {
            try {
                coordinator.probe(HostReachabilityPlan.create(local, null, null, null),
                        new HostReachabilityCoordinator.Probe<String>() {
                            @Override
                            public String probe(HostEndpoint endpoint) {
                                started.countDown();
                                await(transportCanceled);
                                return null;
                            }

                            @Override
                            public void cancel() {
                                transportCanceled.countDown();
                            }
                        });
            }
            catch (InterruptedException expected) {
                Thread.currentThread().interrupt();
            }
        });
        assertTrue(started.await(2, TimeUnit.SECONDS));
        first.cancel(true);
        assertTrue(transportCanceled.await(2, TimeUnit.SECONDS));
        assertEquals("fresh", coordinator.probe(
                HostReachabilityPlan.create(local, null, null, null), endpoint -> "fresh").getValue());
    }
    private final ExecutorService probeExecutor = Executors.newFixedThreadPool(4);
    private final ExecutorService callerExecutor = Executors.newFixedThreadPool(2);
    private final HostReachabilityCoordinator coordinator =
            new HostReachabilityCoordinator(probeExecutor);
    private final HostEndpoint local = endpoint(HostEndpoint.Kind.LOCAL_IPV4, "local");
    private final HostEndpoint manual = endpoint(HostEndpoint.Kind.MANUAL, "manual");
    private final HostEndpoint remote = endpoint(HostEndpoint.Kind.REMOTE, "remote");
    private final HostEndpoint ipv6 = endpoint(HostEndpoint.Kind.LOCAL_IPV6, "ipv6");

    @After
    public void tearDown() {
        callerExecutor.shutdownNow();
        probeExecutor.shutdownNow();
    }

    @Test
    public void anyAddressCanPublishWhileAllOtherAddressesRemainBlocked() throws Exception {
        HostReachabilityPlan plan = HostReachabilityPlan.create(local, manual, remote, ipv6);
        for (HostEndpoint winner : plan.getEndpoints()) {
            CountDownLatch allStarted = new CountDownLatch(4);
            CountDownLatch allowWinner = new CountDownLatch(1);
            CountDownLatch losersCancelled = new CountDownLatch(3);

            Future<HostReachabilityCoordinator.Result<String>> future =
                    callerExecutor.submit(() -> coordinator.probe(plan, endpoint -> {
                        allStarted.countDown();
                        if (endpoint.equals(winner)) {
                            await(allowWinner);
                            return "validated-response";
                        }
                        try {
                            new CountDownLatch(1).await();
                            throw new AssertionError("Only cancellation may release a losing probe");
                        }
                        catch (InterruptedException expected) {
                            losersCancelled.countDown();
                            Thread.currentThread().interrupt();
                            return null;
                        }
                    }));

            assertTrue("Every address must be probed concurrently",
                    allStarted.await(2, TimeUnit.SECONDS));
            allowWinner.countDown();
            HostReachabilityCoordinator.Result<String> result = future.get(2, TimeUnit.SECONDS);
            assertEquals(winner, result.getEndpoint());
            assertEquals("validated-response", result.getValue());
            assertTrue(losersCancelled.await(2, TimeUnit.SECONDS));
        }
    }

    @Test
    public void failedCandidateDoesNotPublishOfflineWhileAnotherIsPending() throws Exception {
        CountDownLatch localFailed = new CountDownLatch(1);
        CountDownLatch allowRemote = new CountDownLatch(1);
        Future<HostReachabilityCoordinator.Result<String>> future =
                callerExecutor.submit(() -> coordinator.probe(
                        HostReachabilityPlan.create(local, null, remote, null), endpoint -> {
                            if (endpoint.equals(local)) {
                                localFailed.countDown();
                                return null;
                            }
                            await(allowRemote);
                            return "remote-response";
                        }));

        assertTrue(localFailed.await(2, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> future.get(50, TimeUnit.MILLISECONDS));
        allowRemote.countDown();
        assertEquals(remote, future.get(2, TimeUnit.SECONDS).getEndpoint());
    }

    @Test
    public void allFailedCandidatesAreRequiredToPublishUnreachable() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        assertNull(coordinator.probe(
                HostReachabilityPlan.create(local, manual, remote, ipv6), endpoint -> {
                    attempts.incrementAndGet();
                    return null;
                }));
        assertEquals(4, attempts.get());
    }

    @Test
    public void runtimeFailureIsOneFailedCandidateNotAStall() throws Exception {
        HostReachabilityCoordinator.Result<String> result = coordinator.probe(
                HostReachabilityPlan.create(local, null, remote, null), endpoint -> {
                    if (endpoint.equals(local)) {
                        throw new IllegalStateException("bad response");
                    }
                    return "remote-response";
                });

        assertEquals(remote, result.getEndpoint());
        assertEquals("remote-response", result.getValue());
    }

    @Test
    public void slowHostDoesNotHoldBackAnotherHost() throws Exception {
        CountDownLatch slowStarted = new CountDownLatch(1);
        Future<?> slowHost = callerExecutor.submit(() -> coordinator.probe(
                HostReachabilityPlan.create(local, null, null, null), endpoint -> {
                    slowStarted.countDown();
                    await(new CountDownLatch(1));
                    return null;
                }));

        assertTrue(slowStarted.await(2, TimeUnit.SECONDS));
        Future<HostReachabilityCoordinator.Result<String>> fastHost =
                callerExecutor.submit(() -> coordinator.probe(
                        HostReachabilityPlan.create(null, null, remote, null),
                        endpoint -> "other-host-response"));
        assertEquals(remote, fastHost.get(2, TimeUnit.SECONDS).getEndpoint());
        assertFalse(slowHost.isDone());
        slowHost.cancel(true);
    }

    @Test
    public void lateLocalResponseCannotReplacePublishedRemoteResponse() throws Exception {
        CountDownLatch localStarted = new CountDownLatch(1);
        CountDownLatch allowLateLocal = new CountDownLatch(1);
        CountDownLatch localFinished = new CountDownLatch(1);
        Future<HostReachabilityCoordinator.Result<String>> future =
                callerExecutor.submit(() -> coordinator.probe(
                        HostReachabilityPlan.create(local, null, remote, null), endpoint -> {
                            if (endpoint.equals(local)) {
                                localStarted.countDown();
                                // Model a transport that finishes despite cancellation.
                                boolean interrupted = false;
                                while (true) {
                                    try {
                                        allowLateLocal.await();
                                        break;
                                    }
                                    catch (InterruptedException expected) {
                                        interrupted = true;
                                    }
                                }
                                if (interrupted) {
                                    Thread.currentThread().interrupt();
                                }
                                localFinished.countDown();
                                return "late-local-response";
                            }
                            await(localStarted);
                            return "remote-response";
                        }));

        try {
            HostReachabilityCoordinator.Result<String> result = future.get(2, TimeUnit.SECONDS);
            allowLateLocal.countDown();
            assertTrue(localFinished.await(2, TimeUnit.SECONDS));
            assertEquals(remote, result.getEndpoint());
            assertEquals("remote-response", result.getValue());
        }
        finally {
            allowLateLocal.countDown();
        }
    }

    @Test
    public void callerInterruptionCancelsEveryOutstandingProbe() throws Exception {
        CountDownLatch allStarted = new CountDownLatch(4);
        CountDownLatch allCancelled = new CountDownLatch(4);
        CountDownLatch callerFinished = new CountDownLatch(1);
        AtomicBoolean callerInterrupted = new AtomicBoolean();

        Future<?> future = callerExecutor.submit(() -> {
            try {
                coordinator.probe(HostReachabilityPlan.create(local, manual, remote, ipv6),
                        endpoint -> {
                            allStarted.countDown();
                            try {
                                new CountDownLatch(1).await();
                            }
                            catch (InterruptedException expected) {
                                allCancelled.countDown();
                                Thread.currentThread().interrupt();
                            }
                            return null;
                        });
            }
            catch (InterruptedException expected) {
                callerInterrupted.set(true);
            }
            finally {
                callerFinished.countDown();
            }
        });

        assertTrue(allStarted.await(2, TimeUnit.SECONDS));
        future.cancel(true);
        assertTrue(callerFinished.await(2, TimeUnit.SECONDS));
        assertTrue(callerInterrupted.get());
        assertTrue(allCancelled.await(2, TimeUnit.SECONDS));
    }

    @Test
    public void alreadyInterruptedCallerStartsNoNetworkWork() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        Future<?> future = callerExecutor.submit(() -> {
            Thread.currentThread().interrupt();
            return coordinator.probe(HostReachabilityPlan.create(local, null, null, null),
                    endpoint -> {
                        attempts.incrementAndGet();
                        return "unexpected";
                    });
        });

        ExecutionException failure = assertThrows(ExecutionException.class,
                () -> future.get(2, TimeUnit.SECONDS));
        assertTrue(failure.getCause() instanceof InterruptedException);
        assertEquals(0, attempts.get());
    }

    @Test
    public void duplicateAddressIsProbedOnce() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        HostEndpoint duplicate = endpoint(HostEndpoint.Kind.MANUAL, local.getAddress());
        HostReachabilityCoordinator.Result<String> result = coordinator.probe(
                HostReachabilityPlan.create(local, duplicate, null, null), endpoint -> {
                    attempts.incrementAndGet();
                    return "response";
                });
        assertEquals(1, attempts.get());
        assertEquals(local, result.getEndpoint());
    }

    @Test
    public void emptyPlanIsImmediatelyUnreachable() throws Exception {
        assertNull(coordinator.probe(HostReachabilityPlan.create(null, null, null, null),
                endpoint -> {
                    throw new AssertionError("An empty plan must not start a probe");
                }));
    }

    private static HostEndpoint endpoint(HostEndpoint.Kind kind, String address) {
        return new HostEndpoint(kind, address, 47989);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        }
        catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
    }
}
