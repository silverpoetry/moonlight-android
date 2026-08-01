package com.limelight.ui;

import static org.junit.Assert.assertEquals;

import android.app.Instrumentation;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class WrappingFlowLayoutTest {
    @Test
    public void wrapsMarginAwareChildrenAndMeasuresRows() {
        Instrumentation instrumentation =
                InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> {
            WrappingFlowLayout layout = new WrappingFlowLayout(
                    instrumentation.getTargetContext());
            View first = addChild(layout, 60, 20);
            View second = addChild(layout, 60, 20);
            View third = addChild(layout, 60, 20);

            layout.measure(
                    View.MeasureSpec.makeMeasureSpec(
                            140,
                            View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(
                            0,
                            View.MeasureSpec.UNSPECIFIED));
            layout.layout(0, 0, 140, layout.getMeasuredHeight());

            assertEquals(50, layout.getMeasuredHeight());
            assertEquals(0, first.getLeft());
            assertEquals(5, first.getTop());
            assertEquals(64, second.getLeft());
            assertEquals(5, second.getTop());
            assertEquals(0, third.getLeft());
            assertEquals(30, third.getTop());
        });
    }

    @Test
    public void laysOutRowsFromTheRightInRtl() {
        Instrumentation instrumentation =
                InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> {
            WrappingFlowLayout layout = new WrappingFlowLayout(
                    instrumentation.getTargetContext());
            layout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            View first = addChild(layout, 60, 20);
            View second = addChild(layout, 60, 20);

            layout.measure(
                    View.MeasureSpec.makeMeasureSpec(
                            140,
                            View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(
                            0,
                            View.MeasureSpec.UNSPECIFIED));
            layout.layout(0, 0, 140, layout.getMeasuredHeight());

            assertEquals(76, first.getLeft());
            assertEquals(12, second.getLeft());
        });
    }

    private static View addChild(
            ViewGroup parent,
            int width,
            int height) {
        Context context = parent.getContext();
        View child = new View(context);
        ViewGroup.MarginLayoutParams params =
                new ViewGroup.MarginLayoutParams(width, height);
        params.topMargin = 5;
        params.rightMargin = 4;
        parent.addView(child, params);
        return child;
    }
}
