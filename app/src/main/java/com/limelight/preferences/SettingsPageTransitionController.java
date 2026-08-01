package com.limelight.preferences;

import android.animation.ValueAnimator;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;

import java.util.Objects;

/**
 * Owns the compact settings page stack transition.
 *
 * <p>A forward navigation places the destination above the source and moves
 * it in from the right. A backward navigation keeps the destination below
 * the current page and moves only the current page out to the right. This is
 * the same visual model as an Android activity stack: the previous page is
 * revealed on back instead of being animated in as a new foreground page.</p>
 */
final class SettingsPageTransitionController {
    enum Direction {
        NONE,
        FORWARD,
        BACKWARD
    }

    private static final float BACKGROUND_PARALLAX_RATIO = 0.18f;

    private final FrameLayout container;
    private final PathInterpolator interpolator =
            new PathInterpolator(0.2f, 0f, 0f, 1f);

    private View currentPage;
    private View outgoingPage;
    private long generation;
    private boolean destroyed;

    SettingsPageTransitionController(FrameLayout container) {
        this.container = Objects.requireNonNull(container, "container");
    }

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
                direction == Direction.NONE ||
                !areAnimationsEnabled()) {
            showImmediately(nextPage);
            return;
        }

        outgoingPage = previousPage;
        disableInteraction(previousPage);
        float slideDistance = getSlideDistance();

        if (direction == Direction.FORWARD) {
            container.addView(nextPage, matchParentLayoutParams());
            resetVisualState(previousPage);
            nextPage.setAlpha(1f);
            nextPage.setTranslationX(slideDistance);

            previousPage.animate()
                    .translationX(-slideDistance *
                            BACKGROUND_PARALLAX_RATIO)
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
            return;
        }

        // The destination stays behind the outgoing page on back. This
        // prevents the root page from visibly flying in over the detail page.
        nextPage.setAlpha(1f);
        nextPage.setTranslationX(-slideDistance *
                BACKGROUND_PARALLAX_RATIO);
        container.addView(nextPage, 0, matchParentLayoutParams());
        resetVisualState(previousPage);

        nextPage.animate()
                .translationX(0f)
                .setDuration(getDurationMs())
                .setInterpolator(interpolator)
                .withLayer()
                .start();
        previousPage.animate()
                .translationX(slideDistance)
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
