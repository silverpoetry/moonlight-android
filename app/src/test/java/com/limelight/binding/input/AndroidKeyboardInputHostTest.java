package com.limelight.binding.input;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class AndroidKeyboardInputHostTest {
    @Test
    public void routesKeyboardActionsAndPreservesGrabDelay() {
        List<String> events = new ArrayList<>();
        AndroidKeyboardInputHost host = new AndroidKeyboardInputHost(
                () -> true,
                (action, delayMs) -> {
                    events.add("schedule:" + delayMs);
                    action.run();
                },
                () -> events.add("key"),
                () -> events.add("grab"),
                () -> events.add("quit"),
                () -> events.add("cursor"));

        assertTrue(host.isInputGrabbed());
        host.onNonBackKeyDown();
        host.requestToggleInputGrab();
        host.requestQuit();
        host.requestToggleCursorVisibility();

        assertEquals(List.of(
                "key",
                "schedule:250",
                "grab",
                "quit",
                "cursor"), events);
    }

    @Test
    public void readsCurrentGrabStateOnEveryQuery() {
        boolean[] grabbed = {false};
        AndroidKeyboardInputHost host = new AndroidKeyboardInputHost(
                () -> grabbed[0],
                (action, delayMs) -> { },
                () -> { },
                () -> { },
                () -> { },
                () -> { });

        assertFalse(host.isInputGrabbed());
        grabbed[0] = true;
        assertTrue(host.isInputGrabbed());
    }
}
