package com.limelight.ui.floatingview;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.MainThread;

import com.limelight.settings.ui.StreamUiSettings;
import com.limelight.settings.ui.StreamUiSettingsState;

import java.util.Objects;

/**
 * Lifecycle owner for the optional in-stream floating control.
 */
public final class StreamFloatingControlController {
    public interface PositionSink {
        void save(float x, float y, boolean nearestLeft);
    }

    public interface ActionSink {
        void perform(StreamUiSettings.FloatingAction action);
    }

    private final Context context;
    private final ViewGroup parent;
    private final StreamUiSettingsState settingsState;
    private final PositionSink positionSink;
    private final ActionSink actionSink;

    private FloatingControlView controlView;
    private boolean destroyed;

    @MainThread
    public StreamFloatingControlController(
            Context context,
            ViewGroup parent,
            StreamUiSettingsState settingsState,
            PositionSink positionSink,
            ActionSink actionSink) {
        this.context = Objects.requireNonNull(context, "context");
        this.parent = Objects.requireNonNull(parent, "parent");
        this.settingsState = Objects.requireNonNull(
                settingsState,
                "settingsState");
        this.positionSink = Objects.requireNonNull(
                positionSink,
                "positionSink");
        this.actionSink = Objects.requireNonNull(
                actionSink,
                "actionSink");
    }

    @MainThread
    public void applyEnabled(boolean enabled) {
        if (enabled) {
            show();
        }
        else {
            hide();
        }
    }

    @MainThread
    public void toggleVisibility() {
        if (destroyed) {
            return;
        }
        if (controlView != null &&
                controlView.getVisibility() == View.VISIBLE) {
            hide();
        }
        else {
            show();
        }
    }

    @MainThread
    public void show() {
        if (destroyed) {
            return;
        }
        ensureControlView().setVisibility(View.VISIBLE);
    }

    @MainThread
    public void hide() {
        if (controlView != null) {
            controlView.setVisibility(View.GONE);
        }
    }

    @MainThread
    public boolean isVisible() {
        return controlView != null &&
                controlView.getVisibility() == View.VISIBLE;
    }

    @MainThread
    public void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        if (controlView == null) {
            return;
        }
        controlView.setOnClickListener(null);
        controlView.release();
        if (controlView.getParent() == parent) {
            parent.removeView(controlView);
        }
        controlView = null;
    }

    private FloatingControlView ensureControlView() {
        if (controlView != null) {
            return controlView;
        }

        FloatingControlView created =
                new FloatingControlView(context);
        created.configurePosition(
                settingsState.get(),
                this::onPositionSettled);
        created.setLayoutParams(
                FloatingControlView
                        .createDefaultLayoutParams());
        created.setOnClickListener(view -> actionSink.perform(
                settingsState.get().getFloatingAction()));
        parent.addView(created);
        controlView = created;
        return created;
    }

    private void onPositionSettled(
            float x,
            float y,
            boolean nearestLeft) {
        if (!destroyed && settingsState.get()
                .shouldRememberFloatingPosition()) {
            positionSink.save(x, y, nearestLeft);
        }
    }
}
