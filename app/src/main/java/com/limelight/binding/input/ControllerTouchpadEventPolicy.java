package com.limelight.binding.input;

import android.view.MotionEvent;

/** Pure classification of Android touchpad actions. */
final class ControllerTouchpadEventPolicy {
    enum Dispatch {
        CONTACT_DOWN,
        CONTACT_UP,
        CONTACT_CANCEL,
        MOVE_ALL_CONTACTS,
        CANCEL_ALL_CONTACTS,
        BUTTON_DOWN,
        BUTTON_UP,
        UNHANDLED
    }

    private ControllerTouchpadEventPolicy() {
    }

    static Dispatch resolve(
            int actionMasked,
            int flags,
            int actionButton,
            boolean supportsActionButton) {
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                return Dispatch.CONTACT_DOWN;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                return (flags & MotionEvent.FLAG_CANCELED) != 0
                        ? Dispatch.CONTACT_CANCEL
                        : Dispatch.CONTACT_UP;
            case MotionEvent.ACTION_MOVE:
                return Dispatch.MOVE_ALL_CONTACTS;
            case MotionEvent.ACTION_CANCEL:
                return Dispatch.CANCEL_ALL_CONTACTS;
            case MotionEvent.ACTION_BUTTON_PRESS:
                return supportsActionButton &&
                        actionButton == MotionEvent.BUTTON_PRIMARY
                        ? Dispatch.BUTTON_DOWN
                        : Dispatch.UNHANDLED;
            case MotionEvent.ACTION_BUTTON_RELEASE:
                return supportsActionButton &&
                        actionButton == MotionEvent.BUTTON_PRIMARY
                        ? Dispatch.BUTTON_UP
                        : Dispatch.UNHANDLED;
            default:
                return Dispatch.UNHANDLED;
        }
    }
}
