package com.limelight.binding.input.touch;

import android.content.Context;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import com.limelight.binding.input.PointerInputSink;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.settings.input.InputSettings;
import com.limelight.settings.input.InputSettingsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class TouchscreenTouchpadHandlerTest {
    private static final long DOWN_TIME_MS = 1_000;

    private RecordingPointerInputSink connection;
    private View eventView;
    private InputSettingsState settingsState;

    @Before
    public void setUp() {
        Context context =
                InstrumentationRegistry.getInstrumentation().getTargetContext();
        connection = new RecordingPointerInputSink();
        eventView = new View(context);
        eventView.layout(0, 0, 1_000, 500);
        settingsState = new InputSettingsState(
                InputSettings.builder().build());
    }

    @Test
    public void standaloneFingerRemainsOnLegacyAbsoluteMousePath() {
        TouchscreenTouchpadHandler handler = createHandler();
        handler.setSinglePointerRemainderMode(
                TouchscreenTouchpadHandler.SinglePointerRemainderMode.SUPPRESS);

        assertFalse(handler.handleMotionEvent(eventView,
                event(0, MotionEvent.ACTION_DOWN, 0, 100, 100)));
        assertFalse(handler.handleMotionEvent(eventView,
                event(10, MotionEvent.ACTION_MOVE, 0, 120, 130)));
        assertFalse(handler.handleMotionEvent(eventView,
                event(20, MotionEvent.ACTION_UP, 0, 120, 130)));

        assertTrue(connection.frames.isEmpty());
        assertEquals(0, connection.mouseMovePackets);
        assertFalse(handler.isHandlingGesture());
    }

    @Test
    public void absoluteMouseSuppressesTailWithoutMovingTapPosition() {
        TouchscreenTouchpadHandler handler = createHandler();
        handler.setSinglePointerRemainderMode(
                TouchscreenTouchpadHandler.SinglePointerRemainderMode.SUPPRESS);

        assertFalse(handler.handleMotionEvent(eventView,
                event(0, MotionEvent.ACTION_DOWN, 0, 100, 100)));
        assertTrue(handler.handleMotionEvent(eventView,
                event(10, MotionEvent.ACTION_POINTER_DOWN, 1,
                        100, 100, 700, 300)));

        assertEquals(1, connection.frames.size());
        assertEquals(2, connection.frames.get(0).contactCount);
        assertEquals(MoonBridge.LI_TOUCH_EVENT_DOWN, connection.frames.get(0).eventTypes[0]);
        assertEquals(MoonBridge.LI_TOUCH_EVENT_DOWN, connection.frames.get(0).eventTypes[1]);
        assertTrue(handler.isHandlingGesture());

        assertTrue(handler.handleMotionEvent(eventView,
                event(20, MotionEvent.ACTION_MOVE, 0,
                        130, 110, 730, 310)));
        assertEquals(2, connection.frames.size());
        assertEquals(MoonBridge.LI_TOUCH_EVENT_MOVE, connection.frames.get(1).eventTypes[0]);
        assertEquals(MoonBridge.LI_TOUCH_EVENT_MOVE, connection.frames.get(1).eventTypes[1]);

        assertTrue(handler.handleMotionEvent(eventView,
                event(30, MotionEvent.ACTION_POINTER_UP, 1,
                        150, 120, 750, 320)));
        assertEquals(3, connection.frames.size());
        assertEquals(MoonBridge.LI_TOUCH_EVENT_UP, connection.frames.get(2).eventTypes[0]);
        assertEquals(MoonBridge.LI_TOUCH_EVENT_UP, connection.frames.get(2).eventTypes[1]);

        assertTrue(handler.handleMotionEvent(eventView,
                event(40, MotionEvent.ACTION_MOVE, 0, 180, 140)));
        assertEquals(3, connection.frames.size());
        assertEquals(0, connection.mouseMovePackets);

        assertTrue(handler.handleMotionEvent(eventView,
                event(50, MotionEvent.ACTION_UP, 0, 190, 150)));
        assertEquals(0, connection.mouseMovePackets);
        assertFalse(handler.isHandlingGesture());
    }

    @Test
    public void twoFingerGestureProtocolTraceIsStable() {
        TouchscreenTouchpadHandler handler = createHandler();
        handler.setSinglePointerRemainderMode(
                TouchscreenTouchpadHandler.SinglePointerRemainderMode.SUPPRESS);

        handler.handleMotionEvent(eventView,
                event(0, MotionEvent.ACTION_DOWN, 0, 100, 100));
        handler.handleMotionEvent(eventView,
                event(10, MotionEvent.ACTION_POINTER_DOWN, 1,
                        100, 100, 700, 300));
        handler.handleMotionEvent(eventView,
                event(20, MotionEvent.ACTION_MOVE, 0,
                        120, 110, 720, 310));
        handler.handleMotionEvent(eventView,
                event(30, MotionEvent.ACTION_POINTER_UP, 1,
                        130, 120, 730, 320));
        handler.handleMotionEvent(eventView,
                event(40, MotionEvent.ACTION_UP, 0, 130, 120));

        assertEquals(
                "2:DOWN#0@0.1000,0.2000|DOWN#1@0.7000,0.6000:b0\n" +
                        "2:MOVE#0@0.1200,0.2200|MOVE#1@0.7200,0.6200:b0\n" +
                        "2:UP#0@0.1300,0.2400|UP#1@0.7300,0.6400:b0",
                connection.frameTrace());
        assertEquals(0, connection.mouseMovePackets);
        assertEquals(0, connection.mouseButtonPackets);
    }

    @Test
    public void relativeTouchpadTailNeverUsesAbsoluteTouchscreenPosition() {
        TouchscreenTouchpadHandler handler = createHandler();

        assertFalse(handler.handleMotionEvent(eventView,
                event(0, MotionEvent.ACTION_DOWN, 0, 100, 100)));
        assertTrue(handler.handleMotionEvent(eventView,
                event(10, MotionEvent.ACTION_POINTER_DOWN, 1,
                        100, 100, 700, 300)));
        assertTrue(handler.handleMotionEvent(eventView,
                event(20, MotionEvent.ACTION_POINTER_UP, 1,
                        120, 110, 720, 310)));
        assertTrue(handler.handleMotionEvent(eventView,
                event(30, MotionEvent.ACTION_MOVE, 0, 170, 140)));
        assertTrue(handler.handleMotionEvent(eventView,
                event(40, MotionEvent.ACTION_UP, 0, 170, 140)));

        assertTrue(connection.mouseMovePackets > 0);
        assertFalse(handler.isHandlingGesture());
    }

    @Test
    public void suppressedTailRequiresAllPointersToLiftBeforeNextNativeGesture() {
        TouchscreenTouchpadHandler handler = createHandler();
        handler.setSinglePointerRemainderMode(
                TouchscreenTouchpadHandler.SinglePointerRemainderMode.SUPPRESS);

        handler.handleMotionEvent(eventView,
                event(0, MotionEvent.ACTION_DOWN, 0, 100, 100));
        handler.handleMotionEvent(eventView,
                event(10, MotionEvent.ACTION_POINTER_DOWN, 1,
                        100, 100, 700, 300));
        handler.handleMotionEvent(eventView,
                event(20, MotionEvent.ACTION_POINTER_UP, 1,
                        120, 110, 720, 310));
        handler.handleMotionEvent(eventView,
                event(30, MotionEvent.ACTION_POINTER_DOWN, 1,
                        130, 120, 500, 250));

        assertEquals(2, connection.frames.size());
        assertTrue(handler.isHandlingGesture());

        handler.handleMotionEvent(eventView,
                event(40, MotionEvent.ACTION_POINTER_UP, 1,
                        130, 120, 500, 250));
        handler.handleMotionEvent(eventView,
                event(50, MotionEvent.ACTION_UP, 0, 130, 120));
        assertFalse(handler.isHandlingGesture());

        assertFalse(handler.handleMotionEvent(eventView,
                event(60, MotionEvent.ACTION_DOWN, 0, 200, 150)));
        assertTrue(handler.handleMotionEvent(eventView,
                event(70, MotionEvent.ACTION_POINTER_DOWN, 1,
                        200, 150, 600, 300)));
        assertEquals(3, connection.frames.size());
        assertEquals(2, connection.frames.get(2).contactCount);
    }

    @Test
    public void forcePressUsesNativeTouchpadButtonStateForSameContact() {
        settingsState.replace(
                settingsState.get()
                        .toBuilder()
                        .setAbsoluteMouseMode(true)
                        .build());
        TouchscreenTouchpadHandler handler = createHandler();
        TouchpadMotionSender sharedMotionSender =
                new TouchpadMotionSender(connection, 1_280, 720,
                        eventView, settingsState);
        RelativeTouchContext primaryTouchContext =
                new RelativeTouchContext(connection, 0, 1_280, 720,
                        eventView, settingsState,
                        new TouchpadGestureState(),
                        sharedMotionSender);
        primaryTouchContext.setNativeTouchpadPressHandlingEnabled(true);
        final int[] ownershipTransitions = {0};
        handler.setNativeGestureListener(() -> {
            ownershipTransitions[0]++;
            primaryTouchContext.cancelTouch();
            primaryTouchContext.setPointerCount(0);
        });
        handler.configureNativePressHandling(
                true, true, sharedMotionSender);

        assertFalse(handler.handleMotionEvent(eventView,
                event(0, MotionEvent.ACTION_DOWN, 0, 100, 100)));
        primaryTouchContext.setPointerCount(1);
        primaryTouchContext.touchDownEvent(100, 100,
                DOWN_TIME_MS, true);
        assertTrue(handler.beginForcePress(0, 1));

        assertEquals(1, ownershipTransitions[0]);
        assertEquals(2, connection.frames.size());
        assertEquals(MoonBridge.LI_TOUCH_EVENT_DOWN,
                connection.frames.get(0).eventTypes[0]);
        assertEquals(0, connection.frames.get(0).buttonState);
        assertEquals(MoonBridge.LI_TOUCH_EVENT_BUTTON_ONLY,
                connection.frames.get(1).eventTypes[0]);
        assertEquals(MoonBridge.LI_TOUCHPAD_BUTTON_PRIMARY,
                connection.frames.get(1).buttonState);

        assertTrue(handler.handleMotionEvent(eventView,
                event(10, MotionEvent.ACTION_MOVE, 0, 140, 120)));
        assertEquals(MoonBridge.LI_TOUCH_EVENT_BUTTON_ONLY,
                connection.frames.get(2).eventTypes[0]);
        assertEquals(MoonBridge.LI_TOUCHPAD_BUTTON_PRIMARY,
                connection.frames.get(2).buttonState);
        assertTrue(connection.mouseMovePackets > 0);
        assertTrue(connection.absoluteMouseMovePackets > 0);
        assertEquals(0, connection.relativeMouseMovePackets);
        assertEquals(0, connection.mouseButtonPackets);
        assertEquals(connection.frames.get(0).x[0],
                connection.frames.get(2).x[0], 0.0f);
        assertEquals(connection.frames.get(0).y[0],
                connection.frames.get(2).y[0], 0.0f);

        assertTrue(handler.endForcePress(0, true));
        assertEquals(MoonBridge.LI_TOUCH_EVENT_BUTTON_ONLY,
                connection.frames.get(3).eventTypes[0]);
        assertEquals(0, connection.frames.get(3).buttonState);

        assertTrue(handler.handleMotionEvent(eventView,
                event(20, MotionEvent.ACTION_UP, 0, 140, 120)));
        assertEquals(MoonBridge.LI_TOUCH_EVENT_UP,
                connection.frames.get(4).eventTypes[0]);
        assertEquals(0, connection.frames.get(4).buttonState);
        assertEquals(connection.frames.get(0).x[0],
                connection.frames.get(4).x[0], 0.0f);
        assertEquals(connection.frames.get(0).y[0],
                connection.frames.get(4).y[0], 0.0f);
        assertFalse(handler.isHandlingGesture());
    }

    @Test
    public void stationaryLongPressPromotesContactWithoutMouseButtonPackets() {
        TouchscreenTouchpadHandler handler = createHandler();
        handler.configureNativePressHandling(
                true, false,
                new TouchpadMotionSender(connection, 1_280, 720,
                        eventView, settingsState));

        assertFalse(handler.handleMotionEvent(eventView,
                event(0, MotionEvent.ACTION_DOWN, 0, 100, 100)));
        SystemClock.sleep(
                settingsState.get().getTouchpadLongPressDurationMs() + 100L);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertEquals(2, connection.frames.size());
        assertEquals(MoonBridge.LI_TOUCH_EVENT_DOWN,
                connection.frames.get(0).eventTypes[0]);
        assertEquals(MoonBridge.LI_TOUCH_EVENT_BUTTON_ONLY,
                connection.frames.get(1).eventTypes[0]);
        assertEquals(MoonBridge.LI_TOUCHPAD_BUTTON_PRIMARY,
                connection.frames.get(1).buttonState);
        assertEquals(0, connection.mouseButtonPackets);
    }

    private TouchscreenTouchpadHandler createHandler() {
        return new TouchscreenTouchpadHandler(connection, eventView,
                1_280, 720, settingsState);
    }

    private static MotionEvent event(long elapsedMs, int actionMasked, int actionIndex,
                                     float... coordinates) {
        int pointerCount = coordinates.length / 2;
        MotionEvent.PointerProperties[] properties =
                new MotionEvent.PointerProperties[pointerCount];
        MotionEvent.PointerCoords[] pointerCoords =
                new MotionEvent.PointerCoords[pointerCount];

        for (int i = 0; i < pointerCount; i++) {
            MotionEvent.PointerProperties pointerProperties =
                    new MotionEvent.PointerProperties();
            pointerProperties.id = i;
            pointerProperties.toolType = MotionEvent.TOOL_TYPE_FINGER;
            properties[i] = pointerProperties;

            MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
            coords.x = coordinates[i * 2];
            coords.y = coordinates[i * 2 + 1];
            coords.pressure = 1.0f;
            coords.size = 0.05f;
            coords.touchMajor = 20;
            coords.touchMinor = 16;
            pointerCoords[i] = coords;
        }

        int action = actionMasked;
        if (actionMasked == MotionEvent.ACTION_POINTER_DOWN ||
                actionMasked == MotionEvent.ACTION_POINTER_UP) {
            action |= actionIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        }

        return MotionEvent.obtain(DOWN_TIME_MS, DOWN_TIME_MS + elapsedMs, action,
                pointerCount, properties, pointerCoords, 0, 0,
                1.0f, 1.0f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
    }

    private static final class Frame {
        final int contactCount;
        final byte[] eventTypes;
        final int[] pointerIds;
        final float[] x;
        final float[] y;
        final byte buttonState;

        Frame(int contactCount, byte[] eventTypes, int[] pointerIds,
              float[] x, float[] y, byte buttonState) {
            this.contactCount = contactCount;
            this.eventTypes = eventTypes;
            this.pointerIds = pointerIds;
            this.x = x;
            this.y = y;
            this.buttonState = buttonState;
        }
    }

    private static final class RecordingPointerInputSink
            implements PointerInputSink {
        final List<Frame> frames = new ArrayList<>();
        int mouseMovePackets;
        int relativeMouseMovePackets;
        int absoluteMouseMovePackets;
        int mouseButtonPackets;

        @Override
        public void sendMousePosition(short x, short y,
                                      short referenceWidth,
                                      short referenceHeight) {
        }

        @Override
        public int sendTouchpadFrameEvent(byte contactCount, byte[] eventTypes,
                                          int[] pointerIds, float[] x, float[] y,
                                          float[] pressure, short rotation,
                                          short deviceWidthMm, short deviceHeightMm,
                                          byte buttonState) {
            byte[] copiedEventTypes = new byte[contactCount];
            System.arraycopy(eventTypes, 0, copiedEventTypes, 0, contactCount);
            int[] copiedPointerIds = new int[contactCount];
            System.arraycopy(pointerIds, 0, copiedPointerIds, 0, contactCount);
            float[] copiedX = new float[contactCount];
            float[] copiedY = new float[contactCount];
            System.arraycopy(x, 0, copiedX, 0, contactCount);
            System.arraycopy(y, 0, copiedY, 0, contactCount);
            frames.add(new Frame(contactCount, copiedEventTypes,
                    copiedPointerIds,
                    copiedX, copiedY, buttonState));
            return 0;
        }

        @Override
        public int sendTouchpadEvent(byte eventType, int pointerId, float x, float y,
                                     float pressure, float contactAreaMajor,
                                     float contactAreaMinor, short rotation,
                                     short deviceWidthMm, short deviceHeightMm,
                                     byte buttonState) {
            return 0;
        }

        @Override
        public void sendMouseMove(short deltaX, short deltaY) {
            mouseMovePackets++;
            relativeMouseMovePackets++;
        }

        @Override
        public void sendMouseMoveAsMousePosition(short deltaX, short deltaY,
                                                 short referenceWidth,
                                                 short referenceHeight) {
            mouseMovePackets++;
            absoluteMouseMovePackets++;
        }

        @Override
        public void sendMouseButtonDown(byte mouseButton) {
            mouseButtonPackets++;
        }

        @Override
        public void sendMouseButtonUp(byte mouseButton) {
            mouseButtonPackets++;
        }

        @Override
        public void sendMouseHighResScroll(short delta) {
        }

        @Override
        public void sendMouseHighResHScroll(short delta) {
        }

        @Override
        public int sendTouchEvent(
                byte eventType,
                int pointerId,
                float x,
                float y,
                float pressureOrDistance,
                float contactAreaMajor,
                float contactAreaMinor,
                short rotation) {
            return 0;
        }

        @Override
        public int sendPenEvent(
                byte eventType,
                byte toolType,
                byte penButtons,
                float x,
                float y,
                float pressureOrDistance,
                float contactAreaMajor,
                float contactAreaMinor,
                short rotation,
                byte tilt) {
            return 0;
        }

        String frameTrace() {
            StringBuilder trace = new StringBuilder();
            for (Frame frame : frames) {
                if (trace.length() != 0) {
                    trace.append('\n');
                }
                trace.append(frame.contactCount).append(':');
                for (int i = 0; i < frame.contactCount; i++) {
                    if (i != 0) {
                        trace.append('|');
                    }
                    trace.append(eventTypeName(frame.eventTypes[i]))
                            .append('#')
                            .append(frame.pointerIds[i])
                            .append('@')
                            .append(String.format(
                                    Locale.US,
                                    "%.4f,%.4f",
                                    frame.x[i],
                                    frame.y[i]));
                }
                trace.append(":b").append(frame.buttonState);
            }
            return trace.toString();
        }

        private static String eventTypeName(byte eventType) {
            if (eventType == MoonBridge.LI_TOUCH_EVENT_DOWN) {
                return "DOWN";
            }
            if (eventType == MoonBridge.LI_TOUCH_EVENT_MOVE) {
                return "MOVE";
            }
            if (eventType == MoonBridge.LI_TOUCH_EVENT_UP) {
                return "UP";
            }
            if (eventType == MoonBridge.LI_TOUCH_EVENT_CANCEL) {
                return "CANCEL";
            }
            if (eventType == MoonBridge.LI_TOUCH_EVENT_BUTTON_ONLY) {
                return "BUTTON";
            }
            return Byte.toString(eventType);
        }
    }
}
