package com.limelight.preferences;

import android.animation.ValueAnimator;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;

import java.util.Objects;

/**
 * Owns replacement transitions for the settings page stack.
 *
 * <p>The controller keeps exactly one authoritative current page. During a
 * forward transition, the outgoing page remains attached only long enough
 * to render the same entrance motion used when opening settings. Backward
 * and non-navigation replacements settle immediately. Starting another
 * transition first removes stale pages, so rapid navigation cannot leave
 * duplicate content behind.</p>
 */
final class SettingsPageTransitionController {
    enum Direction {
        NONE,
        FORWARD,
        BACKWARD
    }

    private static final float OUTGOING_DISTANCE_RATIO = 0.18f;

    private final FrameLayout container;
    private final PathInterpolator interpolator =
            new PathInterpolator(0.2f, 0f, 0f, 1f);

    private View currentPage;
    private View outgoingPage;
    private long generation;
    private boolean destroyed;

    SettingsPageTransitionController(FrameLayout container) {
        this.container = Objects.requireNonNull(
                container,
                "container");
    }

    /**
     * Replaces the current settings screen as one opaque page. Header,
     * background, and content move together so two readable screens can
     * never be composited on top of one another.
     */
    void replace(View nextPage, Direction direction) {
        Objects.requireNonNull(nextPage, "nextPage");
        Objects.requireNonNull(direction, "direction");
        if (destroyed) {
            return;
        }

        settleOnCurrentPage();
        View previousPage = currentPage;
        currentPage = nextPage;
        long transitionGeneration = ++generation;

        if (previousPage == null ||
                direction != Direction.FORWARD ||
                !areAnimationsEnabled()) {
            showImmediately(nextPage);
            return;
        }

        container.addView(nextPage, matchParentLayoutParams());
        outgoingPage = previousPage;
        disableInteraction(previousPage);

        float slideDistance = getSlideDistance();
        nextPage.setAlpha(1f);
        nextPage.setTranslationX(slideDistance);

        previousPage.animate()
                .translationX(-slideDistance *
                        OUTGOING_DISTANCE_RATIO)
                .setDuration(getDurationMs())
                .setInterpolator(interpolator)
                .withLayer()
                .start();
        nextPage.animate()
                .translationX(0f)
                .setDuration(getDurationMs())
                .setInterpolator(interpolator)
                .withLayer()
                .withEndAction(() -> finishTransition(
                        transitionGeneration,
                        previousPage,
                        nextPage))
                .start();
    }

    void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        generation++;
        cancelPageAnimations();
        currentPage = null;
        outgoingPage = null;
    }

    private void showImmediately(View nextPage) {
        cancelPageAnimations();
        container.removeAllViews();
        resetVisualState(nextPage);
        container.addView(nextPage, matchParentLayoutParams());
        outgoingPage = null;
    }

    private void settleOnCurrentPage() {
        generation++;
        cancelPageAnimations();
        if (currentPage == null) {
            container.removeAllViews();
            outgoingPage = null;
            return;
        }

        for (int index = container.getChildCount() - 1;
                index >= 0;
                index--) {
            View child = container.getChildAt(index);
            if (child != currentPage) {
                container.removeViewAt(index);
            }
        }
        resetVisualState(currentPage);
        outgoingPage = null;
    }

    private void finishTransition(
            long transitionGeneration,
            View previousPage,
            View nextPage) {
        if (destroyed ||
                transitionGeneration != generation ||
                currentPage != nextPage) {
            return;
        }
        container.removeView(previousPage);
        resetVisualState(nextPage);
        outgoingPage = null;
    }

    private void cancelPageAnimations() {
        if (currentPage != null) {
            currentPage.animate().cancel();
        }
        if (outgoingPage != null && outgoingPage != currentPage) {
            outgoingPage.animate().cancel();
        }
    }

    private void disableInteraction(View view) {
        view.setEnabled(false);
        if (!(view instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) view;
        for (int index = 0; index < group.getChildCount(); index++) {
            disableInteraction(group.getChildAt(index));
        }
    }

    private void resetVisualState(View page) {
        page.setAlpha(1f);
        page.setTranslationX(0f);
    }

    private float getSlideDistance() {
        float availableWidth = container.getWidth();
        if (availableWidth <= 0f) {
            availableWidth = container.getResources()
                    .getDisplayMetrics()
                    .widthPixels;
        }
        return availableWidth;
    }

    private long getDurationMs() {
        return container.getResources().getInteger(
                android.R.integer.config_mediumAnimTime);
    }

    private boolean areAnimationsEnabled() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                ValueAnimator.areAnimatorsEnabled();
    }

    private FrameLayout.LayoutParams matchParentLayoutParams() {
        return new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
    }
}
