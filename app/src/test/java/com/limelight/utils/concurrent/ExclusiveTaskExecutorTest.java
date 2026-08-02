package com.limelight.utils.concurrent;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public final class ExclusiveTaskExecutorTest {
    @Test
    public void rejectsWorkWhileOperationIsRunning() throws Exception {
        ExclusiveTaskExecutor executor =
                new ExclusiveTaskExecutor("exclusive-task-test");
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);

        try {
            executor.execute(() -> {
                started.countDown();
                await(release);
                finished.countDown();
            });
            assertTrue(started.await(1, TimeUnit.SECONDS));

            assertThrows(
                    RejectedExecutionException.class,
                    () -> executor.execute(() -> { }));

            release.countDown();
            assertTrue(finished.await(1, TimeUnit.SECONDS));
        }
        finally {
            release.countDown();
            executor.close();
        }
    }

    @Test
    public void closedExecutorRejectsWork() {
        ExclusiveTaskExecutor executor =
                new ExclusiveTaskExecutor("closed-exclusive-task-test");
        executor.close();

        assertThrows(
                RejectedExecutionException.class,
                () -> executor.execute(() -> { }));
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        }
        catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
