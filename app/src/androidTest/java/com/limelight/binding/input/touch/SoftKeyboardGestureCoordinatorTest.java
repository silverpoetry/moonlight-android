package com.limelight.binding.input.touch;

import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class SoftKeyboardGestureCoordinatorTest {
    private final List<Integer> dispatchedActions = new ArrayList<>();
    private final AtomicInteger deferredPrefixCount = new AtomicInteger();
    private final AtomicInteger recognizedGestureCount = new AtomicInteger();

    private SoftKeyboardGestureCoordinator coordinator;
    private View eventView;
    private CountDownLatch dispatchedEvent;

    @Before
    public void setUp() {
        dispatchedEvent = new CountDownLatch(1);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            eventView = new View(InstrumentationRegistry.getTargetContext());
            coordinator = new SoftKeyboardGestureCoordinator(
                    10,
                    new SoftKeyboardGestureCoordinator.Listener() {
                        @Override
                        public void dispatchDeferredTouchEvent(
                                View view,
                                MotionEvent event) {
                            dispatchedActions.add(event.getActionMasked());
                            dispatchedEvent.countDown();
                        }

                        @Override
                        public void onGesturePrefixDeferred() {
                            deferredPrefixCount.incrementAndGet();
                        }

                        @Override
                        public void onKeyboardGestureRecognized() {
                            recognizedGestureCount.incrementAndGet();
                        }
                    });
        });
    }

    @After
    public void tearDown() {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(coordinator::cancel);
    }

    @Test
    public void disabledGestureAddsNoInputDelay() {
        long downTime = SystemClock.uptimeMillis();
        MotionEvent down = event(downTime, 0, MotionEvent.ACTION_DOWN, 1, 0);
        MotionEvent secondDown =
                event(downTime, 10, MotionEvent.ACTION_POINTER_DOWN, 2, 1);
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                assertFalse(coordinator.onTouchEvent(eventView, down, 0));
                assertFalse(coordinator.onTouchEvent(eventView, secondDown, 0));
            });
        }
        finally {
            down.recycle();
            secondDown.recycle();
        }

        assertEquals(0, deferredPrefixCount.get());
        assertTrue(dispatchedActions.isEmpty());
    }

    @Test
    public void exactThreeFingerTapNeverDispatchesTwoFingerPrefix() {
        long downTime = SystemClock.uptimeMillis();
        MotionEvent down = event(downTime, 0, MotionEvent.ACTION_DOWN, 1, 0);
        MotionEvent secondDown =
                event(downTime, 10, MotionEvent.ACTION_POINTER_DOWN, 2, 1);
        MotionEvent thirdDown =
                event(downTime, 20, MotionEvent.ACTION_POINTER_DOWN, 3, 2);
        MotionEvent thirdUp =
                event(downTime, 80, MotionEvent.ACTION_POINTER_UP, 3, 2);
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                assertFalse(coordinator.onTouchEvent(eventView, down, 3));
                assertTrue(coordinator.onTouchEvent(eventView, secondDown, 3));
                assertTrue(coordinator.onTouchEvent(eventView, thirdDown, 3));
                assertTrue(coordinator.onTouchEvent(eventView, thirdUp, 3));
            });
        }
        finally {
            down.recycle();
            secondDown.recycle();
            thirdDown.recycle();
            thirdUp.recycle();
        }

        assertEquals(1, deferredPrefixCount.get());
        assertEquals(1, recognizedGestureCount.get());
        assertTrue(dispatchedActions.isEmpty());
    }

    @Test
    public void deadlineReleasesStationaryTwoFingerPrefix() throws Exception {
        long downTime = SystemClock.uptimeMillis();
        MotionEvent down = event(downTime, 0, MotionEvent.ACTION_DOWN, 1, 0);
        MotionEvent secondDown =
                event(downTime, 10, MotionEvent.ACTION_POINTER_DOWN, 2, 1);
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                assertFalse(coordinator.onTouchEvent(eventView, down, 3));
                assertTrue(coordinator.onTouchEvent(eventView, secondDown, 3));
            });
        }
        finally {
            down.recycle();
            secondDown.recycle();
        }

        assertTrue(dispatchedEvent.await(1, TimeUnit.SECONDS));
        assertEquals(1, deferredPrefixCount.get());
        assertEquals(0, recognizedGestureCount.get());
        assertEquals(1, dispatchedActions.size());
        assertEquals(
                MotionEvent.ACTION_POINTER_DOWN,
                dispatchedActions.get(0).intValue());
    }

    @Test
    public void competingGestureSynchronouslyReceivesDeferredPrefix() {
        long downTime = SystemClock.uptimeMillis();
        MotionEvent down = event(downTime, 0, MotionEvent.ACTION_DOWN, 1, 0);
        MotionEvent secondDown =
                event(downTime, 10, MotionEvent.ACTION_POINTER_DOWN, 2, 1);
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                assertFalse(coordinator.onTouchEvent(eventView, down, 3));
                assertTrue(coordinator.onTouchEvent(eventView, secondDown, 3));
                coordinator.resolveForCompetingGesture();
                assertEquals(1, dispatchedActions.size());
            });
        }
        finally {
            down.recycle();
            secondDown.recycle();
        }

        assertEquals(
                MotionEvent.ACTION_POINTER_DOWN,
                dispatchedActions.get(0).intValue());
        assertEquals(0, recognizedGestureCount.get());
    }

    private static MotionEvent event(
            long downTime,
            long eventOffsetMs,
            int action,
            int pointerCount,
            int actionIndex) {
        MotionEvent.PointerProperties[] properties =
                new MotionEvent.PointerProperties[pointerCount];
        MotionEvent.PointerCoords[] coordinates =
                new MotionEvent.PointerCoords[pointerCount];

        for (int i = 0; i < pointerCount; i++) {
            MotionEvent.PointerProperties pointerProperties =
                    new MotionEvent.PointerProperties();
            pointerProperties.id = i;
            pointerProperties.toolType = MotionEvent.TOOL_TYPE_FINGER;
            properties[i] = pointerProperties;

            MotionEvent.PointerCoords pointerCoordinates =
                    new MotionEvent.PointerCoords();
            pointerCoordinates.x = 100 + i * 30;
            pointerCoordinates.y = 200 + i * 30;
            pointerCoordinates.pressure = 1;
            pointerCoordinates.size = 1;
            coordinates[i] = pointerCoordinates;
        }

        int encodedAction = action;
        if (action == MotionEvent.ACTION_POINTER_DOWN ||
                action == MotionEvent.ACTION_POINTER_UP) {
            encodedAction |=
                    actionIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        }

        return MotionEvent.obtain(
                downTime,
                downTime + eventOffsetMs,
                encodedAction,
                pointerCount,
                properties,
                coordinates,
                0,
                0,
                1,
                1,
                0,
                0,
                InputDevice.SOURCE_TOUCHSCREEN,
                0);
    }
}
