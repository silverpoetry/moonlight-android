package com.limelight.binding.input;

import android.hardware.lights.Light;
import android.hardware.lights.LightState;
import android.hardware.lights.LightsManager;
import android.hardware.lights.LightsRequest;
import android.os.Build;
import android.view.InputDevice;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Adapts an Android input device's RGB lights to a controller LED target.
 */
final class AndroidControllerLedTarget {
    private AndroidControllerLedTarget() {
    }

    static ControllerLedSession.Target create(InputDevice inputDevice) {
        Objects.requireNonNull(inputDevice, "inputDevice");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return ControllerLedSession.unavailableTarget();
        }
        return new Api31Target(inputDevice.getLightsManager());
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private static final class Api31Target
            implements ControllerLedSession.Target {
        private final LightsManager lightsManager;
        private final List<Light> rgbLights = new ArrayList<>();
        private LightsManager.LightsSession lightsSession;
        private boolean closed;

        private Api31Target(LightsManager lightsManager) {
            this.lightsManager = Objects.requireNonNull(
                    lightsManager,
                    "lightsManager");
            for (Light light : lightsManager.getLights()) {
                if (light.hasRgbControl()) {
                    rgbLights.add(light);
                }
            }
        }

        @Override
        public synchronized boolean isAvailable() {
            return !closed && !rgbLights.isEmpty();
        }

        @Override
        public synchronized void applyArgb(int argb) {
            if (!isAvailable()) {
                return;
            }

            if (lightsSession == null) {
                lightsSession = lightsManager.openSession();
            }

            LightState lightState = new LightState.Builder()
                    .setColor(argb)
                    .build();
            LightsRequest.Builder request = new LightsRequest.Builder();
            for (Light light : rgbLights) {
                request.addLight(light, lightState);
            }
            lightsSession.requestLights(request.build());
        }

        @Override
        public synchronized void close() {
            if (closed) {
                return;
            }

            closed = true;
            if (lightsSession != null) {
                lightsSession.close();
                lightsSession = null;
            }
        }
    }
}
