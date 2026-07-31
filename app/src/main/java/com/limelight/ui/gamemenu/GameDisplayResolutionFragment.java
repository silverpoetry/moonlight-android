package com.limelight.ui.gamemenu;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Rect;
import android.os.Build;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowMetrics;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.limelight.R;
import com.limelight.settings.stream.CustomResolution;
import com.limelight.settings.stream.CustomResolutionRepository;
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
    private static final int PRESET_COUNT = 6;

    private final Set<CustomResolution> defaultResolutions =
            new HashSet<>();

    private EditText widthInput;
    private EditText heightInput;
    private FlowLayout customResolutionFlow;
    private TextView customResolutionTitle;
    private CustomResolutionRepository repository;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof GameDisplayHost)) {
            throw new IllegalStateException(
                    "Resolution dialog host must implement GameDisplayHost");
        }
        repository =
                ((GameDisplayHost) activity)
                        .getCustomResolutionRepository();
    }

    @Override
    public void onDetach() {
        repository = null;
        super.onDetach();
    }

    @Override
    public int getLayoutRes() {
        return R.layout.dialog_game_menu_display_resolution;
    }

    @Override
    public void bindView(View view) {
        super.bindView(view);
        if (repository == null) {
            throw new IllegalStateException(
                    "Custom resolution repository is required");
        }

        TextView titleView = view.findViewById(R.id.tx_title);
        titleView.setText(R.string.game_menu_resolution);
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
            CustomResolution resolution =
                    CustomResolution.parse(
                            preset.getText().toString());
            if (resolution == null) {
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
        Set<CustomResolution> savedResolutions =
                repository.load();

        boolean hasCustomResolution = false;
        List<CustomResolution> sortedResolutions =
                new ArrayList<>(savedResolutions);
        Collections.sort(sortedResolutions);
        for (CustomResolution resolution :
                sortedResolutions) {
            if (!defaultResolutions.contains(resolution)) {
                addResolutionToFlow(resolution);
                hasCustomResolution = true;
            }
        }
        customResolutionTitle.setVisibility(
                hasCustomResolution ? View.VISIBLE : View.GONE);
    }

    private void addResolutionToFlow(
            CustomResolution resolution) {
        TextView resolutionView = (TextView) LayoutInflater
                .from(getActivity())
                .inflate(
                        R.layout.layout_resolution_item,
                        customResolutionFlow,
                        false);
        resolutionView.setText(resolution.toStorageValue());
        resolutionView.setOnClickListener(
                view -> selectResolution(resolution));
        resolutionView.setOnLongClickListener(view -> {
            showDeleteConfirmDialog(resolution);
            return true;
        });
        customResolutionFlow.addView(resolutionView);
    }

    private void selectResolution(
            CustomResolution resolution) {
        requireTargetListener().onResolutionSelected(
                resolution.getWidth(),
                resolution.getHeight());
        dismiss();
    }

    private void showDeleteConfirmDialog(
            CustomResolution resolution) {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.game_menu_resolution_delete_title)
                .setMessage(getString(
                        R.string.game_menu_resolution_delete_message,
                        resolution.toStorageValue()))
                .setPositiveButton(
                        R.string.game_menu_delete,
                        (dialog, which) ->
                                deleteResolution(resolution))
                .setNegativeButton(
                        R.string.game_menu_customize_cancel, null)
                .show();
    }

    private void deleteResolution(
            CustomResolution resolution) {
        repository.remove(resolution);
        loadCustomResolutions();
    }

    private void saveResolution(int width, int height) {
        CustomResolution resolution =
                new CustomResolution(width, height);
        if (defaultResolutions.contains(resolution)) {
            return;
        }
        repository.add(resolution);
    }

    private String getResolutionText(int width, int height) {
        return getString(
                R.string.game_menu_resolution_format,
                width,
                height);
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
                widthText,
                CustomResolution.MIN_DIMENSION,
                CustomResolution.MAX_DIMENSION);
        Integer height = BoundedIntegerParser.parse(
                heightText,
                CustomResolution.MIN_DIMENSION,
                CustomResolution.MAX_DIMENSION);
        if (width == null || height == null) {
            showToast(R.string.game_menu_resolution_invalid);
            return;
        }

        saveResolution(width, height);
        requireTargetListener().onResolutionSelected(
                width,
                height);
        dismiss();
    }

    private void showToast(@StringRes int messageRes) {
        UiToast.makeText(
                getActivity(), messageRes, UiToast.LENGTH_SHORT).show();
    }

    private Listener requireTargetListener() {
        if (!(getTargetFragment() instanceof Listener)) {
            throw new IllegalStateException(
                    "Resolution dialog target must implement Listener");
        }
        return (Listener) getTargetFragment();
    }

    public interface Listener {
        void onResolutionSelected(int width, int height);
    }
}
