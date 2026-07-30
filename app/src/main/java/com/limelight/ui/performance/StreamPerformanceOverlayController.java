package com.limelight.ui.performance;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import com.limelight.binding.video.PerfOverlayListener;
import com.limelight.binding.video.PerfOverlayStats;
import com.limelight.preferences.PreferenceConfiguration;
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

    private final Activity activity;
    private final PreferenceConfiguration preferences;
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
            PreferenceConfiguration preferences,
            RuntimeStateProvider
                    runtimeStateSupplier,
            Runnable compactOverlayAction) {
        this.activity = Objects.requireNonNull(
                activity,
                "activity");
        this.preferences = Objects.requireNonNull(
                preferences,
                "preferences");
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
            if (preferences.enablePerfOverlayLiteDialog) {
                this.compactOverlayAction.run();
            }
        });
        applyAllPreferences();
    }

    @Override
    public void onPerfUpdate(PerfOverlayStats stats) {
        activity.runOnUiThread(() -> {
            if (destroyed) {
                return;
            }
            PerformanceOverlayRuntimeState runtime =
                    runtimeStateSupplier.get();
            if (preferences.enablePerfOverlayLite) {
                String text = formatter.formatCompact(
                        stats,
                        preferences,
                        runtime);
                compactOverlay.setText(
                        applyHighlighting(text, runtime));
            }
            else {
                renderExpanded(stats, runtime);
            }
        });
    }

    public void toggleVisibility() {
        preferences.enablePerfOverlay =
                !preferences.enablePerfOverlay;
        applyOverlayVisibility();
    }

    public void toggleExpandedMode() {
        preferences.enablePerfOverlayLite =
                !preferences.enablePerfOverlayLite;
        overlay.setVisibility(View.VISIBLE);
        compactOverlay.setVisibility(
                preferences.enablePerfOverlayLite ?
                        View.VISIBLE :
                        View.GONE);
        expandedOverlay.setVisibility(
                preferences.enablePerfOverlayLite ?
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
                preferences.showRumbleHUD ?
                        View.VISIBLE :
                        View.GONE);
    }

    public void updateRumble(
            short controllerNumber,
            short lowFrequencyMotor,
            short highFrequencyMotor) {
        if (!preferences.showRumbleHUD) {
            return;
        }
        activity.runOnUiThread(() -> {
            if (destroyed || !preferences.showRumbleHUD) {
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
                preferences.enablePerfOverlayLiteDialog);
    }

    public void applyCompactScale() {
        compactOverlay.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                preferences.gameSettingPrefZoom * 0.1f);
        applyNetworkIcon();
    }

    public void applyCompactMargin() {
        if (preferences.performanceOverlayLiteMaginTop == 4) {
            return;
        }
        LinearLayout.LayoutParams params =
                (LinearLayout.LayoutParams)
                        compactOverlay.getLayoutParams();
        params.setMargins(
                0,
                UiHelper.dpToPx(
                        activity,
                        preferences.performanceOverlayLiteMaginTop),
                0,
                0);
        compactOverlay.setLayoutParams(params);
    }

    public void destroy() {
        destroyed = true;
        compactOverlay.setOnClickListener(null);
    }

    private void applyAllPreferences() {
        applyOverlayVisibility();
        applyRumbleVisibility();
        applyCompactInteractivity();
        applyCompactScale();
        applyCompactMargin();
    }

    private void applyOverlayVisibility() {
        if (!preferences.enablePerfOverlay) {
            overlay.setVisibility(View.GONE);
            compactOverlay.setVisibility(View.GONE);
            expandedOverlay.setVisibility(View.GONE);
            return;
        }

        overlay.setVisibility(View.VISIBLE);
        compactOverlay.setVisibility(
                preferences.enablePerfOverlayLite ?
                        View.VISIBLE :
                        View.GONE);
        expandedOverlay.setVisibility(
                preferences.enablePerfOverlayLite ?
                        View.GONE :
                        View.VISIBLE);
    }

    private void renderExpanded(
            PerfOverlayStats stats,
            PerformanceOverlayRuntimeState runtime) {
        expandedContent.removeAllViews();
        List<PerformanceOverlayFormatter.Row> rows =
                formatter.formatExpanded(
                        stats,
                        preferences,
                        runtime);
        for (PerformanceOverlayFormatter.Row row : rows) {
            addRow(row, runtime);
        }
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
        if (runtime.fsrEnabled) {
            applySpan(
                    spannable,
                    text,
                    "FSR ",
                    Color.rgb(250, 191, 2),
                    true);
        }
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
        ConnectivityManager connectivityManager =
                (ConnectivityManager) activity.getSystemService(
                        Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo =
                connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) {
            compactOverlay.setCompoundDrawables(
                    null,
                    null,
                    null,
                    null);
            return;
        }

        int icon = networkInfo.getType() ==
                ConnectivityManager.TYPE_MOBILE ?
                R.drawable.icon_axi_mobile :
                R.drawable.icon_axi_wifi;
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

    @SuppressWarnings("unchecked")
    private <ViewT extends View> ViewT requireView(int id) {
        View view = activity.findViewById(id);
        if (view == null) {
            throw new IllegalStateException(
                    "Missing performance overlay view: " + id);
        }
        return (ViewT) view;
    }
}
