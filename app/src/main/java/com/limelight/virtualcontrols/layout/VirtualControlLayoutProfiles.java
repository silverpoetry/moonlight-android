package com.limelight.virtualcontrols.layout;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Canonical profile identifiers shared by settings validation and layout
 * storage.
 */
public final class VirtualControlLayoutProfiles {
    public static final String DEFAULT_KEYBOARD = "OSC_Keyboard";
    public static final String DEFAULT_GAMEPAD = "gamePad";

    private static final String[] KEYBOARD_IDS = {
            DEFAULT_KEYBOARD,
            "OSC_Keyboard_2",
            "OSC_Keyboard_3",
            "OSC_Keyboard_4",
            "OSC_Keyboard_5"
    };
    private static final String[] GAMEPAD_IDS = {
            DEFAULT_GAMEPAD,
            "gamePad_2",
            "gamePad_3",
            "gamePad_4",
            "gamePad_5"
    };
    private static final Set<String> KEYBOARD_ID_SET =
            immutableSet(KEYBOARD_IDS);
    private static final Set<String> GAMEPAD_ID_SET =
            immutableSet(GAMEPAD_IDS);

    public static String[] keyboardIds() {
        return KEYBOARD_IDS.clone();
    }

    public static String[] gamepadIds() {
        return GAMEPAD_IDS.clone();
    }

    public static boolean isKnown(
            VirtualControlLayoutKind kind,
            String profileId) {
        if (kind == null) {
            return false;
        }
        return (kind == VirtualControlLayoutKind.KEYBOARD
                        ? KEYBOARD_ID_SET
                        : GAMEPAD_ID_SET)
                .contains(profileId);
    }

    private static Set<String> immutableSet(String[] values) {
        return Collections.unmodifiableSet(
                new HashSet<>(Arrays.asList(values)));
    }

    private VirtualControlLayoutProfiles() {
    }
}
