package com.limelight.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

public class NativeCursorOverlayView extends View {
    public static final int CURSOR_FORMAT_BGRA = 1;

    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

    private Bitmap cursorBitmap;
    private Bitmap scaledCursorBitmap;
    private boolean visible;
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

        if (oldVisible != visible) {
            invalidate();
        }
    }

    public void setCursorPosition(float x, float y) {
        invalidateCursorBounds();
        this.x = x;
        this.y = y;
        this.hasPosition = true;
        invalidateCursorBounds();
    }

    public void clearCursor() {
        visible = false;
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
        invalidate((int)bounds.left - 2, (int)bounds.top - 2,
                (int)bounds.right + 2, (int)bounds.bottom + 2);
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
        // with color data but a fully transparent alpha channel.
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
