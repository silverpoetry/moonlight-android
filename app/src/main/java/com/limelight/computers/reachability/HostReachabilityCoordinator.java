package com.limelight.computers.reachability;

import com.limelight.computers.model.HostEndpoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Runs one bounded, cancelable race across an immutable endpoint plan. */
public final class HostReachabilityCoordinator {
    public interface Probe<T> {
        T probe(HostEndpoint endpoint);
    }

    public interface Clock {
        long elapsedRealtimeMillis();
    }

    public static final class Result<T> {
        private final HostEndpoint endpoint;
        private final T value;

        private Result(HostEndpoint endpoint, T value) {
            this.endpoint = endpoint;
            this.value = value;
        }

        public HostEndpoint getEndpoint() {
            return endpoint;
        }

        public T getValue() {
            return value;
        }
    }

    private final ExecutorService executor;
    private final Clock clock;
    private final long upgradeGraceMillis;

    public HostReachabilityCoordinator(
            ExecutorService executor,
            Clock clock,
            long upgradeGraceMillis) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.clock = Objects.requireNonNull(clock, "clock");
        if (upgradeGraceMillis < 0) {
            throw new IllegalArgumentException(
                    "Upgrade grace cannot be negative");
        }
        this.upgradeGraceMillis = upgradeGraceMillis;
    }

    public <T> Result<T> probe(
            HostReachabilityPlan plan,
            Probe<T> probe) throws InterruptedException {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(probe, "probe");
        List<HostEndpoint> endpoints = plan.getEndpoints();
        if (endpoints.isEmpty()) {
            return null;
        }

        CompletionService<Completion<T>> completions =
                new ExecutorCompletionService<>(executor);
        ArrayList<Future<Completion<T>>> futures =
                new ArrayList<>(endpoints.size());
        try {
            for (int index = 0; index < endpoints.size(); index++) {
                int candidateIndex = index;
                HostEndpoint endpoint = endpoints.get(index);
                futures.add(completions.submit(() ->
                        runProbe(candidateIndex, endpoint, probe)));
            }

            HostReachabilitySelection<T> selection =
                    new HostReachabilitySelection<>(
                            endpoints.size(),
                            upgradeGraceMillis);
            while (true) {
                HostReachabilitySelection.Decision<T> decision =
                        selection.decide(
                                clock.elapsedRealtimeMillis());
                switch (decision.getStatus()) {
                    case REACHABLE:
                        return new Result<>(
                                endpoints.get(
                                        decision.getWinnerIndex()),
                                decision.getValue());
                    case UNREACHABLE:
                        return null;
                    case WAITING:
                        break;
                    default:
                        throw new AssertionError(
                                "Unhandled reachability decision");
                }

                Future<Completion<T>> completedFuture =
                        awaitCompletion(
                                completions,
                                decision.getWaitMillis());
                if (completedFuture == null) {
                    continue;
                }
                Completion<T> completion = getCompletion(
                        completedFuture);
                selection.recordCompletion(
                        completion.index,
                        completion.value,
                        clock.elapsedRealtimeMillis());
            }
        }
        finally {
            for (Future<?> future : futures) {
                future.cancel(true);
            }
        }
    }

    private static <T> Completion<T> runProbe(
            int index,
            HostEndpoint endpoint,
            Probe<T> probe) {
        try {
            return new Completion<>(index, probe.probe(endpoint));
        }
        catch (RuntimeException error) {
            return new Completion<>(index, null);
        }
    }

    private static <T> Future<Completion<T>> awaitCompletion(
            CompletionService<Completion<T>> completions,
            long waitMillis) throws InterruptedException {
        if (waitMillis ==
                HostReachabilitySelection.WAIT_INDEFINITELY) {
            return completions.take();
        }
        return completions.poll(
                Math.max(1L, waitMillis),
                TimeUnit.MILLISECONDS);
    }

    private static <T> Completion<T> getCompletion(
            Future<Completion<T>> future)
            throws InterruptedException {
        try {
            return future.get();
        }
        catch (CancellationException | ExecutionException error) {
            throw new IllegalStateException(
                    "Endpoint probe did not publish a completion",
                    error);
        }
    }

    private static final class Completion<T> {
        private final int index;
        private final T value;

        Completion(int index, T value) {
            this.index = index;
            this.value = value;
        }
    }
}
