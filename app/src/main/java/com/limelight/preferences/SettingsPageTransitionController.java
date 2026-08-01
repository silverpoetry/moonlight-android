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
 * transition, the outgoing page remains attached only long enough to render
 * the paired navigation animation. Starting another transition settles the
 * previous one first, so rapid navigation cannot leave stale pages behind.</p>
 */
final class SettingsPageTransitionController {
    enum Direction {
        NONE,
        FORWARD,
        BACKWARD
    }

    private static final long DURATION_MS = 220L;
    private static final float INCOMING_START_ALPHA = 0.72f;
    private static final float OUTGOING_END_ALPHA = 0.72f;
    private static final float OUTGOING_DISTANCE_RATIO = 0.35f;
    private static final float SLIDE_DISTANCE_RATIO = 0.16f;
    private static final int MIN_SLIDE_DISTANCE_DP = 32;
    private static final int MAX_SLIDE_DISTANCE_DP = 96;

    private final FrameLayout container;
    private final PathInterpolator interpolator =
            new PathInterpolator(0.2f, 0f, 0f, 1f);

    private Page currentPage;
    private Page outgoingPage;
    private long generation;
    private boolean destroyed;

    SettingsPageTransitionController(FrameLayout container) {
        this.container = Objects.requireNonNull(
                container,
                "container");
    }

    /**
     * Replaces the current page while animating only the page region that
     * represents navigation progress. This keeps persistent chrome, such as
     * the wide-layout section rail, visually stationary.
     */
    void replace(
            View nextPage,
            View nextMotionView,
            Direction direction) {
        Objects.requireNonNull(nextPage, "nextPage");
        Objects.requireNonNull(nextMotionView, "nextMotionView");
        Objects.requireNonNull(direction, "direction");
        if (destroyed) {
            return;
        }

        settleOnCurrentPage();
        Page previousPage = currentPage;
        Page nextPageEntry = new Page(nextPage, nextMotionView);
        currentPage = nextPageEntry;
        long transitionGeneration = ++generation;

        if (previousPage == null ||
                direction == Direction.NONE ||
                !areAnimationsEnabled()) {
            showImmediately(nextPageEntry);
            return;
        }

        container.addView(nextPage, matchParentLayoutParams());
        outgoingPage = previousPage;
        disableInteraction(previousPage.root);

        float directionSign = direction == Direction.FORWARD
                ? 1f
                : -1f;
        float slideDistance = getSlideDistance();
        nextMotionView.setAlpha(INCOMING_START_ALPHA);
        nextMotionView.setTranslationX(directionSign * slideDistance);

        previousPage.motion.animate()
                .translationX(-directionSign * slideDistance *
                        OUTGOING_DISTANCE_RATIO)
                .alpha(OUTGOING_END_ALPHA)
                .setDuration(DURATION_MS)
                .setInterpolator(interpolator)
                .withLayer()
                .start();
        nextMotionView.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(DURATION_MS)
                .setInterpolator(interpolator)
                .withLayer()
                .withEndAction(() -> finishTransition(
                        transitionGeneration,
                        previousPage,
                        nextPageEntry))
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

    private void showImmediately(Page nextPage) {
        cancelPageAnimations();
        container.removeAllViews();
        resetVisualState(nextPage);
        container.addView(nextPage.root, matchParentLayoutParams());
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
            if (child != currentPage.root) {
                container.removeViewAt(index);
            }
        }
        resetVisualState(currentPage);
        outgoingPage = null;
    }

    private void finishTransition(
            long transitionGeneration,
            Page previousPage,
            Page nextPage) {
        if (destroyed ||
                transitionGeneration != generation ||
                currentPage != nextPage) {
            return;
        }
        container.removeView(previousPage.root);
        resetVisualState(nextPage);
        outgoingPage = null;
    }

    private void cancelPageAnimations() {
        if (currentPage != null) {
            currentPage.motion.animate().cancel();
        }
        if (outgoingPage != null && outgoingPage != currentPage) {
            outgoingPage.motion.animate().cancel();
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

    private void resetVisualState(Page page) {
        page.root.setAlpha(1f);
        page.root.setTranslationX(0f);
        page.motion.setAlpha(1f);
        page.motion.setTranslationX(0f);
    }

    private float getSlideDistance() {
        float density = container.getResources()
                .getDisplayMetrics()
                .density;
        float minimum = MIN_SLIDE_DISTANCE_DP * density;
        float maximum = MAX_SLIDE_DISTANCE_DP * density;
        float availableWidth = container.getWidth();
        if (availableWidth <= 0f) {
            availableWidth = container.getResources()
                    .getDisplayMetrics()
                    .widthPixels;
        }
        return Math.max(
                minimum,
                Math.min(maximum, availableWidth *
                        SLIDE_DISTANCE_RATIO));
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

    private static final class Page {
        final View root;
        final View motion;

        Page(View root, View motion) {
            this.root = root;
            this.motion = motion;
        }
    }
}
