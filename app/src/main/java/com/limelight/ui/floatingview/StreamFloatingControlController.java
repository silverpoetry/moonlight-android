package com.limelight.ui.floatingview;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.MainThread;

import com.limelight.settings.ui.StreamUiSettings;
import com.limelight.binding.input.StreamInputGateway;
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
    private FloatingMousePanel mousePanel;
    private Runnable mouseClosed;

    public boolean isMouseControlsVisible() { return mousePanel != null; }
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
        if (isVisible()) {
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
        if (mousePanel != null) {
            return;
        }
        FloatingControlView view = ensureControlView();
        view.setVisibility(View.VISIBLE);
    }

    @MainThread
    public void hide() {
        closeMousePanel();
        if (controlView != null) {
            controlView.setVisibility(View.GONE);
        }
    }

    @MainThread
    public boolean isVisible() {
        return mousePanel != null || controlView != null &&
                controlView.getVisibility() == View.VISIBLE;
    }

    @MainThread
    public void showMouseControls(StreamInputGateway input, View streamView,
            FloatingMousePanel.PointerSink pointerSink, Runnable onViewportChanged, Runnable onClosed) {
        if (destroyed || mousePanel != null) {
            return;
        }
        float density = context.getResources().getDisplayMetrics().density;
        FloatingControlView ball = ensureControlView();
        ball.suspendDocking();
        final float ballX = ball.getX();
        final float ballY = ball.getY();
        int panelWidth = Math.min((int) (156 * density), Math.max(1, parent.getWidth() - (int) (16 * density)));
        int panelHeight = Math.round(panelWidth * 208f / 156f);
        final float panelX = Math.max(0, Math.min(parent.getWidth() - panelWidth,
                ballX + ball.getWidth() / 2f - panelWidth / 2f));
        final float panelY = Math.max(0, Math.min(parent.getHeight() - panelHeight,
                ballY + ball.getHeight() / 2f - panelHeight / 2f));
        FloatingMousePanel panel = new FloatingMousePanel(context, input, streamView, pointerSink, onViewportChanged, () -> {
            float restoreX = ballX;
            float restoreY = ballY;
            if (controlView != null && mousePanel != null) {
                restoreX += mousePanel.getX() - panelX;
                restoreY += mousePanel.getY() - panelY;
            }
            closeMousePanel();
            show();
            ball.resumeDocking(restoreX, restoreY);
        });
        mouseClosed = onClosed;
        android.widget.FrameLayout.LayoutParams parameters = new android.widget.FrameLayout.LayoutParams(
                panelWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        parent.addView(panel, parameters);
        panel.setX(panelX);
        panel.setY(panelY);
        if (controlView != null) {
            controlView.setVisibility(View.GONE);
        }
        mousePanel = panel;
    }

    private void closeMousePanel() {
        if (mousePanel != null) {
            mousePanel.release();
            parent.removeView(mousePanel);
            mousePanel = null;
            if (mouseClosed != null) {
                mouseClosed.run();
                mouseClosed = null;
            }
        }
    }

    public void cancelActiveInput() {
        if (mousePanel != null) {
            mousePanel.releaseButtons();
        }
    }

    @MainThread
    public void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        closeMousePanel();
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
