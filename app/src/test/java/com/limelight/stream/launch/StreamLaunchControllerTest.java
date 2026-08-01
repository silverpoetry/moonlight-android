package com.limelight.stream.launch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public final class StreamLaunchControllerTest {
    @Test
    public void rejectsDuplicateUntilOwnerResumes() {
        StreamLaunchController controller =
                new StreamLaunchController();
        AtomicInteger launches = new AtomicInteger();

        assertEquals(
                StreamLaunchController.Status.STARTED,
                controller.launch(launches::incrementAndGet));
        assertEquals(
                StreamLaunchController.Status.ALREADY_STARTING,
                controller.launch(launches::incrementAndGet));
        assertEquals(1, launches.get());
        assertTrue(controller.isStarting());

        controller.onOwnerPaused();
        controller.onOwnerResumed();

        assertFalse(controller.isStarting());
        assertEquals(
                StreamLaunchController.Status.STARTED,
                controller.launch(launches::incrementAndGet));
        assertEquals(2, launches.get());
    }

    @Test
    public void failedLaunchRollsBackAdmission() {
        StreamLaunchController controller =
                new StreamLaunchController();
        RuntimeException failure = new RuntimeException("launch");
        try {
            controller.launch(() -> {
                throw failure;
            });
            fail("Expected launch failure");
        }
        catch (RuntimeException error) {
            assertEquals(failure, error);
        }

        assertFalse(controller.isStarting());
        assertEquals(
                StreamLaunchController.Status.STARTED,
                controller.launch(() -> { }));
    }

    @Test
    public void destructionPermanentlyRejectsLaunches() {
        StreamLaunchController controller =
                new StreamLaunchController();
        controller.onOwnerDestroyed();
        controller.onOwnerResumed();

        assertEquals(
                StreamLaunchController.Status.OWNER_DESTROYED,
                controller.launch(() -> fail("must not launch")));
        assertTrue(controller.isDestroyed());
        assertFalse(controller.isStarting());
    }

    @Test
    public void initialResumeCannotReopenAnAdmittedLaunch() {
        StreamLaunchController controller =
                new StreamLaunchController();
        controller.launch(() -> { });

        controller.onOwnerResumed();

        assertEquals(
                StreamLaunchController.Status.ALREADY_STARTING,
                controller.launch(() -> fail("must not launch")));
    }
}
