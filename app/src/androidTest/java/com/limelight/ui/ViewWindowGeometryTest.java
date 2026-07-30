package com.limelight.ui;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.limelight.utils.ViewWindowGeometry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ViewWindowGeometryTest {
    @Test
    public void boundsIncludeAncestorsAndViewTransformExactlyOnce() {
        Context context = ApplicationProvider.getApplicationContext();
        FrameLayout root = new FrameLayout(context);
        FrameLayout parent = new FrameLayout(context);
        View child = new View(context);

        FrameLayout.LayoutParams parentParams =
                new FrameLayout.LayoutParams(500, 300);
        parentParams.leftMargin = 50;
        parentParams.topMargin = 30;
        root.addView(parent, parentParams);

        FrameLayout.LayoutParams childParams =
                new FrameLayout.LayoutParams(200, 100);
        childParams.leftMargin = 20;
        childParams.topMargin = 10;
        parent.addView(child, childParams);

        root.measure(
                View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, 800, 600);

        child.setPivotX(0);
        child.setPivotY(0);
        child.setScaleX(0.5f);
        child.setScaleY(0.5f);
        child.setTranslationX(10);
        child.setTranslationY(20);

        Rect bounds = new Rect();
        assertTrue(ViewWindowGeometry.getVisibleBoundsInWindow(
                child,
                root,
                bounds,
                new int[2]));
        assertEquals(new Rect(80, 60, 180, 110), bounds);
    }

    @Test
    public void rejectsViewHostedByAnotherWindowRoot() {
        Context context = ApplicationProvider.getApplicationContext();
        FrameLayout sourceRoot = new FrameLayout(context);
        View source = new View(context);
        sourceRoot.addView(source);

        Rect bounds = new Rect();
        assertFalse(ViewWindowGeometry.getVisibleBoundsInWindow(
                source,
                new FrameLayout(context),
                bounds,
                new int[2]));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUndersizedScratchArray() {
        View view =
                new View(ApplicationProvider.getApplicationContext());
        ViewWindowGeometry.getVisibleBoundsInWindow(
                view,
                view.getRootView(),
                new Rect(),
                new int[1]);
    }
}
