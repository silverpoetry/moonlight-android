package com.limelight.binding.input.touch;

import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class BufferedTouchEventDispatcherTest {
    private BufferedTouchEventDispatcher dispatcher;
    private View eventView;

    @Before
    public void setUp() {
        dispatcher = new BufferedTouchEventDispatcher();
        eventView = new View(
                InstrumentationRegistry.getInstrumentation().getTargetContext());
    }

    @After
    public void tearDown() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(dispatcher::cancel);
    }

    @Test
    public void pointerUpReplayPreservesMinimumHoldAndQueuesNewInput() throws Exception {
        List<Integer> actions = Collections.synchronizedList(new ArrayList<>());
        List<Long> dispatchTimes = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch dispatched = new CountDownLatch(3);

        List<MotionEvent> bufferedEvents = Arrays.asList(
                event(10, MotionEvent.ACTION_POINTER_DOWN, 1, 2),
                event(20, MotionEvent.ACTION_POINTER_UP, 1, 2));
        MotionEvent finalUp = event(25, MotionEvent.ACTION_UP, 0, 1);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            dispatcher.dispatch(eventView, bufferedEvents, (view, event) -> {
                actions.add(event.getActionMasked());
                dispatchTimes.add(SystemClock.uptimeMillis());
                dispatched.countDown();
            });
            assertTrue(dispatcher.queueIfReplaying(eventView, finalUp));
        });
        finalUp.recycle();

        assertTrue(dispatched.await(2, TimeUnit.SECONDS));
        assertEquals(Arrays.asList(
                MotionEvent.ACTION_POINTER_DOWN,
                MotionEvent.ACTION_POINTER_UP,
                MotionEvent.ACTION_UP), actions);
        assertTrue(dispatchTimes.get(1) - dispatchTimes.get(0) >=
                BufferedTouchEventDispatcher.MIN_NATIVE_TAP_HOLD_MS - 5);
    }

    @Test
    public void movementDisqualificationReplaysImmediately() {
        List<Integer> actions = new ArrayList<>();
        List<MotionEvent> bufferedEvents = Arrays.asList(
                event(10, MotionEvent.ACTION_POINTER_DOWN, 1, 2),
                event(80, MotionEvent.ACTION_MOVE, 0, 2));

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                dispatcher.dispatch(eventView, bufferedEvents,
                        (view, event) -> actions.add(event.getActionMasked())));

        assertEquals(Arrays.asList(
                MotionEvent.ACTION_POINTER_DOWN,
                MotionEvent.ACTION_MOVE), actions);

        MotionEvent nextMove = event(90, MotionEvent.ACTION_MOVE, 0, 2);
        try {
            assertFalse(dispatcher.queueIfReplaying(eventView, nextMove));
        }
        finally {
            nextMove.recycle();
        }
    }

    private static MotionEvent event(long elapsedMs, int actionMasked,
                                     int actionIndex, int pointerCount) {
        MotionEvent.PointerProperties[] properties =
                new MotionEvent.PointerProperties[pointerCount];
        MotionEvent.PointerCoords[] coords =
                new MotionEvent.PointerCoords[pointerCount];

        for (int i = 0; i < pointerCount; i++) {
            MotionEvent.PointerProperties pointerProperties =
                    new MotionEvent.PointerProperties();
            pointerProperties.id = i;
            pointerProperties.toolType = MotionEvent.TOOL_TYPE_FINGER;
            properties[i] = pointerProperties;

            MotionEvent.PointerCoords pointerCoords = new MotionEvent.PointerCoords();
            pointerCoords.x = 100 + i * 200;
            pointerCoords.y = 100 + i * 100;
            pointerCoords.pressure = 1.0f;
            coords[i] = pointerCoords;
        }

        int action = actionMasked;
        if (actionMasked == MotionEvent.ACTION_POINTER_DOWN ||
                actionMasked == MotionEvent.ACTION_POINTER_UP) {
            action |= actionIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        }

        return MotionEvent.obtain(1_000, 1_000 + elapsedMs, action,
                pointerCount, properties, coords, 0, 0,
                1.0f, 1.0f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
    }
}
