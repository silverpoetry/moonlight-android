package com.limelight.binding.input;

import java.util.Objects;

/**
 * Resolves known Android controller key-layout quirks from immutable device facts.
 */
final class ControllerDeviceQuirks {
    private static final float LEGACY_TRIGGER_DEADZONE = 0.30f;

    private final boolean hasMode;
    private final boolean hasSelect;
    private final boolean serval;
    private final boolean nonStandardXboxBluetooth;
    private final boolean backIsStart;
    private final boolean modeIsSelect;
    private final boolean searchIsMode;
    private final float triggerDeadzone;

    private ControllerDeviceQuirks(
            boolean hasMode,
            boolean hasSelect,
            boolean serval,
            boolean nonStandardXboxBluetooth,
            boolean backIsStart,
            boolean modeIsSelect,
            boolean searchIsMode,
            float triggerDeadzone) {
        this.hasMode = hasMode;
        this.hasSelect = hasSelect;
        this.serval = serval;
        this.nonStandardXboxBluetooth =
                nonStandardXboxBluetooth;
        this.backIsStart = backIsStart;
        this.modeIsSelect = modeIsSelect;
        this.searchIsMode = searchIsMode;
        this.triggerDeadzone = triggerDeadzone;
    }

    static ControllerDeviceQuirks resolve(Facts facts) {
        Objects.requireNonNull(facts, "facts");
        boolean hasMode = facts.hasMode;
        boolean hasSelect = facts.hasSelect;
        boolean serval = false;
        boolean nonStandardXboxBluetooth = false;
        boolean backIsStart = false;
        boolean modeIsSelect = false;
        boolean searchIsMode = false;
        float triggerDeadzone = facts.triggerDeadzone;

        if (facts.vendorId == 0x18d1 &&
                facts.productId == 0x2c40) {
            backIsStart = true;
            modeIsSelect = true;
            triggerDeadzone = LEGACY_TRIGGER_DEADZONE;
            hasSelect = true;
            hasMode = false;
        }

        String name = facts.deviceName;
        if (name != null) {
            if (name.contains("ASUS Gamepad")) {
                if (!facts.hasStartOrMenu) {
                    backIsStart = true;
                    modeIsSelect = true;
                    hasSelect = true;
                    hasMode = false;
                }
                triggerDeadzone = LEGACY_TRIGGER_DEADZONE;
            }
            else if (name.contains("SHIELD") ||
                    name.contains("NVIDIA Controller")) {
                if (name.contains("NVIDIA Controller v01.03") ||
                        name.contains("NVIDIA Controller v01.04")) {
                    searchIsMode = true;
                    hasMode = true;
                }
            }
            else if (name.contains("Razer Serval")) {
                serval = true;
                hasMode = true;
                hasSelect = true;
            }
            else if (name.equals("Xbox Wireless Controller") &&
                    !facts.hasGasAxis) {
                nonStandardXboxBluetooth = true;
                hasMode = true;
                hasSelect = true;
            }
        }

        if (facts.vendorId == 0x044f &&
                facts.productId == 0xb328) {
            hasMode = false;
        }

        return new ControllerDeviceQuirks(
                hasMode,
                hasSelect,
                serval,
                nonStandardXboxBluetooth,
                backIsStart,
                modeIsSelect,
                searchIsMode,
                triggerDeadzone);
    }

    boolean hasMode() {
        return hasMode;
    }

    boolean hasSelect() {
        return hasSelect;
    }

    boolean isServal() {
        return serval;
    }

    boolean isNonStandardXboxBluetooth() {
        return nonStandardXboxBluetooth;
    }

    boolean isBackStart() {
        return backIsStart;
    }

    boolean isModeSelect() {
        return modeIsSelect;
    }

    boolean isSearchMode() {
        return searchIsMode;
    }

    float getTriggerDeadzone() {
        return triggerDeadzone;
    }

    static final class Facts {
        private final int vendorId;
        private final int productId;
        private final String deviceName;
        private final boolean hasMode;
        private final boolean hasSelect;
        private final boolean hasStartOrMenu;
        private final boolean hasGasAxis;
        private final float triggerDeadzone;

        private Facts(Builder builder) {
            vendorId = builder.vendorId;
            productId = builder.productId;
            deviceName = builder.deviceName;
            hasMode = builder.hasMode;
            hasSelect = builder.hasSelect;
            hasStartOrMenu = builder.hasStartOrMenu;
            hasGasAxis = builder.hasGasAxis;
            triggerDeadzone = builder.triggerDeadzone;
        }

        static Builder builder(
                int vendorId,
                int productId) {
            return new Builder(vendorId, productId);
        }

        static final class Builder {
            private final int vendorId;
            private final int productId;
            private String deviceName;
            private boolean hasMode;
            private boolean hasSelect;
            private boolean hasStartOrMenu;
            private boolean hasGasAxis;
            private float triggerDeadzone;

            private Builder(
                    int vendorId,
                    int productId) {
                this.vendorId = vendorId;
                this.productId = productId;
            }

            Builder deviceName(String name) {
                deviceName = name;
                return this;
            }

            Builder hasMode(boolean present) {
                hasMode = present;
                return this;
            }

            Builder hasSelect(boolean present) {
                hasSelect = present;
                return this;
            }

            Builder hasStartOrMenu(boolean present) {
                hasStartOrMenu = present;
                return this;
            }

            Builder hasGasAxis(boolean present) {
                hasGasAxis = present;
                return this;
            }

            Builder triggerDeadzone(float deadzone) {
                triggerDeadzone = deadzone;
                return this;
            }

            Facts build() {
                return new Facts(this);
            }
        }
    }
}
