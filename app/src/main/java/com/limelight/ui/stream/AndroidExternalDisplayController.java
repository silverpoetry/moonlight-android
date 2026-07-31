package com.limelight.ui.stream;

import android.app.Activity;
import android.app.Presentation;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.view.Display;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.MainThread;

import com.limelight.LimeLog;
import com.limelight.R;
import com.limelight.ui.StreamView;

import java.util.Objects;

/** Owns the external-display Presentation and StreamView parent transfer. */
public final class AndroidExternalDisplayController {
    private final Activity activity;
    private final StreamView streamView;
    private final ViewGroup originalParent;
    private final ViewGroup.LayoutParams originalLayoutParams;

    private StreamPresentation presentation;
    private boolean destroyed;

    public AndroidExternalDisplayController(
            Activity activity,
            StreamView streamView,
            ViewGroup originalParent) {
        this.activity = Objects.requireNonNull(activity, "activity");
        this.streamView = Objects.requireNonNull(streamView, "streamView");
        this.originalParent = Objects.requireNonNull(
                originalParent,
                "originalParent");
        originalLayoutParams = streamView.getLayoutParams();
    }

    @MainThread
    public boolean showOnFirstSecondaryDisplay() {
        if (destroyed || presentation != null) {
            return false;
        }
        DisplayManager displayManager =
                (DisplayManager) activity.getSystemService(
                        Activity.DISPLAY_SERVICE);
        if (displayManager == null) {
            return false;
        }
        Display[] displays = displayManager.getDisplays();
        int[] displayIds = new int[displays.length];
        for (int index = 0; index < displays.length; index++) {
            displayIds[index] = displays[index].getDisplayId();
        }
        int selectedDisplayId =
                ExternalDisplaySelectionPolicy.select(
                        Display.DEFAULT_DISPLAY,
                        displayIds);
        if (selectedDisplayId ==
                ExternalDisplaySelectionPolicy.NO_DISPLAY) {
            return false;
        }
        Display selectedDisplay =
                displayManager.getDisplay(selectedDisplayId);
        if (selectedDisplay == null) {
            return false;
        }

        StreamPresentation candidate =
                new StreamPresentation(activity, selectedDisplay);
        candidate.setOnDismissListener(
                ignored -> onPresentationDismissed(candidate));
        try {
            candidate.show();
            ViewGroup currentParent =
                    streamView.getParent() instanceof ViewGroup
                            ? (ViewGroup) streamView.getParent()
                            : null;
            if (currentParent != null) {
                currentParent.removeView(streamView);
            }
            candidate.attach(streamView);
            presentation = candidate;
            return true;
        }
        catch (RuntimeException exception) {
            candidate.dismiss();
            reattachToOriginalParent();
            LimeLog.warning(
                    "Unable to show stream on external display: " +
                            exception.getClass().getSimpleName());
            return false;
        }
    }

    @MainThread
    public void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        if (presentation != null) {
            presentation.dismiss();
            presentation = null;
        }
    }

    private void reattachToOriginalParent() {
        if (streamView.getParent() == null) {
            if (originalLayoutParams != null) {
                originalParent.addView(
                        streamView,
                        originalLayoutParams);
            }
            else {
                originalParent.addView(streamView);
            }
        }
    }

    private void onPresentationDismissed(
            StreamPresentation dismissedPresentation) {
        if (presentation != dismissedPresentation) {
            return;
        }
        presentation = null;
        if (!destroyed) {
            reattachToOriginalParent();
        }
    }

    private static final class StreamPresentation
            extends Presentation {
        private FrameLayout content;

        private StreamPresentation(
                Activity activity,
                Display display) {
            super(activity, display, R.style.SecondaryDisplayTheme);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            content = new FrameLayout(getContext());
            content.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            setContentView(content);
        }

        private void attach(StreamView streamView) {
            if (content == null) {
                throw new IllegalStateException(
                        "Presentation content is not ready");
            }
            content.addView(streamView);
        }

        @Override
        protected void onStop() {
            super.onStop();
            if (content != null) {
                content.removeAllViews();
            }
        }
    }
}
