package com.limelight.binding.input;

import android.view.MotionEvent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ControllerTouchpadEventPolicyTest {
    @Test
    public void downActionsAddressOneContact() {
        assertDispatch(
                ControllerTouchpadEventPolicy.Dispatch.CONTACT_DOWN,
                MotionEvent.ACTION_DOWN,
                0,
                0,
                true);
        assertDispatch(
                ControllerTouchpadEventPolicy.Dispatch.CONTACT_DOWN,
                MotionEvent.ACTION_POINTER_DOWN,
                0,
                0,
                true);
    }

    @Test
    public void upActionDistinguishesCanceledContact() {
        assertDispatch(
                ControllerTouchpadEventPolicy.Dispatch.CONTACT_UP,
                MotionEvent.ACTION_POINTER_UP,
                0,
                0,
                true);
        assertDispatch(
                ControllerTouchpadEventPolicy.Dispatch.CONTACT_CANCEL,
                MotionEvent.ACTION_POINTER_UP,
                MotionEvent.FLAG_CANCELED,
                0,
                true);
    }

    @Test
    public void moveAndCancelAddressAllContacts() {
        assertDispatch(
                ControllerTouchpadEventPolicy.Dispatch.MOVE_ALL_CONTACTS,
                MotionEvent.ACTION_MOVE,
                0,
                0,
                true);
        assertDispatch(
                ControllerTouchpadEventPolicy.Dispatch.CANCEL_ALL_CONTACTS,
                MotionEvent.ACTION_CANCEL,
                0,
                0,
                true);
    }

    @Test
    public void primaryClickpadButtonRequiresSupportedActionButtonApi() {
        assertDispatch(
                ControllerTouchpadEventPolicy.Dispatch.BUTTON_DOWN,
                MotionEvent.ACTION_BUTTON_PRESS,
                0,
                MotionEvent.BUTTON_PRIMARY,
                true);
        assertDispatch(
                ControllerTouchpadEventPolicy.Dispatch.BUTTON_UP,
                MotionEvent.ACTION_BUTTON_RELEASE,
                0,
                MotionEvent.BUTTON_PRIMARY,
                true);
        assertDispatch(
                ControllerTouchpadEventPolicy.Dispatch.UNHANDLED,
                MotionEvent.ACTION_BUTTON_PRESS,
                0,
                MotionEvent.BUTTON_PRIMARY,
                false);
    }

    @Test
    public void secondaryAndUnknownActionsAreUnhandled() {
        assertDispatch(
                ControllerTouchpadEventPolicy.Dispatch.UNHANDLED,
                MotionEvent.ACTION_BUTTON_PRESS,
                0,
                MotionEvent.BUTTON_SECONDARY,
                true);
        assertDispatch(
                ControllerTouchpadEventPolicy.Dispatch.UNHANDLED,
                999,
                0,
                0,
                true);
    }

    private static void assertDispatch(
            ControllerTouchpadEventPolicy.Dispatch expected,
            int action,
            int flags,
            int actionButton,
            boolean supportsActionButton) {
        assertEquals(
                expected,
                ControllerTouchpadEventPolicy.resolve(
                        action,
                        flags,
                        actionButton,
                        supportsActionButton));
    }
}
