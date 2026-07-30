package com.limelight;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class GlideAndroidCompatibilityTest {
    @Test
    public void generatedModuleRetainsOpaqueRgb565DecodePolicy()
            throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        File source = createOpaqueJpeg(context);
        FutureTarget<Bitmap> target = Glide.with(context)
                .asBitmap()
                .load(source)
                .disallowHardwareConfig()
                .submit();
        try {
            Bitmap bitmap = target.get(3, TimeUnit.SECONDS);
            assertEquals(64, bitmap.getWidth());
            assertEquals(64, bitmap.getHeight());
            assertEquals(Bitmap.Config.RGB_565, bitmap.getConfig());
        }
        finally {
            clearTarget(context, target);
            assertTrue(source.delete() || !source.exists());
        }
    }

    @Test
    public void drawableLoadUsesPlatformBitmapDrawable() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        File source = createOpaqueJpeg(context);
        FutureTarget<Drawable> target = Glide.with(context)
                .load(source)
                .disallowHardwareConfig()
                .submit();
        try {
            Drawable drawable = target.get(3, TimeUnit.SECONDS);
            assertTrue(drawable instanceof BitmapDrawable);
        }
        finally {
            clearTarget(context, target);
            assertTrue(source.delete() || !source.exists());
        }
    }

    private static File createOpaqueJpeg(Context context) throws Exception {
        File file = File.createTempFile(
                "glide-compat-", ".jpg", context.getCacheDir());
        Bitmap source = Bitmap.createBitmap(
                64, 64, Bitmap.Config.ARGB_8888);
        source.eraseColor(Color.rgb(18, 92, 160));
        try (FileOutputStream output = new FileOutputStream(file)) {
            assertTrue(source.compress(Bitmap.CompressFormat.JPEG, 100, output));
        }
        finally {
            source.recycle();
        }
        return file;
    }

    private static void clearTarget(
            Context context, FutureTarget<?> target) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> Glide.with(context).clear(target));
    }
}
