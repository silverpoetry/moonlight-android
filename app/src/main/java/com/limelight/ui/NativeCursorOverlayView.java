package com.limelight.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

import com.limelight.utils.ViewCoordinateMapper;

public class NativeCursorOverlayView extends View {
    public static final int CURSOR_FORMAT_BGRA = 1;
    private static final int DEFAULT_CURSOR_SIZE = 32;
    private static final int DEFAULT_CURSOR_HOTSPOT = 2;
    private static final Bitmap DEFAULT_CURSOR_BITMAP = createDefaultCursorBitmap();

    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final float[] mappedPosition = new float[2];
    private final float[] mappedBasis = new float[4];
    private final Matrix targetInverse = new Matrix();

    private Bitmap cursorBitmap;
    private Bitmap scaledCursorBitmap;
    private boolean visible;
    private boolean hasNativeCursorState;
    private boolean hasPosition;
    private float x;
    private float y;
    private float scaleX = 1f;
    private float scaleY = 1f;
    private int hotspotX;
    private int hotspotY;
    private int scaledHotspotX;
    private int scaledHotspotY;
    private int shapeId;

    public NativeCursorOverlayView(Context context) {
        super(context);
        setWillNotDraw(false);
        setFocusable(false);
        setClickable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public boolean hasCursorPosition() {
        return hasPosition;
    }

    public void setCursorScale(float scaleX, float scaleY) {
        if (scaleX <= 0 || scaleY <= 0) {
            return;
        }
        if (Float.compare(this.scaleX, scaleX) == 0 &&
                Float.compare(this.scaleY, scaleY) == 0) {
            return;
        }

        invalidateCursorBounds();
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        updateScaledCursorBitmap();
        invalidateCursorBounds();
    }

    public void updateCursor(boolean visible, boolean shapeChanged, int format,
                             int width, int height,
                             int hotspotX, int hotspotY, int shapeId, byte[] imageData) {
        boolean oldVisible = this.visible;
        hasNativeCursorState = true;
        this.visible = visible;

        if (shapeChanged && format == CURSOR_FORMAT_BGRA && width > 0 && height > 0 &&
                imageData != null && imageData.length >= width * height * 4) {
            invalidateCursorBounds();
            this.hotspotX = hotspotX;
            this.hotspotY = hotspotY;
            this.shapeId = shapeId;
            cursorBitmap = createBitmapFromBgra(imageData, width, height);
            updateScaledCursorBitmap();
            invalidateCursorBounds();
        }
        else if (visible && cursorBitmap == null) {
            // Native cursor streaming separates visibility from the cursor image. Show a
            // deterministic local arrow while the initial shape is unavailable, then replace
            // it atomically as soon as the host provides a valid BGRA image.
            invalidateCursorBounds();
            hotspotX = DEFAULT_CURSOR_HOTSPOT;
            hotspotY = DEFAULT_CURSOR_HOTSPOT;
            cursorBitmap = DEFAULT_CURSOR_BITMAP;
            updateScaledCursorBitmap();
            invalidateCursorBounds();
        }

        if (oldVisible != visible) {
            invalidate();
        }
    }

    /**
     * Presents a local arrow only until the host has supplied an authoritative cursor state.
     *
     * <p>Windows' secure desktop can prevent Sunshine from probing any cursor state at all.
     * That differs from a host {@code visible=false} update, which remains authoritative and
     * must keep the overlay hidden.</p>
     */
    public void showFallbackCursorIfNativeStateUnavailable() {
        if (hasNativeCursorState) {
            return;
        }

        boolean oldVisible = visible;
        if (cursorBitmap == null) {
            invalidateCursorBounds();
            hotspotX = DEFAULT_CURSOR_HOTSPOT;
            hotspotY = DEFAULT_CURSOR_HOTSPOT;
            cursorBitmap = DEFAULT_CURSOR_BITMAP;
            updateScaledCursorBitmap();
            invalidateCursorBounds();
        }
        visible = true;
        if (!oldVisible) {
            invalidate();
        }
    }

    private void setCursorPosition(float x, float y) {
        invalidateCursorBounds();
        this.x = x;
        this.y = y;
        this.hasPosition = true;
        invalidateCursorBounds();
    }

    public boolean setCursorScaleFromStream(
            View source,
            int encodedWidth,
            int encodedHeight,
            int captureScaleX,
            int captureScaleY) {
        if (source.getWidth() <= 0 || source.getHeight() <= 0) {
            return false;
        }

        mappedBasis[0] =
                StreamViewportGeometry.mapScaledDimension(
                        captureScaleX,
                        encodedWidth,
                        source.getWidth());
        mappedBasis[1] = 0f;
        mappedBasis[2] = 0f;
        mappedBasis[3] =
                StreamViewportGeometry.mapScaledDimension(
                        captureScaleY,
                        encodedHeight,
                        source.getHeight());
        if (!ViewCoordinateMapper.mapBasisBetweenSiblings(
                source,
                this,
                mappedBasis,
                targetInverse)) {
            return false;
        }

        float mappedScaleX = (float) Math.hypot(
                mappedBasis[0],
                mappedBasis[1]);
        float mappedScaleY = (float) Math.hypot(
                mappedBasis[2],
                mappedBasis[3]);
        setCursorScale(mappedScaleX, mappedScaleY);
        return true;
    }

    public boolean setCursorPositionFromView(View source, float sourceX,
                                             float sourceY) {
        mappedPosition[0] = sourceX;
        mappedPosition[1] = sourceY;
        if (!ViewCoordinateMapper.mapPointBetweenSiblings(
                source, this, mappedPosition, targetInverse)) {
            return false;
        }

        setCursorPosition(mappedPosition[0], mappedPosition[1]);
        return true;
    }

    public boolean setCursorPositionFromReference(View source,
                                                  int referenceX,
                                                  int referenceY,
                                                  int referenceWidth,
                                                  int referenceHeight) {
        if (referenceWidth <= 1 || referenceHeight <= 1 ||
                source.getWidth() <= 0 || source.getHeight() <= 0) {
            return false;
        }

        float sourceX = StreamViewportGeometry.mapPixelCoordinate(
                referenceX, referenceWidth, source.getWidth());
        float sourceY = StreamViewportGeometry.mapPixelCoordinate(
                referenceY, referenceHeight, source.getHeight());
        return setCursorPositionFromView(source, sourceX, sourceY);
    }

    public void clearCursor() {
        visible = false;
        hasNativeCursorState = false;
        hasPosition = false;
        cursorBitmap = null;
        scaledCursorBitmap = null;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!visible || !hasPosition || scaledCursorBitmap == null) {
            return;
        }

        canvas.drawBitmap(scaledCursorBitmap, x - scaledHotspotX, y - scaledHotspotY, paint);
    }

    private void invalidateCursorBounds() {
        if (scaledCursorBitmap == null || !hasPosition) {
            invalidate();
            return;
        }

        RectF bounds = getCursorBounds();
        postInvalidateOnAnimation(
                (int) bounds.left - 2,
                (int) bounds.top - 2,
                (int) bounds.right + 2,
                (int) bounds.bottom + 2);
    }

    private RectF getCursorBounds() {
        float left = x - scaledHotspotX;
        float top = y - scaledHotspotY;
        float right = left + scaledCursorBitmap.getWidth();
        float bottom = top + scaledCursorBitmap.getHeight();
        return new RectF(left, top, right, bottom);
    }

    private void updateScaledCursorBitmap() {
        if (cursorBitmap == null) {
            scaledCursorBitmap = null;
            return;
        }

        int scaledWidth = Math.max(1, Math.round(cursorBitmap.getWidth() * scaleX));
        int scaledHeight = Math.max(1, Math.round(cursorBitmap.getHeight() * scaleY));
        scaledHotspotX = Math.max(0, Math.round(hotspotX * scaleX));
        scaledHotspotY = Math.max(0, Math.round(hotspotY * scaleY));

        if (scaledWidth == cursorBitmap.getWidth() && scaledHeight == cursorBitmap.getHeight()) {
            scaledCursorBitmap = cursorBitmap;
        }
        else {
            scaledCursorBitmap = scaleCursorBitmapMaxAlpha(cursorBitmap, scaledWidth, scaledHeight);
        }
    }

    private static Bitmap createBitmapFromBgra(byte[] bgra, int width, int height) {
        int[] argb = new int[width * height];
        boolean hasAlpha = false;
        boolean hasRgb = false;

        for (int p = 0; p < argb.length * 4; p += 4) {
            int b = bgra[p] & 0xFF;
            int g = bgra[p + 1] & 0xFF;
            int r = bgra[p + 2] & 0xFF;
            int a = bgra[p + 3] & 0xFF;

            hasAlpha |= a != 0;
            hasRgb |= r != 0 || g != 0 || b != 0;
        }

        // Some monochrome host cursors (notably the Windows I-beam) can arrive
        // with color data but a fully transparent alpha channel. Treat those
        // non-empty mask pixels as a black cursor so they remain visible.
        boolean recoverMissingAlpha = !hasAlpha && hasRgb;

        for (int i = 0, p = 0; i < argb.length; i++, p += 4) {
            int b = bgra[p] & 0xFF;
            int g = bgra[p + 1] & 0xFF;
            int r = bgra[p + 2] & 0xFF;
            int a = bgra[p + 3] & 0xFF;

            if (recoverMissingAlpha && (r != 0 || g != 0 || b != 0)) {
                a = 0xFF;
                r = 0;
                g = 0;
                b = 0;
            }

            argb[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }

        return Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888);
    }

    private static Bitmap createDefaultCursorBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(
                DEFAULT_CURSOR_SIZE,
                DEFAULT_CURSOR_SIZE,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Path path = new Path();

        path.moveTo(2f, 1f);
        path.lineTo(2f, 29f);
        path.lineTo(9f, 22f);
        path.lineTo(14f, 31f);
        path.lineTo(20f, 27f);
        path.lineTo(15f, 18f);
        path.lineTo(28f, 18f);
        path.close();
        paint.setColor(0xFF101010);
        canvas.drawPath(path, paint);

        path.reset();
        path.moveTo(5f, 5f);
        path.lineTo(5f, 23f);
        path.lineTo(10f, 18f);
        path.lineTo(15f, 28f);
        path.lineTo(17f, 27f);
        path.lineTo(12f, 16f);
        path.lineTo(23f, 16f);
        path.close();
        paint.setColor(0xFFF8F8F8);
        canvas.drawPath(path, paint);

        return bitmap;
    }

    private static Bitmap scaleCursorBitmapMaxAlpha(Bitmap source, int width, int height) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        int[] sourcePixels = new int[sourceWidth * sourceHeight];
        int[] scaledPixels = new int[width * height];

        source.getPixels(sourcePixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight);

        for (int y = 0; y < height; y++) {
            int sourceTop = Math.max(0, (int)Math.floor(y * sourceHeight / (float)height));
            int sourceBottom = Math.min(sourceHeight, (int)Math.ceil((y + 1) * sourceHeight / (float)height));

            for (int x = 0; x < width; x++) {
                int sourceLeft = Math.max(0, (int)Math.floor(x * sourceWidth / (float)width));
                int sourceRight = Math.min(sourceWidth, (int)Math.ceil((x + 1) * sourceWidth / (float)width));
                int bestPixel = 0;
                int bestAlpha = -1;

                for (int sy = sourceTop; sy < sourceBottom; sy++) {
                    int sourceOffset = sy * sourceWidth;

                    for (int sx = sourceLeft; sx < sourceRight; sx++) {
                        int pixel = sourcePixels[sourceOffset + sx];
                        int alpha = pixel >>> 24;

                        if (alpha > bestAlpha) {
                            bestAlpha = alpha;
                            bestPixel = pixel;
                        }
                    }
                }

                scaledPixels[y * width + x] = bestPixel;
            }
        }

        return Bitmap.createBitmap(scaledPixels, width, height, Bitmap.Config.ARGB_8888);
    }
}
