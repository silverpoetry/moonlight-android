package com.limelight.utils.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public final class LatestTaskExecutorTest {
    @Test
    public void newestPendingTaskReplacesObsoleteWork() throws Exception {
        LatestTaskExecutor executor =
                new LatestTaskExecutor("latest-task-test");
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch latestFinished = new CountDownLatch(1);
        CountDownLatch obsoleteDiscarded = new CountDownLatch(1);
        List<Integer> completed = Collections.synchronizedList(
                new ArrayList<>());

        try {
            executor.execute(() -> {
                firstStarted.countDown();
                await(releaseFirst);
                completed.add(1);
            });
            assertTrue(firstStarted.await(1, TimeUnit.SECONDS));

            executor.execute(
                    () -> completed.add(2),
                    obsoleteDiscarded::countDown);
            executor.execute(() -> {
                completed.add(3);
                latestFinished.countDown();
            });
            releaseFirst.countDown();

            assertTrue(obsoleteDiscarded.await(1, TimeUnit.SECONDS));
            assertTrue(latestFinished.await(1, TimeUnit.SECONDS));
            assertEquals(Arrays.asList(1, 3), completed);
        }
        finally {
            releaseFirst.countDown();
            executor.close();
        }
    }

    @Test
    public void closedExecutorRejectsNewWork() {
        LatestTaskExecutor executor =
                new LatestTaskExecutor("closed-task-test");
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
