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

/**
 * Races all distinct endpoints and returns the first validated response.
 * A failed probe cannot declare the host offline while other probes are pending.
 */
public final class HostReachabilityCoordinator {
    public interface Probe<T> {
        /** Returns a validated host response, or null if this endpoint failed. */
        T probe(HostEndpoint endpoint);

        /** Cancels the transport requests still owned by this probe batch. */
        default void cancel() { }
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

    public HostReachabilityCoordinator(ExecutorService executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public <T> Result<T> probe(
            HostReachabilityPlan plan,
            Probe<T> probe) throws InterruptedException {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(probe, "probe");
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        List<HostEndpoint> endpoints = plan.getEndpoints();
        if (endpoints.isEmpty()) {
            return null;
        }

        CompletionService<Result<T>> completions =
                new ExecutorCompletionService<>(executor);
        ArrayList<Future<Result<T>>> futures =
                new ArrayList<>(endpoints.size());
        try {
            for (HostEndpoint endpoint : endpoints) {
                futures.add(completions.submit(() ->
                        runProbe(endpoint, probe)));
            }

            for (int remaining = endpoints.size(); remaining > 0; remaining--) {
                Result<T> result = getCompletion(completions.take());
                if (result != null) {
                    return result;
                }
            }
            return null;
        }
        finally {
            probe.cancel();
            for (Future<?> future : futures) {
                future.cancel(true);
            }
        }
    }

    private static <T> Result<T> runProbe(
            HostEndpoint endpoint,
            Probe<T> probe) {
        try {
            T value = probe.probe(endpoint);
            return value == null ? null : new Result<>(endpoint, value);
        }
        catch (RuntimeException error) {
            return null;
        }
    }

    private static <T> Result<T> getCompletion(
            Future<Result<T>> future)
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
}
