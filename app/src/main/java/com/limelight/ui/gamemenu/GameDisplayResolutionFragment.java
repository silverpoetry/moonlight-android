package com.limelight.ui.gamemenu;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import androidx.annotation.StringRes;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowMetrics;
import android.widget.EditText;
import android.widget.TextView;

import com.limelight.R;
import com.limelight.ui.BaseFragmentDialog.BaseGameMenuDialog;
import com.limelight.utils.UiToast;

import org.apmem.tools.layouts.FlowLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameDisplayResolutionFragment
        extends BaseGameMenuDialog implements View.OnClickListener {
    private static final String PREFS_NAME = "CustomResolutions";
    private static final String KEY_RESOLUTIONS = "resolutions";
    private static final int MIN_DIMENSION = 1;
    private static final int MAX_DIMENSION = 99_999;
    private static final int PRESET_COUNT = 6;

    private final Set<String> defaultResolutions = new HashSet<>();

    private int titleRes = R.string.game_menu_resolution;
    private EditText widthInput;
    private EditText heightInput;
    private FlowLayout customResolutionFlow;
    private TextView customResolutionTitle;
    private Listener listener;

    @Override
    public int getLayoutRes() {
        return R.layout.dialog_game_menu_display_resolution;
    }

    @Override
    public void bindView(View view) {
        super.bindView(view);

        TextView titleView = view.findViewById(R.id.tx_title);
        titleView.setText(titleRes);
        widthInput = view.findViewById(R.id.edt_width);
        heightInput = view.findViewById(R.id.edt_height);
        customResolutionFlow = view.findViewById(R.id.flow_custom);
        customResolutionTitle = view.findViewById(R.id.tx_custom_title);

        defaultResolutions.clear();
        int[] windowDimensions = getWindowDimensions();
        TextView nativeResolutionView = view.findViewWithTag("5");
        nativeResolutionView.setText(getResolutionText(
                windowDimensions[0], windowDimensions[1]));

        for (int index = 0; index < PRESET_COUNT; index++) {
            TextView preset = view.findViewWithTag(
                    Integer.toString(index));
            String resolution = preset.getText().toString().trim();
            if (parseResolution(resolution) == null) {
                preset.setEnabled(false);
                continue;
            }
            defaultResolutions.add(resolution);
            preset.setOnClickListener(
                    clickedView -> selectResolution(resolution));
        }

        loadCustomResolutions();
        view.findViewById(R.id.ibtn_back).setOnClickListener(this);
        view.findViewById(R.id.btn_right).setOnClickListener(this);
    }

    private int[] getWindowDimensions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics metrics =
                    getActivity().getWindowManager()
                            .getCurrentWindowMetrics();
            Rect bounds = metrics.getBounds();
            return new int[] {bounds.width(), bounds.height()};
        }
        return new int[] {
                getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels
        };
    }

    private void loadCustomResolutions() {
        customResolutionFlow.removeAllViews();
        Set<String> savedResolutions = getSavedResolutions();

        boolean hasCustomResolution = false;
        List<String> sortedResolutions =
                new ArrayList<>(savedResolutions);
        Collections.sort(sortedResolutions);
        for (String resolution : sortedResolutions) {
            if (!defaultResolutions.contains(resolution) &&
                    parseResolution(resolution) != null) {
                addResolutionToFlow(resolution);
                hasCustomResolution = true;
            }
        }
        customResolutionTitle.setVisibility(
                hasCustomResolution ? View.VISIBLE : View.GONE);
    }

    private void addResolutionToFlow(String resolution) {
        TextView resolutionView = (TextView) LayoutInflater
                .from(getActivity())
                .inflate(
                        R.layout.layout_resolution_item,
                        customResolutionFlow,
                        false);
        resolutionView.setText(resolution);
        resolutionView.setOnClickListener(
                view -> selectResolution(resolution));
        resolutionView.setOnLongClickListener(view -> {
            showDeleteConfirmDialog(resolution);
            return true;
        });
        customResolutionFlow.addView(resolutionView);
    }

    private void selectResolution(String resolution) {
        int[] dimensions = parseResolution(resolution);
        if (dimensions == null) {
            return;
        }
        if (listener != null) {
            listener.onResolutionSelected(
                    dimensions[0], dimensions[1]);
        }
        dismiss();
    }

    private void showDeleteConfirmDialog(String resolution) {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.game_menu_resolution_delete_title)
                .setMessage(getString(
                        R.string.game_menu_resolution_delete_message,
                        resolution))
                .setPositiveButton(
                        R.string.game_menu_delete,
                        (dialog, which) ->
                                deleteResolution(resolution))
                .setNegativeButton(
                        R.string.game_menu_customize_cancel, null)
                .show();
    }

    private void deleteResolution(String resolution) {
        Set<String> resolutions = getSavedResolutions();
        if (resolutions.remove(resolution)) {
            getPreferences().edit()
                    .putStringSet(KEY_RESOLUTIONS, resolutions)
                    .apply();
            loadCustomResolutions();
        }
    }

    private void saveResolution(int width, int height) {
        String resolution = getResolutionText(width, height);
        if (defaultResolutions.contains(resolution)) {
            return;
        }

        Set<String> resolutions = getSavedResolutions();
        if (resolutions.add(resolution)) {
            getPreferences().edit()
                    .putStringSet(KEY_RESOLUTIONS, resolutions)
                    .apply();
        }
    }

    private SharedPreferences getPreferences() {
        return getActivity().getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE);
    }

    private Set<String> getSavedResolutions() {
        Set<String> savedResolutions = getPreferences().getStringSet(
                KEY_RESOLUTIONS, null);
        return savedResolutions == null ?
                new HashSet<>() : new HashSet<>(savedResolutions);
    }

    private String getResolutionText(int width, int height) {
        return getString(
                R.string.game_menu_resolution_format,
                width,
                height);
    }

    private static int[] parseResolution(String resolution) {
        if (resolution == null) {
            return null;
        }
        String[] dimensions = resolution.split("x", -1);
        if (dimensions.length != 2) {
            return null;
        }
        Integer width = BoundedIntegerParser.parse(
                dimensions[0], MIN_DIMENSION, MAX_DIMENSION);
        Integer height = BoundedIntegerParser.parse(
                dimensions[1], MIN_DIMENSION, MAX_DIMENSION);
        return width != null && height != null ?
                new int[] {width, height} : null;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.ibtn_back) {
            dismiss();
            return;
        }
        if (view.getId() != R.id.btn_right) {
            return;
        }

        String widthText =
                widthInput.getText().toString().trim();
        String heightText =
                heightInput.getText().toString().trim();
        if (TextUtils.isEmpty(widthText)) {
            showToast(R.string.game_menu_width_required);
            return;
        }
        if (TextUtils.isEmpty(heightText)) {
            showToast(R.string.game_menu_height_required);
            return;
        }

        Integer width = BoundedIntegerParser.parse(
                widthText, MIN_DIMENSION, MAX_DIMENSION);
        Integer height = BoundedIntegerParser.parse(
                heightText, MIN_DIMENSION, MAX_DIMENSION);
        if (width == null || height == null) {
            showToast(R.string.game_menu_resolution_invalid);
            return;
        }

        saveResolution(width, height);
        if (listener != null) {
            listener.onResolutionSelected(width, height);
        }
        dismiss();
    }

    private void showToast(@StringRes int messageRes) {
        UiToast.makeText(
                getActivity(), messageRes, UiToast.LENGTH_SHORT).show();
    }

    public void setTitle(@StringRes int titleRes) {
        this.titleRes = titleRes;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onResolutionSelected(int width, int height);
    }
}
