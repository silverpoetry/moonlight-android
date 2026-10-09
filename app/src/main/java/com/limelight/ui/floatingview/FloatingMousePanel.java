package com.limelight.ui.floatingview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.limelight.binding.input.StreamInputGateway;

/** Mouse-shaped input surface; its hotspot and video coordinates share one transform. */
public final class FloatingMousePanel extends View {
    public interface PointerSink { void position(float x, float y, int width, int height); }

    private final StreamInputGateway input;
    private final View streamView;
    private final PointerSink pointerSink;
    private final Runnable onMinimize;
    private final FloatingMouseEdgePan edgePan;
    private float edgeVelocityX, edgeVelocityY;
    private long lastEdgeFrame;
    private boolean edgeFrameScheduled;
    private final android.view.Choreographer.FrameCallback edgeFrame = this::panAtEdge;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Matrix sourceMatrix = new Matrix();
    private final Matrix targetMatrix = new Matrix();
    private final Matrix inverseMatrix = new Matrix();
    private final float[] point = new float[2];
    private final ViewTreeObserver.OnPreDrawListener geometryListener = () -> {
        updatePointer();
        return true;
    };
    private float sentX = Float.NaN, sentY = Float.NaN;
    private int sentWidth, sentHeight;
    private float downX, downY, lastX, lastY, scrollRemainder;
    private int heldButton, region, pointerId = -1, feedbackRegion = -1, scrollDirection;
    private boolean moved;
    private final Runnable clearFeedback = () -> {
        feedbackRegion = -1;
        scrollDirection = 0;
        invalidate();
    };
    private final Runnable longPress = () -> {
        if (pointerId >= 0 && region >= 1 && region <= 3) pressButton(region);
    };

    public FloatingMousePanel(Context context, StreamInputGateway input, View streamView,
            PointerSink pointerSink, Runnable onViewportChanged, Runnable onMinimize) {
        super(context);
        this.input = input;
        this.streamView = streamView;
        this.pointerSink = pointerSink;
        this.onMinimize = onMinimize;
        this.edgePan = new FloatingMouseEdgePan(this, streamView, onViewportChanged);
        setContentDescription(context.getString(com.limelight.R.string.floating_mouse_controls));
        setClickable(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    private float unit() { return getWidth() / 156f; }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getMode(widthSpec) == MeasureSpec.UNSPECIFIED
                ? dp(156) : MeasureSpec.getSize(widthSpec);
        setMeasuredDimension(width, Math.round(width * 208f / 156f));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.scale(unit(), unit());
        RectF body = new RectF(8, 40, 140, 198);
        path.reset();
        path.addRoundRect(body, 54, 54, Path.Direction.CW);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xB3DCE9FF);
        canvas.drawPath(path, paint);
        canvas.save();
        canvas.clipPath(path);
        paint.setColor(0xAA83B4F7);
        int active = pointerId >= 0 ? region : feedbackRegion;
        if (active == 1) canvas.drawRect(8, 40, 74, 119, paint);
        if (active == 3) canvas.drawRect(74, 40, 140, 119, paint);
        if (active == 0 && moved) canvas.drawRect(8, 119, 140, 198, paint);
        canvas.restore();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.8f);
        paint.setColor(moved && region != 4 ? 0xFF377BCE : 0xFF71859E);
        canvas.drawPath(path, paint);
        canvas.drawLine(8, 119, 140, 119, paint);
        canvas.drawLine(74, 40, 74, 119, paint);
        drawControl(canvas, new RectF(58, 54, 90, 102), active == 4);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(scrollDirection < 0 ? 0xFF246FC7 : 0xFF71859E);
        canvas.drawLine(69, 66, 74, 61, paint);
        canvas.drawLine(74, 61, 79, 66, paint);
        paint.setColor(scrollDirection > 0 ? 0xFF246FC7 : 0xFF71859E);
        canvas.drawLine(69, 90, 74, 95, paint);
        canvas.drawLine(74, 95, 79, 90, paint);
        canvas.drawLine(74, 74 + scrollDirection * 3, 74, 82 + scrollDirection * 3, paint);
        drawControl(canvas, new RectF(56, 105, 92, 139), active == 2);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0xFF71859E);
        canvas.drawLine(66, 118, 82, 118, paint);
        canvas.drawLine(66, 125, 82, 125, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(active == 5 ? 0xFF83B4F7 : 0xCCE2ECFF);
        canvas.drawCircle(132, 22, 17, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0xFF71859E);
        canvas.drawLine(127, 17, 137, 27, paint);
        canvas.drawLine(137, 17, 127, 27, paint);
        // Grip dots distinguish the movement area from the mouse buttons.
        paint.setStyle(Paint.Style.FILL);
        for (int y = 157; y <= 165; y += 8)
            for (int x = 66; x <= 82; x += 8) canvas.drawCircle(x, y, 1.4f, paint);
        canvas.restore();
    }

    private void drawControl(Canvas canvas, RectF bounds, boolean active) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(active ? 0xFF83B4F7 : 0xFFE0EBFB);
        canvas.drawRoundRect(bounds, 10, 10, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0xFF71859E);
        canvas.drawRoundRect(bounds, 10, 10, paint);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                removeCallbacks(clearFeedback);
                feedbackRegion = -1;
                pointerId = event.getPointerId(0);
                downX = lastX = event.getRawX();
                downY = lastY = event.getRawY();
                moved = false;
                scrollRemainder = 0;
                region = hitRegion(event.getX() / unit(), event.getY() / unit());
                updatePointer();
                if (region >= 1 && region <= 3)
                    postDelayed(longPress, ViewConfiguration.getLongPressTimeout());
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (pointerId < 0 || event.findPointerIndex(pointerId) != 0) return true;
                float x = event.getRawX(), y = event.getRawY();
                if (!moved && Math.hypot(x - downX, y - downY)
                        > ViewConfiguration.get(getContext()).getScaledTouchSlop()) {
                    moved = true;
                    removeCallbacks(longPress);
                    if (region >= 1 && region <= 3) pressButton(region);
                }
                if (moved && region == 4) {
                    scrollRemainder += y - lastY;
                    while (Math.abs(scrollRemainder) >= dp(10)) {
                        scroll(scrollRemainder < 0);
                        scrollRemainder += scrollRemainder < 0 ? dp(10) : -dp(10);
                    }
                } else if (moved && region >= 0 && region <= 3) {
                    movePanel(x - lastX, y - lastY);
                }
                lastX = x;
                lastY = y;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                removeCallbacks(longPress);
                if (!moved && region == 5) {
                    releaseButtons();
                    onMinimize.run();
                } else {
                    if (!moved && region >= 1 && region <= 3 && heldButton == 0) pressButton(region);
                    if (!moved && region == 4) scroll(event.getY() / unit() < 78);
                    feedbackRegion = region;
                    releaseButtons();
                    postDelayed(clearFeedback, 140);
                }
                performClick();
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerId(event.getActionIndex()) == pointerId) releaseButtons();
                return true;
            case MotionEvent.ACTION_CANCEL:
                releaseButtons();
                return true;
            default: return true;
        }
    }

    private int hitRegion(float x, float y) {
        if (Math.hypot(x - 132, y - 22) < 24) return 5;
        if (x >= 50 && x <= 98 && y >= 50 && y < 104) return 4;
        if (x >= 50 && x <= 98 && y >= 104 && y <= 143) return 2;
        if (y >= 40 && y < 119) return x < 74 ? 1 : 3;
        return 0;
    }

    private void scroll(boolean up) {
        input.sendHighResolutionScroll(up);
        scrollDirection = up ? -1 : 1;
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        removeCallbacks(clearFeedback);
        postDelayed(clearFeedback, 140);
        invalidate();
    }

    private void pressButton(int button) {
        if (heldButton != 0) return;
        updatePointer();
        heldButton = button;
        input.sendMouseButton(button, true);
        performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
        invalidate();
    }

    @Override public boolean performClick() { return super.performClick(); }

    private void movePanel(float dx, float dy) {
        ViewGroup parent = (ViewGroup) getParent();
        if (parent == null) return;
        setX(Math.max(0, Math.min(parent.getWidth() - getWidth(), getX() + dx)));
        setY(Math.max(0, Math.min(parent.getHeight() - getHeight(), getY() + dy)));
        float zone = dp(40);
        float travelX = Math.max(0, parent.getWidth() - getWidth());
        float travelY = Math.max(0, parent.getHeight() - getHeight());
        edgeVelocityX = FloatingMousePanGeometry.directionalVelocity(
                edgeVelocity(getX(), travelX, zone), dx, edgeVelocityX);
        edgeVelocityY = FloatingMousePanGeometry.directionalVelocity(
                edgeVelocity(getY(), travelY, zone), dy, edgeVelocityY);
        if (!edgeFrameScheduled && (edgeVelocityX != 0 || edgeVelocityY != 0)) {
            lastEdgeFrame = 0;
            edgeFrameScheduled = true;
            android.view.Choreographer.getInstance().postFrameCallback(edgeFrame);
        }
        updatePointer();
    }

    private float edgeVelocity(float position, float travel, float zone) {
        return FloatingMousePanGeometry.edgeVelocity(position, travel, zone, dp(900));
    }

    private void panAtEdge(long frameTimeNanos) {
        edgeFrameScheduled = false;
        if (pointerId < 0 || !moved || region > 3) return;
        if (edgeVelocityX == 0 && edgeVelocityY == 0) return;
        if (lastEdgeFrame == 0) {
            lastEdgeFrame = frameTimeNanos;
            edgeFrameScheduled = true;
            android.view.Choreographer.getInstance().postFrameCallback(edgeFrame);
            return;
        }
        float seconds = Math.min(0.05f, (frameTimeNanos - lastEdgeFrame) / 1_000_000_000f);
        lastEdgeFrame = frameTimeNanos;
        if (edgePan.pan(edgeVelocityX * seconds, edgeVelocityY * seconds, 8 * unit(), 40 * unit())) {
            updatePointer();
            edgeFrameScheduled = true;
            android.view.Choreographer.getInstance().postFrameCallback(edgeFrame);
        }
    }

    private void updatePointer() {
        int width = streamView.getWidth(), height = streamView.getHeight();
        if (getWidth() == 0 || width == 0 || height == 0 || getParent() == null) return;
        sourceMatrix.reset();
        targetMatrix.reset();
        toRoot(this, sourceMatrix);
        toRoot(streamView, targetMatrix);
        if (!targetMatrix.invert(inverseMatrix)) return;
        point[0] = 8 * unit();
        point[1] = 40 * unit();
        sourceMatrix.mapPoints(point);
        inverseMatrix.mapPoints(point);
        float x = Math.max(0, Math.min(width - 1, point[0]));
        float y = Math.max(0, Math.min(height - 1, point[1]));
        if (x != sentX || y != sentY || width != sentWidth || height != sentHeight) {
            pointerSink.position(x, y, width, height);
            sentX = x; sentY = y; sentWidth = width; sentHeight = height;
        }
    }

    // Compose ancestor transforms, including scroll, without relying on API 29 global matrices.
    static void toRoot(View view, Matrix matrix) {
        if (view.getParent() instanceof View) {
            View parent = (View) view.getParent();
            toRoot(parent, matrix);
            matrix.preTranslate(-parent.getScrollX(), -parent.getScrollY());
        }
        matrix.preTranslate(view.getLeft(), view.getTop());
        matrix.preConcat(view.getMatrix());
    }

    public void releaseButtons() {
        android.view.Choreographer.getInstance().removeFrameCallback(edgeFrame);
        edgeFrameScheduled = false;
        lastEdgeFrame = 0;
        edgeVelocityX = edgeVelocityY = 0;
        removeCallbacks(longPress);
        pointerId = -1;
        moved = false;
        if (heldButton != 0) input.sendMouseButton(heldButton, false);
        heldButton = 0;
        invalidate();
    }

    public void release() {
        releaseButtons();
        edgePan.restore();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnPreDrawListener(geometryListener);
    }

    @Override protected void onDetachedFromWindow() {
        getViewTreeObserver().removeOnPreDrawListener(geometryListener);
        removeCallbacks(clearFeedback);
        release();
        super.onDetachedFromWindow();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
