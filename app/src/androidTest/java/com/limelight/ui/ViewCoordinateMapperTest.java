package com.limelight.ui;

import android.content.Context;
import android.graphics.Matrix;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.limelight.utils.ViewCoordinateMapper;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ViewCoordinateMapperTest {
    @Test
    public void sharedParentInsetIsNotCountedAsCursorOffset() {
        Context context = ApplicationProvider.getApplicationContext();
        FrameLayout parent = new FrameLayout(context);
        parent.setPadding(126, 0, 0, 0);

        View stream = new View(context);
        View overlay = new View(context);
        parent.addView(stream, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        parent.addView(overlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        int widthSpec = View.MeasureSpec.makeMeasureSpec(
                2400, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(
                1080, View.MeasureSpec.EXACTLY);
        parent.measure(widthSpec, heightSpec);
        parent.layout(0, 0, 2400, 1080);

        float[] point = {640f, 360f};
        assertTrue(ViewCoordinateMapper.mapPointBetweenSiblings(
                stream, overlay, point, new Matrix()));
        assertEquals(640f, point[0], 0f);
        assertEquals(360f, point[1], 0f);
    }

    @Test
    public void sourceAndTargetTransformsAreIncludedExactlyOnce() {
        Context context = ApplicationProvider.getApplicationContext();
        FrameLayout parent = new FrameLayout(context);
        View stream = new View(context);
        View overlay = new View(context);
        parent.addView(stream, new FrameLayout.LayoutParams(1000, 500));
        parent.addView(overlay, new FrameLayout.LayoutParams(1000, 500));
        parent.measure(
                View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(700, View.MeasureSpec.EXACTLY));
        parent.layout(0, 0, 1200, 700);

        stream.setPivotX(0f);
        stream.setPivotY(0f);
        stream.setScaleX(0.5f);
        stream.setScaleY(0.5f);
        stream.setTranslationX(30f);
        stream.setTranslationY(20f);

        overlay.setTranslationX(10f);
        overlay.setTranslationY(5f);

        float[] point = {200f, 100f};
        assertTrue(ViewCoordinateMapper.mapPointBetweenSiblings(
                stream, overlay, point, new Matrix()));
        assertEquals(120f, point[0], 0.001f);
        assertEquals(65f, point[1], 0.001f);
    }

    @Test
    public void basisMappingExcludesLayoutTranslation() {
        Context context = ApplicationProvider.getApplicationContext();
        FrameLayout parent = new FrameLayout(context);
        View stream = new View(context);
        View overlay = new View(context);
        parent.addView(stream, new FrameLayout.LayoutParams(1000, 500));
        parent.addView(overlay, new FrameLayout.LayoutParams(1000, 500));
        parent.measure(
                View.MeasureSpec.makeMeasureSpec(
                        1200,
                        View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(
                        700,
                        View.MeasureSpec.EXACTLY));
        parent.layout(0, 0, 1200, 700);

        stream.setPivotX(0f);
        stream.setPivotY(0f);
        stream.setScaleX(0.5f);
        stream.setScaleY(0.25f);
        stream.setTranslationX(300f);
        stream.setTranslationY(200f);
        overlay.setTranslationX(40f);
        overlay.setTranslationY(30f);

        float[] basis = {2f, 0f, 0f, 4f};
        assertTrue(ViewCoordinateMapper.mapBasisBetweenSiblings(
                stream,
                overlay,
                basis,
                new Matrix()));
        assertEquals(1f, basis[0], 0.001f);
        assertEquals(0f, basis[1], 0.001f);
        assertEquals(0f, basis[2], 0.001f);
        assertEquals(1f, basis[3], 0.001f);
    }
}
