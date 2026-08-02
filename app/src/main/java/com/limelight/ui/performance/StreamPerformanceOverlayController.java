package com.limelight.ui.performance;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.Formatter;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;

import com.limelight.R;
import com.limelight.platform.AndroidNetworkTransport;
import com.limelight.binding.video.PerfOverlayListener;
import com.limelight.binding.video.PerfOverlayStats;
import com.limelight.settings.ui.StreamUiSettings;
import com.limelight.settings.ui.StreamUiSettingsState;
import com.limelight.utils.UiHelper;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Owns performance-overlay views, formatting, and preference application.
 */
public final class StreamPerformanceOverlayController
        implements PerfOverlayListener {
    public interface RuntimeStateProvider {
        PerformanceOverlayRuntimeState get();
    }

    public interface ConfigurationProvider {
        PerformanceOverlayConfiguration get();
    }

    private final Activity activity;
    private final StreamUiSettingsState uiSettingsState;
    private final ConfigurationProvider configurationProvider;
    private final RuntimeStateProvider
            runtimeStateSupplier;
    private final Runnable compactOverlayAction;
    private final PerformanceOverlayFormatter formatter;
    private final View overlay;
    private final TextView compactOverlay;
    private final LinearLayout expandedOverlay;
    private final LinearLayout expandedContent;
    private final TextView rumbleOverlay;
    private boolean destroyed;

    public StreamPerformanceOverlayController(
            Activity activity,
            StreamUiSettingsState uiSettingsState,
            ConfigurationProvider configurationProvider,
            RuntimeStateProvider
                    runtimeStateSupplier,
            Runnable compactOverlayAction) {
        this.activity = Objects.requireNonNull(
                activity,
                "activity");
        this.uiSettingsState = Objects.requireNonNull(
                uiSettingsState,
                "uiSettingsState");
        this.configurationProvider = Objects.requireNonNull(
                configurationProvider,
                "configurationProvider");
        this.runtimeStateSupplier = Objects.requireNonNull(
                runtimeStateSupplier,
                "runtimeStateSupplier");
        this.compactOverlayAction = Objects.requireNonNull(
                compactOverlayAction,
                "compactOverlayAction");
        formatter = new PerformanceOverlayFormatter(
                bytes -> Formatter.formatShortFileSize(
                        activity,
                        bytes));

        overlay = requireView(R.id.performanceOverlay);
        compactOverlay = requireView(
                R.id.performanceOverlayLite);
        expandedOverlay = requireView(
                R.id.performanceOverlayBig);
        expandedContent = requireView(
                R.id.performanceOverlayBigContent);
        rumbleOverlay = requireView(R.id.performanceRumble);

        compactOverlay.setOnClickListener(view -> {
            if (uiSettingsState
                    .get()
                    .isCompactPerformanceInteractive()) {
                this.compactOverlayAction.run();
            }
        });
        applyPreferences();
    }

    @Override
    public void onPerfUpdate(PerfOverlayStats stats) {
        activity.runOnUiThread(() -> {
            if (destroyed) {
                return;
            }
            PerformanceOverlayRuntimeState runtime =
                    runtimeStateSupplier.get();
            PerformanceOverlayConfiguration configuration =
                    requireConfiguration();
            if (configuration
                    .getUiSettings()
                    .isCompactPerformanceOverlay()) {
                String text = formatter.formatCompact(
                        stats,
                        configuration,
                        runtime);
                compactOverlay.setText(
                        applyHighlighting(text, runtime));
            }
            else {
                renderExpanded(
                        stats,
                        configuration,
                        runtime);
            }
        });
    }

    public void toggleVisibility() {
        StreamUiSettings settings = uiSettingsState.get();
        uiSettingsState.replace(settings.toBuilder()
                .setPerformanceOverlayEnabled(
                        !settings
                                .isPerformanceOverlayEnabled())
                .build());
        applyOverlayVisibility();
    }

    public void toggleExpandedMode() {
        StreamUiSettings settings = uiSettingsState.get();
        StreamUiSettings updated = settings.toBuilder()
                .setCompactPerformanceOverlay(
                        !settings
                                .isCompactPerformanceOverlay())
                .build();
        uiSettingsState.replace(updated);
        overlay.setVisibility(View.VISIBLE);
        compactOverlay.setVisibility(
                updated.isCompactPerformanceOverlay() ?
                        View.VISIBLE :
                        View.GONE);
        expandedOverlay.setVisibility(
                updated.isCompactPerformanceOverlay() ?
                        View.GONE :
                        View.VISIBLE);
    }

    public void hideForPictureInPicture() {
        overlay.setVisibility(View.GONE);
    }

    public void restoreAfterPictureInPicture() {
        applyOverlayVisibility();
    }

    public void applyRumbleVisibility() {
        rumbleOverlay.setVisibility(
                uiSettingsState
                        .get()
                        .isRumbleOverlayEnabled() ?
                        View.VISIBLE :
                        View.GONE);
    }

    public void updateRumble(
            short controllerNumber,
            short lowFrequencyMotor,
            short highFrequencyMotor) {
        if (!uiSettingsState.get().isRumbleOverlayEnabled()) {
            return;
        }
        activity.runOnUiThread(() -> {
            if (destroyed ||
                    !uiSettingsState
                            .get()
                            .isRumbleOverlayEnabled()) {
                return;
            }
            rumbleOverlay.setText(String.format(
                    Locale.US,
                    "手柄%d 震动信号 高%d 低%d",
                    controllerNumber,
                    (short) ((highFrequencyMotor >> 8) & 0xFF),
                    (short) ((lowFrequencyMotor >> 8) & 0xFF)));
        });
    }

    public void applyCompactInteractivity() {
        compactOverlay.setClickable(
                uiSettingsState
                        .get()
                        .isCompactPerformanceInteractive());
    }

    public void applyCompactScale() {
        compactOverlay.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                uiSettingsState
                        .get()
                        .getCompactPerformanceScalePercent() *
                        0.1f);
        applyNetworkIcon();
    }

    public void applyCompactMargin() {
        int marginTopDp = uiSettingsState
                .get()
                .getCompactPerformanceMarginTopDp();
        LinearLayout.LayoutParams params =
                (LinearLayout.LayoutParams)
                        compactOverlay.getLayoutParams();
        params.setMargins(
                0,
                UiHelper.dpToPx(
                        activity,
                        marginTopDp),
                0,
                0);
        compactOverlay.setLayoutParams(params);
    }

    public void destroy() {
        destroyed = true;
        compactOverlay.setOnClickListener(null);
    }

    /** Applies the current UI settings snapshot without rebuilding the view. */
    public void applyPreferences() {
        applyOverlayVisibility();
        applyRumbleVisibility();
        applyCompactInteractivity();
        applyCompactScale();
        applyCompactMargin();
    }

    private void applyOverlayVisibility() {
        StreamUiSettings settings = uiSettingsState.get();
        if (!settings.isPerformanceOverlayEnabled()) {
            overlay.setVisibility(View.GONE);
            compactOverlay.setVisibility(View.GONE);
            expandedOverlay.setVisibility(View.GONE);
            return;
        }

        overlay.setVisibility(View.VISIBLE);
        compactOverlay.setVisibility(
                settings.isCompactPerformanceOverlay() ?
                        View.VISIBLE :
                        View.GONE);
        expandedOverlay.setVisibility(
                settings.isCompactPerformanceOverlay() ?
                        View.GONE :
                        View.VISIBLE);
    }

    private void renderExpanded(
            PerfOverlayStats stats,
            PerformanceOverlayConfiguration configuration,
            PerformanceOverlayRuntimeState runtime) {
        expandedContent.removeAllViews();
        List<PerformanceOverlayFormatter.Row> rows =
                formatter.formatExpanded(
                        stats,
                        configuration,
                        runtime);
        for (PerformanceOverlayFormatter.Row row : rows) {
            addRow(row, runtime);
        }
    }

    private PerformanceOverlayConfiguration
            requireConfiguration() {
        return Objects.requireNonNull(
                configurationProvider.get(),
                "configurationProvider returned null");
    }

    private void addRow(
            PerformanceOverlayFormatter.Row row,
            PerformanceOverlayRuntimeState runtime) {
        LinearLayout rowView = new LinearLayout(activity);
        rowView.setOrientation(LinearLayout.HORIZONTAL);
        rowView.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin =
                expandedContent.getChildCount() == 0 ?
                        0 :
                        UiHelper.dpToPx(activity, 6);
        rowView.setLayoutParams(rowParams);

        TextView labelView = new TextView(activity);
        labelView.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f));
        labelView.setText(row.label);
        labelView.setTextColor(
                Color.argb(204, 218, 230, 255));
        labelView.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                10f);
        labelView.setSingleLine(true);

        TextView valueView = new TextView(activity);
        valueView.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f));
        valueView.setText(applyHighlighting(
                row.value,
                runtime));
        valueView.setTextColor(Color.WHITE);
        valueView.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                10f);
        valueView.setGravity(Gravity.END);
        valueView.setSingleLine(true);

        rowView.addView(labelView);
        rowView.addView(valueView);
        expandedContent.addView(rowView);
    }

    private CharSequence applyHighlighting(
            String text,
            PerformanceOverlayRuntimeState runtime) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        SpannableString spannable = new SpannableString(text);
        int micStart = text.indexOf("Mic");
        if (micStart < 0) {
            micStart = text.indexOf("麦克风");
        }
        if (micStart >= 0) {
            applySpan(
                    spannable,
                    text,
                    micStart,
                    Color.rgb(79, 210, 122),
                    false);
        }
        return spannable;
    }

    private void applySpan(SpannableString spannable,
                           String text,
                           String marker,
                           int color,
                           boolean stopAtDoubleSpace) {
        int start = text.indexOf(marker);
        if (start >= 0) {
            applySpan(
                    spannable,
                    text,
                    start,
                    color,
                    stopAtDoubleSpace);
        }
    }

    private void applySpan(SpannableString spannable,
                           String text,
                           int start,
                           int color,
                           boolean stopAtDoubleSpace) {
        int end = text.indexOf('\n', start);
        if (end < 0 && stopAtDoubleSpace) {
            end = text.indexOf("  ", start);
        }
        if (end < 0) {
            end = text.length();
        }
        spannable.setSpan(
                new ForegroundColorSpan(color),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void applyNetworkIcon() {
        AndroidNetworkTransport transport =
                AndroidNetworkTransport.getActive(activity);
        if (transport == AndroidNetworkTransport.NONE) {
            compactOverlay.setCompoundDrawables(
                    null,
                    null,
                    null,
                    null);
            return;
        }

        int icon = transport == AndroidNetworkTransport.CELLULAR ?
                R.drawable.ic_network_mobile :
                R.drawable.ic_network_wifi;
        Drawable drawable =
                AppCompatResources.getDrawable(activity, icon);
        if (drawable == null) {
            return;
        }
        int textSize = Math.max(
                1,
                Math.round(compactOverlay.getTextSize()));
        drawable.setBounds(0, 0, textSize, textSize);
        compactOverlay.setCompoundDrawables(
                drawable,
                null,
                null,
                null);
    }

    private <ViewT extends View> ViewT requireView(int id) {
        ViewT view = activity.findViewById(id);
        if (view == null) {
            throw new IllegalStateException(
                    "Missing performance overlay view: " + id);
        }
        return view;
    }
}
