package com.limelight;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RenderingResourcesTest {

    @Test
    @SdkSuppress(minSdkVersion = 33)
    public void launcherProvidesSafeAdaptiveLayers() {
        Context context = ApplicationProvider.getApplicationContext();
        Drawable launcher = context.getDrawable(R.mipmap.ic_app_axi);
        assertTrue(launcher instanceof AdaptiveIconDrawable);

        AdaptiveIconDrawable adaptiveIcon = (AdaptiveIconDrawable) launcher;
        assertNotNull(adaptiveIcon.getBackground());
        assertNotNull(adaptiveIcon.getForeground());
        assertNotNull(adaptiveIcon.getMonochrome());

        Bitmap foreground = renderDrawable(
                context.getDrawable(R.mipmap.ic_app_axi_foreground),
                108,
                108);
        try {
            AlphaBounds bounds = findAlphaBounds(foreground);
            assertTrue(bounds.left >= 20);
            assertTrue(bounds.right <= 88);
            assertTrue(bounds.top >= 20);
            assertTrue(bounds.bottom <= 88);
        }
        finally {
            foreground.recycle();
        }
    }

    @Test
    public void compactControllerRetainsTransparentControlCutouts() {
        Context context = ApplicationProvider.getApplicationContext();
        Bitmap controller = renderDrawable(
                context.getDrawable(R.drawable.ic_game_controller),
                1024,
                1024);
        try {
            assertEquals(0, Color.alpha(controller.getPixel(309, 456)));
            assertEquals(0, Color.alpha(controller.getPixel(588, 456)));
            assertEquals(0, Color.alpha(controller.getPixel(681, 364)));
            assertEquals(0, Color.alpha(controller.getPixel(681, 549)));
            assertEquals(0, Color.alpha(controller.getPixel(774, 456)));
            assertTrue(Color.alpha(controller.getPixel(512, 250)) > 0);
        }
        finally {
            controller.recycle();
        }
    }

    private static Bitmap renderDrawable(Drawable drawable, int width, int height) {
        assertNotNull(drawable);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(new Canvas(bitmap));
        return bitmap;
    }

    private static AlphaBounds findAlphaBounds(Bitmap bitmap) {
        int left = bitmap.getWidth();
        int top = bitmap.getHeight();
        int right = -1;
        int bottom = -1;
        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                if (Color.alpha(bitmap.getPixel(x, y)) == 0) {
                    continue;
                }
                left = Math.min(left, x);
                top = Math.min(top, y);
                right = Math.max(right, x);
                bottom = Math.max(bottom, y);
            }
        }
        assertTrue(right >= left);
        assertTrue(bottom >= top);
        return new AlphaBounds(left, top, right, bottom);
    }

    private static final class AlphaBounds {
        final int left;
        final int top;
        final int right;
        final int bottom;

        AlphaBounds(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }
}
