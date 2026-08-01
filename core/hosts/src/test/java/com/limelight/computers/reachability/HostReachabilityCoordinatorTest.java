package com.limelight.computers.reachability;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.limelight.computers.model.HostEndpoint;

import org.junit.After;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class HostReachabilityCoordinatorTest {
    private final ExecutorService probeExecutor =
            Executors.newFixedThreadPool(4);
    private final ExecutorService callerExecutor =
            Executors.newSingleThreadExecutor();

    @After
    public void tearDown() {
        callerExecutor.shutdownNow();
        probeExecutor.shutdownNow();
    }

    @Test
    public void higherPriorityResultCanUpgradeFirstSuccess()
            throws Exception {
        HostEndpoint local = endpoint(
                HostEndpoint.Kind.LOCAL_IPV4,
                "local");
        HostEndpoint remote = endpoint(
                HostEndpoint.Kind.REMOTE,
                "remote");
        HostReachabilityPlan plan = HostReachabilityPlan.create(
                local,
                null,
                remote,
                null,
                false);
        CountDownLatch localMayComplete = new CountDownLatch(1);
        CountDownLatch remoteCompleted = new CountDownLatch(1);
        HostReachabilityCoordinator coordinator = coordinator(1000);

        Future<HostReachabilityCoordinator.Result<String>> future =
                callerExecutor.submit(() -> coordinator.probe(
                        plan,
                        endpoint -> {
                            if (endpoint.equals(local)) {
                                await(localMayComplete);
                                return "local-result";
                            }
                            remoteCompleted.countDown();
                            return "remote-result";
                        }));

        assertTrue(remoteCompleted.await(2, TimeUnit.SECONDS));
        localMayComplete.countDown();
        HostReachabilityCoordinator.Result<String> result =
                future.get(2, TimeUnit.SECONDS);
        assertEquals(local, result.getEndpoint());
        assertEquals("local-result", result.getValue());
    }

    @Test
    public void lowerPriorityResultPublishesAfterGrace()
            throws Exception {
        HostEndpoint local = endpoint(
                HostEndpoint.Kind.LOCAL_IPV4,
                "local");
        HostEndpoint remote = endpoint(
                HostEndpoint.Kind.REMOTE,
                "remote");
        HostReachabilityPlan plan = HostReachabilityPlan.create(
                local,
                null,
                remote,
                null,
                false);
        CountDownLatch localBlocked = new CountDownLatch(1);
        AtomicBoolean localInterrupted = new AtomicBoolean();

        HostReachabilityCoordinator.Result<String> result =
                coordinator(20).probe(plan, endpoint -> {
                    if (endpoint.equals(local)) {
                        try {
                            localBlocked.await();
                        }
                        catch (InterruptedException error) {
                            localInterrupted.set(true);
                            Thread.currentThread().interrupt();
                        }
                        return null;
                    }
                    return "remote-result";
                });

        assertEquals(remote, result.getEndpoint());
        assertEquals("remote-result", result.getValue());
        for (int attempt = 0;
                attempt < 20 && !localInterrupted.get();
                attempt++) {
            Thread.sleep(10);
        }
        assertTrue(localInterrupted.get());
    }

    @Test
    public void runtimeFailureIsOneFailedCandidateNotAStall()
            throws Exception {
        HostEndpoint local = endpoint(
                HostEndpoint.Kind.LOCAL_IPV4,
                "local");
        HostEndpoint remote = endpoint(
                HostEndpoint.Kind.REMOTE,
                "remote");
        HostReachabilityPlan plan = HostReachabilityPlan.create(
                local,
                null,
                remote,
                null,
                false);

        HostReachabilityCoordinator.Result<String> result =
                coordinator(200).probe(plan, endpoint -> {
                    if (endpoint.equals(local)) {
                        throw new IllegalStateException("bad response");
                    }
                    return "remote-result";
                });

        assertEquals(remote, result.getEndpoint());
    }

    @Test
    public void callerInterruptionCancelsOutstandingProbes()
            throws Exception {
        HostEndpoint local = endpoint(
                HostEndpoint.Kind.LOCAL_IPV4,
                "local");
        HostReachabilityPlan plan = HostReachabilityPlan.create(
                local,
                null,
                null,
                null,
                false);
        CountDownLatch probeStarted = new CountDownLatch(1);
        AtomicBoolean probeInterrupted = new AtomicBoolean();
        AtomicBoolean callerInterrupted = new AtomicBoolean();

        Thread caller = new Thread(() -> {
            try {
                coordinator(200).probe(plan, endpoint -> {
                    probeStarted.countDown();
                    try {
                        new CountDownLatch(1).await();
                    }
                    catch (InterruptedException error) {
                        probeInterrupted.set(true);
                        Thread.currentThread().interrupt();
                    }
                    return null;
                });
            }
            catch (InterruptedException expected) {
                callerInterrupted.set(true);
            }
        });
        caller.start();
        assertTrue(probeStarted.await(2, TimeUnit.SECONDS));
        caller.interrupt();
        caller.join(2000);

        assertFalse(caller.isAlive());
        assertTrue(callerInterrupted.get());
        for (int attempt = 0;
                attempt < 20 && !probeInterrupted.get();
                attempt++) {
            Thread.sleep(10);
        }
        assertTrue(probeInterrupted.get());
    }

    @Test
    public void emptyPlanIsImmediatelyUnreachable()
            throws Exception {
        HostReachabilityPlan plan = HostReachabilityPlan.create(
                null,
                null,
                null,
                null,
                false);

        assertNull(coordinator(200).probe(
                plan,
                endpoint -> "unexpected"));
    }

    private HostReachabilityCoordinator coordinator(
            long graceMillis) {
        return new HostReachabilityCoordinator(
                probeExecutor,
                System::currentTimeMillis,
                graceMillis);
    }

    private static HostEndpoint endpoint(
            HostEndpoint.Kind kind,
            String address) {
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
