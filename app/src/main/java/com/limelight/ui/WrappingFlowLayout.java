package com.limelight.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * A small horizontal flow container that wraps margin-aware children onto
 * additional rows. It intentionally exposes no styling API: callers own all
 * spacing through ordinary child margins and container padding.
 */
public final class WrappingFlowLayout extends ViewGroup {
    public WrappingFlowLayout(Context context) {
        super(context);
    }

    public WrappingFlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WrappingFlowLayout(
            Context context,
            AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(
            int widthMeasureSpec,
            int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int availableWidth = widthMode == MeasureSpec.UNSPECIFIED
                ? Integer.MAX_VALUE
                : Math.max(
                        0,
                        widthSize - getPaddingLeft() - getPaddingRight());

        int lineWidth = 0;
        int lineHeight = 0;
        int maximumLineWidth = 0;
        int contentHeight = 0;
        int childState = 0;
        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);
            if (child.getVisibility() == GONE) {
                continue;
            }
            measureChildWithMargins(
                    child,
                    widthMeasureSpec,
                    0,
                    heightMeasureSpec,
                    0);
            MarginLayoutParams margins =
                    (MarginLayoutParams) child.getLayoutParams();
            int childWidth = child.getMeasuredWidth() +
                    margins.leftMargin + margins.rightMargin;
            int childHeight = child.getMeasuredHeight() +
                    margins.topMargin + margins.bottomMargin;

            if (lineWidth > 0 &&
                    lineWidth + childWidth > availableWidth) {
                maximumLineWidth = Math.max(
                        maximumLineWidth,
                        lineWidth);
                contentHeight += lineHeight;
                lineWidth = 0;
                lineHeight = 0;
            }
            lineWidth += childWidth;
            lineHeight = Math.max(lineHeight, childHeight);
            childState = combineMeasuredStates(
                    childState,
                    child.getMeasuredState());
        }
        maximumLineWidth = Math.max(maximumLineWidth, lineWidth);
        contentHeight += lineHeight;

        int desiredWidth = maximumLineWidth +
                getPaddingLeft() + getPaddingRight();
        int desiredHeight = contentHeight +
                getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(
                resolveSizeAndState(
                        Math.max(desiredWidth, getSuggestedMinimumWidth()),
                        widthMeasureSpec,
                        childState),
                resolveSizeAndState(
                        Math.max(desiredHeight, getSuggestedMinimumHeight()),
                        heightMeasureSpec,
                        childState << MEASURED_HEIGHT_STATE_SHIFT));
    }

    @Override
    protected void onLayout(
            boolean changed,
            int left,
            int top,
            int right,
            int bottom) {
        int availableWidth = Math.max(
                0,
                right - left - getPaddingLeft() - getPaddingRight());
        int lineWidth = 0;
        int lineHeight = 0;
        int lineTop = getPaddingTop();
        boolean rightToLeft = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        int cursor = rightToLeft
                ? right - left - getPaddingRight()
                : getPaddingLeft();

        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);
            if (child.getVisibility() == GONE) {
                continue;
            }
            MarginLayoutParams margins =
                    (MarginLayoutParams) child.getLayoutParams();
            int occupiedWidth = child.getMeasuredWidth() +
                    margins.leftMargin + margins.rightMargin;
            int occupiedHeight = child.getMeasuredHeight() +
                    margins.topMargin + margins.bottomMargin;
            if (lineWidth > 0 &&
                    lineWidth + occupiedWidth > availableWidth) {
                lineTop += lineHeight;
                lineWidth = 0;
                lineHeight = 0;
                cursor = rightToLeft
                        ? right - left - getPaddingRight()
                        : getPaddingLeft();
            }

            int childLeft;
            int childRight;
            if (rightToLeft) {
                childRight = cursor - margins.rightMargin;
                childLeft = childRight - child.getMeasuredWidth();
                cursor -= occupiedWidth;
            } else {
                childLeft = cursor + margins.leftMargin;
                childRight = childLeft + child.getMeasuredWidth();
                cursor += occupiedWidth;
            }
            int childTop = lineTop + margins.topMargin;
            child.layout(
                    childLeft,
                    childTop,
                    childRight,
                    childTop + child.getMeasuredHeight());
            lineWidth += occupiedWidth;
            lineHeight = Math.max(lineHeight, occupiedHeight);
        }
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams params) {
        return new MarginLayoutParams(params);
    }

    @Override
    protected boolean checkLayoutParams(LayoutParams params) {
        return params instanceof MarginLayoutParams;
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }
}
