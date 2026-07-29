package com.limelight.binding.input.touch;

import android.support.test.runner.AndroidJUnit4;
import android.view.InputDevice;
import android.view.MotionEvent;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public final class SoftKeyboardGestureDetectorTest {
    private static final long DOWN_TIME_MS = 1_000;
    private static final int TOUCH_SLOP_PX = 10;

    @Test
    public void threeFingerSwipeIsNeverConsumed() {
        SoftKeyboardGestureDetector detector =
                new SoftKeyboardGestureDetector(TOUCH_SLOP_PX);

        assertResult(detector, event(0, MotionEvent.ACTION_DOWN, 1, 0), 3,
                SoftKeyboardGestureDetector.Result.NONE);
        assertResult(detector, event(10, MotionEvent.ACTION_POINTER_DOWN, 2, 1), 3,
                SoftKeyboardGestureDetector.Result.NONE);
        assertResult(detector, event(20, MotionEvent.ACTION_POINTER_DOWN, 3, 2), 3,
                SoftKeyboardGestureDetector.Result.STARTED);
        assertResult(detector, event(30, MotionEvent.ACTION_MOVE, 3, 0, 40), 3,
                SoftKeyboardGestureDetector.Result.FORWARD);
        assertBufferedEventCount(detector, 2);
        assertResult(detector, event(40, MotionEvent.ACTION_POINTER_UP, 3, 2, 40), 3,
                SoftKeyboardGestureDetector.Result.NONE);
        assertResult(detector, event(50, MotionEvent.ACTION_POINTER_UP, 2, 1, 40), 3,
                SoftKeyboardGestureDetector.Result.NONE);
        assertResult(detector, event(60, MotionEvent.ACTION_UP, 1, 0, 40), 3,
                SoftKeyboardGestureDetector.Result.NONE);
    }

    @Test
    public void exactThreeFingerTapConsumesOnlyAfterRecognition() {
        SoftKeyboardGestureDetector detector =
                new SoftKeyboardGestureDetector(TOUCH_SLOP_PX);

        assertResult(detector, event(0, MotionEvent.ACTION_DOWN, 1, 0), 3,
                SoftKeyboardGestureDetector.Result.NONE);
        assertResult(detector, event(10, MotionEvent.ACTION_POINTER_DOWN, 2, 1), 3,
                SoftKeyboardGestureDetector.Result.NONE);
        assertResult(detector, event(20, MotionEvent.ACTION_POINTER_DOWN, 3, 2), 3,
                SoftKeyboardGestureDetector.Result.STARTED);
        assertResult(detector, event(60, MotionEvent.ACTION_POINTER_UP, 3, 2), 3,
                SoftKeyboardGestureDetector.Result.TRIGGERED);
        assertBufferedEventCount(detector, 0);
        assertResult(detector, event(70, MotionEvent.ACTION_POINTER_UP, 2, 1), 3,
                SoftKeyboardGestureDetector.Result.CONSUMED);
        assertResult(detector, event(80, MotionEvent.ACTION_UP, 1, 0), 3,
                SoftKeyboardGestureDetector.Result.CONSUMED);
        assertResult(detector, event(100, MotionEvent.ACTION_DOWN, 1, 0), 3,
                SoftKeyboardGestureDetector.Result.NONE);
    }

    @Test
    public void twoFingerGestureIsUnaffected() {
        SoftKeyboardGestureDetector detector =
                new SoftKeyboardGestureDetector(TOUCH_SLOP_PX);

        assertResult(detector, event(0, MotionEvent.ACTION_DOWN, 1, 0), 3,
                SoftKeyboardGestureDetector.Result.NONE);
        assertResult(detector, event(10, MotionEvent.ACTION_POINTER_DOWN, 2, 1), 3,
                SoftKeyboardGestureDetector.Result.NONE);
        assertResult(detector, event(20, MotionEvent.ACTION_MOVE, 2, 0, 30), 3,
                SoftKeyboardGestureDetector.Result.NONE);
        assertBufferedEventCount(detector, 0);
        assertResult(detector, event(30, MotionEvent.ACTION_POINTER_UP, 2, 1, 30), 3,
                SoftKeyboardGestureDetector.Result.NONE);
        assertResult(detector, event(40, MotionEvent.ACTION_UP, 1, 0, 30), 3,
                SoftKeyboardGestureDetector.Result.NONE);
    }

    @Test
    public void addingThirdFingerToExistingScrollDoesNotTriggerKeyboard() {
        SoftKeyboardGestureDetector detector =
                new SoftKeyboardGestureDetector(TOUCH_SLOP_PX);

        assertResult(detector, event(0, MotionEvent.ACTION_DOWN, 1, 0), 3,
                SoftKeyboardGestureDetector.Result.NONE);
        assertResult(detector, event(10, MotionEvent.ACTION_POINTER_DOWN, 2, 1), 3,
                SoftKeyboardGestureDetector.Result.NONE);
        assertResult(detector, event(20, MotionEvent.ACTION_MOVE, 2, 0, 30), 3,
                SoftKeyboardGestureDetector.Result.NONE);
        assertBufferedEventCount(detector, 0);
        assertResult(detector, event(30, MotionEvent.ACTION_POINTER_DOWN, 3, 2, 30), 3,
                SoftKeyboardGestureDetector.Result.NONE);
        assertResult(detector, event(40, MotionEvent.ACTION_POINTER_UP, 3, 2, 30), 3,
                SoftKeyboardGestureDetector.Result.NONE);
    }

    @Test
    public void extraFingerDisqualifiesTapWithoutConsumingGesture() {
        SoftKeyboardGestureDetector detector =
                new SoftKeyboardGestureDetector(TOUCH_SLOP_PX);

        assertResult(detector, event(0, MotionEvent.ACTION_DOWN, 1, 0), 3,
                SoftKeyboardGestureDetector.Result.NONE);
        assertResult(detector, event(10, MotionEvent.ACTION_POINTER_DOWN, 2, 1), 3,
                SoftKeyboardGestureDetector.Result.NONE);
        assertResult(detector, event(20, MotionEvent.ACTION_POINTER_DOWN, 3, 2), 3,
                SoftKeyboardGestureDetector.Result.STARTED);
        assertResult(detector, event(30, MotionEvent.ACTION_POINTER_DOWN, 4, 3), 3,
                SoftKeyboardGestureDetector.Result.FORWARD);
        assertBufferedEventCount(detector, 2);
        assertResult(detector, event(40, MotionEvent.ACTION_POINTER_UP, 4, 3), 3,
                SoftKeyboardGestureDetector.Result.NONE);
    }

    @Test
    public void disabledGestureNeverBuffersEvents() {
        SoftKeyboardGestureDetector detector =
                new SoftKeyboardGestureDetector(TOUCH_SLOP_PX);

        assertResult(detector, event(0, MotionEvent.ACTION_DOWN, 1, 0), 0,
                SoftKeyboardGestureDetector.Result.NONE);
        assertResult(detector, event(10, MotionEvent.ACTION_POINTER_DOWN, 2, 1), 0,
                SoftKeyboardGestureDetector.Result.NONE);
        assertResult(detector, event(20, MotionEvent.ACTION_POINTER_DOWN, 3, 2), 0,
                SoftKeyboardGestureDetector.Result.NONE);
        assertResult(detector, event(30, MotionEvent.ACTION_MOVE, 3, 0, 40), 0,
                SoftKeyboardGestureDetector.Result.NONE);
        assertBufferedEventCount(detector, 0);
    }

    @Test
    public void twoFingerTapNeverEntersKeyboardBuffer() {
        SoftKeyboardGestureDetector detector =
                new SoftKeyboardGestureDetector(TOUCH_SLOP_PX);

        assertResult(detector, event(0, MotionEvent.ACTION_DOWN, 1, 0), 3,
                SoftKeyboardGestureDetector.Result.NONE);
        assertResult(detector, event(10, MotionEvent.ACTION_POINTER_DOWN, 2, 1), 3,
                SoftKeyboardGestureDetector.Result.NONE);
        assertResult(detector, event(40, MotionEvent.ACTION_POINTER_UP, 2, 1), 3,
                SoftKeyboardGestureDetector.Result.NONE);
        assertBufferedEventCount(detector, 0);
    }

    private static void assertBufferedEventCount(
            SoftKeyboardGestureDetector detector,
            int expectedCount) {
        List<MotionEvent> events = detector.takeBufferedEvents();
        try {
            assertEquals(expectedCount, events.size());
        }
        finally {
            recycle(events);
        }
    }

    private static void recycle(List<MotionEvent> events) {
        for (MotionEvent event : events) {
            event.recycle();
        }
    }

    private static void assertResult(
            SoftKeyboardGestureDetector detector,
            MotionEvent event,
            int fingerCount,
            SoftKeyboardGestureDetector.Result expected) {
        try {
            assertEquals(expected, detector.onTouchEvent(event, fingerCount));
        }
        finally {
            event.recycle();
        }
    }

    private static MotionEvent event(
            long eventOffsetMs,
            int action,
            int pointerCount,
            int actionIndex) {
        return event(eventOffsetMs, action, pointerCount, actionIndex, 0);
    }

    private static MotionEvent event(
            long eventOffsetMs,
            int action,
            int pointerCount,
            int actionIndex,
            float xOffset) {
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

            MotionEvent.PointerCoords pointerCoordinates = new MotionEvent.PointerCoords();
            pointerCoordinates.x = 100 + i * 30 + xOffset;
            pointerCoordinates.y = 200 + i * 30;
            pointerCoordinates.pressure = 1;
            pointerCoordinates.size = 1;
            coordinates[i] = pointerCoordinates;
        }

        int encodedAction = action;
        if (action == MotionEvent.ACTION_POINTER_DOWN ||
                action == MotionEvent.ACTION_POINTER_UP) {
            encodedAction |= actionIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        }

        return MotionEvent.obtain(
                DOWN_TIME_MS,
                DOWN_TIME_MS + eventOffsetMs,
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
