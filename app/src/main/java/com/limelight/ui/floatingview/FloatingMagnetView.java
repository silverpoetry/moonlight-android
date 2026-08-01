package com.limelight.ui.floatingview;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.limelight.settings.ui.StreamUiSettings;

import java.util.Objects;

/**
 * Draggable stream control that docks to the nearest horizontal edge.
 */
public class FloatingMagnetView extends FrameLayout {
    public interface PositionListener {
        void onPositionSettled(
                float x,
                float y,
                boolean nearestLeft);
    }

    public static final int EDGE_MARGIN_PX = 13;
    private static final int CLICK_TIME_THRESHOLD_MS = 150;
    private static final long IDLE_COLLAPSE_DELAY_MS = 2_000;

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());
    private final MoveAnimator moveAnimator = new MoveAnimator();
    private final Runnable delayedCollapse = this::collapseAtEdge;

    private float originalRawX;
    private float originalRawY;
    private float originalX;
    private float originalY;
    private long lastTouchDownTime;
    private int availableHorizontalTravel;
    private int parentHeight;
    private boolean nearestLeft = true;
    private boolean collapsed;
    private boolean initialPositionApplied;
    private boolean hasRememberedPosition;
    private float rememberedY;
    private float portraitY;
    private PositionListener positionListener;

    public FloatingMagnetView(Context context) {
        this(context, null);
    }

    public FloatingMagnetView(
            Context context,
            AttributeSet attributes) {
        this(context, attributes, 0);
    }

    public FloatingMagnetView(
            Context context,
            AttributeSet attributes,
            int defaultStyleAttribute) {
        super(context, attributes, defaultStyleAttribute);
        setClickable(true);
        scheduleCollapse();
    }

    public void configurePosition(
            StreamUiSettings settings,
            PositionListener listener) {
        Objects.requireNonNull(settings, "settings");
        positionListener = Objects.requireNonNull(
                listener,
                "listener");
        if (settings.hasRememberedFloatingPosition()) {
            hasRememberedPosition = true;
            rememberedY = settings.getFloatingPositionY();
            nearestLeft =
                    settings.isFloatingPositionNearestLeft();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event == null) {
            return false;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                updateParentBounds();
                moveAnimator.stop();
                cancelCollapse();
                captureTouchOrigin(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                updateViewPosition(event);
                return true;
            case MotionEvent.ACTION_UP:
                finishGesture();
                if (isClickEvent()) {
                    performClick();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                finishGesture();
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void finishGesture() {
        clearPortraitY();
        moveToEdge();
        scheduleCollapse();
    }

    private boolean isClickEvent() {
        return android.os.SystemClock.uptimeMillis() -
                lastTouchDownTime < CLICK_TIME_THRESHOLD_MS;
    }

    private void updateViewPosition(MotionEvent event) {
        setX(clamp(
                originalX + event.getRawX() - originalRawX,
                0,
                availableHorizontalTravel));
        float destinationY =
                originalY + event.getRawY() - originalRawY;
        float maximumY = Math.max(0, parentHeight - getHeight());
        setY(Math.min(
                Math.max(0, destinationY),
                maximumY));
    }

    private void captureTouchOrigin(MotionEvent event) {
        originalX = getX();
        originalY = getY();
        originalRawX = event.getRawX();
        originalRawY = event.getRawY();
        lastTouchDownTime =
                android.os.SystemClock.uptimeMillis();
    }

    private void updateParentBounds() {
        ViewGroup parent = (ViewGroup) getParent();
        if (parent == null) {
            return;
        }
        availableHorizontalTravel =
                Math.max(0, parent.getWidth() - getWidth());
        parentHeight = parent.getHeight();
    }

    public void moveToEdge() {
        moveToEdge(isNearestLeft(), false);
    }

    public void moveToEdge(
            boolean dockLeft,
            boolean isLandscape) {
        float destinationX = dockLeft
                ? edgeX(true)
                : edgeX(false);
        float destinationY = getY();
        if (!isLandscape && portraitY != 0) {
            destinationY = portraitY;
            clearPortraitY();
        }
        moveAnimator.start(
                destinationX,
                Math.min(
                        Math.max(0, destinationY),
                        Math.max(0, parentHeight - getHeight())));
    }

    private boolean isNearestLeft() {
        nearestLeft = getX() <
                (float) availableHorizontalTravel / 2;
        return nearestLeft;
    }

    private void scheduleCollapse() {
        mainHandler.removeCallbacks(delayedCollapse);
        mainHandler.postDelayed(
                delayedCollapse,
                IDLE_COLLAPSE_DELAY_MS);
    }

    private void cancelCollapse() {
        mainHandler.removeCallbacks(delayedCollapse);
        animate().cancel();
        if (collapsed) {
            setX(edgeX(nearestLeft));
            collapsed = false;
        }
        animate().alpha(1f).setDuration(100).start();
    }

    private void collapseAtEdge() {
        if (!isAttachedToWindow() || getVisibility() != VISIBLE) {
            return;
        }
        updateParentBounds();
        moveAnimator.stop();
        setX(edgeX(nearestLeft));
        animate().alpha(0.35f).setDuration(100).start();
        float collapsedX = nearestLeft
                ? (float) -getWidth() / 2
                : availableHorizontalTravel +
                        (float) getWidth() / 2;
        collapsed = true;
        animate().x(collapsedX)
                .setDuration(100)
                .start();
        PositionListener listener = positionListener;
        if (listener != null) {
            listener.onPositionSettled(
                    getX(),
                    getY(),
                    nearestLeft);
        }
    }

    @Override
    protected void onConfigurationChanged(
            Configuration newConfiguration) {
        super.onConfigurationChanged(newConfiguration);
        ViewGroup parent = (ViewGroup) getParent();
        if (parent == null) {
            return;
        }
        boolean isLandscape =
                newConfiguration.orientation ==
                        Configuration.ORIENTATION_LANDSCAPE;
        if (isLandscape) {
            portraitY = getY();
        }
        parent.post(() -> {
            updateParentBounds();
            collapsed = false;
            moveToEdge(nearestLeft, isLandscape);
        });
    }

    @Override
    protected void onLayout(
            boolean changed,
            int left,
            int top,
            int right,
            int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!initialPositionApplied) {
            applyInitialPosition();
        }
    }

    private void clearPortraitY() {
        portraitY = 0;
    }

    @Override
    protected void onDetachedFromWindow() {
        release();
        super.onDetachedFromWindow();
    }

    void release() {
        mainHandler.removeCallbacks(delayedCollapse);
        animate().cancel();
        moveAnimator.stop();
        positionListener = null;
    }

    private void applyInitialPosition() {
        updateParentBounds();
        if (getWidth() == 0 || parentHeight == 0) {
            return;
        }

        if (hasRememberedPosition) {
            setY(clamp(
                    rememberedY,
                    0,
                    Math.max(0, parentHeight - getHeight())));
        }
        else {
            setY(clamp(
                    getY(),
                    0,
                    Math.max(0, parentHeight - getHeight())));
        }
        setX(edgeX(nearestLeft));
        initialPositionApplied = true;
    }

    private float edgeX(boolean dockLeft) {
        return dockLeft
                ? Math.min(EDGE_MARGIN_PX, availableHorizontalTravel)
                : Math.max(
                        0,
                        availableHorizontalTravel - EDGE_MARGIN_PX);
    }

    private static float clamp(
            float value,
            float minimum,
            float maximum) {
        return Math.min(Math.max(minimum, value), maximum);
    }

    private final class MoveAnimator implements Runnable {
        private static final long DURATION_MS = 400;

        private float startingX;
        private float startingY;
        private float destinationX;
        private float destinationY;
        private long startingTime;

        private void start(float x, float y) {
            stop();
            collapsed = false;
            startingX = getX();
            startingY = getY();
            destinationX = x;
            destinationY = y;
            startingTime = android.os.SystemClock.uptimeMillis();
            postOnAnimation(this);
        }

        @Override
        public void run() {
            if (!isAttachedToWindow()) {
                return;
            }
            float progress = Math.min(
                    1f,
                    (android.os.SystemClock.uptimeMillis() -
                            startingTime) / (float) DURATION_MS);
            setX(startingX +
                    (destinationX - startingX) * progress);
            setY(startingY +
                    (destinationY - startingY) * progress);
            if (progress < 1f) {
                postOnAnimation(this);
            }
        }

        private void stop() {
            removeCallbacks(this);
        }
    }
}
