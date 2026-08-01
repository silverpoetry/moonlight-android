package com.limelight.ui.gamemenu;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.core.hardware.display.DisplayManagerCompat;
import androidx.fragment.app.Fragment;

import com.limelight.R;
import com.limelight.ui.BaseFragmentDialog.BaseGameMenuDialog;
import com.limelight.utils.UiToast;

public class GameDisplayFpsFragment
        extends BaseGameMenuDialog implements View.OnClickListener {
    private static final String ARG_UNLOCK_FPS = "unlock_fps";
    private static final String RESULT_KEY =
            GameDisplayFpsFragment.class.getName() + ".result";
    private static final String RESULT_FPS = "fps";
    private static final int MIN_FPS = 1;
    private static final int MAX_CUSTOM_FPS = 999;

    private EditText fpsInput;
    private int maxSupportedFps;

    public static GameDisplayFpsFragment newInstance(
            boolean unlockFps) {
        GameDisplayFpsFragment fragment =
                new GameDisplayFpsFragment();
        Bundle arguments = new Bundle();
        arguments.putBoolean(ARG_UNLOCK_FPS, unlockFps);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.dialog_game_menu_display_fps;
    }

    @Override
    public void bindView(View view) {
        super.bindView(view);

        TextView titleView = view.findViewById(R.id.tx_title);
        titleView.setText(R.string.game_menu_fps);
        fpsInput = view.findViewById(R.id.edt_fps);

        view.findViewById(R.id.ibtn_back).setOnClickListener(this);
        view.findViewById(R.id.btn_right).setOnClickListener(this);
        view.findViewById(R.id.bt_display_fps_30)
                .setOnClickListener(this);
        view.findViewById(R.id.bt_display_fps_60)
                .setOnClickListener(this);
        view.findViewById(R.id.bt_display_fps_90)
                .setOnClickListener(this);
        view.findViewById(R.id.bt_display_fps_120)
                .setOnClickListener(this);
        view.findViewById(R.id.bt_display_fps_144)
                .setOnClickListener(this);
        view.findViewById(R.id.bt_display_fps_max)
                .setOnClickListener(this);

        Display display = DisplayManagerCompat
                .getInstance(requireContext())
                .getDisplay(Display.DEFAULT_DISPLAY);
        if (display == null) {
            throw new IllegalStateException(
                    "Default display is unavailable");
        }
        maxSupportedFps = Math.round(display.getRefreshRate());
        Bundle arguments = getArguments();
        boolean unlockFps = arguments != null &&
                arguments.getBoolean(ARG_UNLOCK_FPS, false);
        setVisible(
                view.findViewById(R.id.bt_display_fps_90),
                maxSupportedFps >= 90 || unlockFps);
        setVisible(
                view.findViewById(R.id.bt_display_fps_120),
                maxSupportedFps >= 120 || unlockFps);
        setVisible(
                view.findViewById(R.id.bt_display_fps_144),
                maxSupportedFps >= 144 || unlockFps);

        Button maxFpsButton =
                view.findViewById(R.id.bt_display_fps_max);
        setVisible(maxFpsButton, maxSupportedFps > 144);
        maxFpsButton.setText(getString(
                R.string.game_menu_fps_format,
                maxSupportedFps));
    }

    private static void setVisible(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.ibtn_back) {
            dismiss();
            return;
        }
        if (viewId == R.id.btn_right) {
            selectCustomFps();
            return;
        }

        if (viewId == R.id.bt_display_fps_30) {
            selectFps(30);
        }
        else if (viewId == R.id.bt_display_fps_60) {
            selectFps(60);
        }
        else if (viewId == R.id.bt_display_fps_90) {
            selectFps(90);
        }
        else if (viewId == R.id.bt_display_fps_120) {
            selectFps(120);
        }
        else if (viewId == R.id.bt_display_fps_144) {
            selectFps(144);
        }
        else if (viewId == R.id.bt_display_fps_max) {
            selectFps(maxSupportedFps);
        }
    }

    private void selectCustomFps() {
        String input = fpsInput.getText().toString().trim();
        if (TextUtils.isEmpty(input)) {
            showToast(R.string.game_menu_fps_required);
            return;
        }

        Integer fps = BoundedIntegerParser.parse(
                input, MIN_FPS, MAX_CUSTOM_FPS);
        if (fps == null) {
            showToast(R.string.game_menu_fps_invalid);
            return;
        }
        selectFps(fps);
    }

    private void selectFps(int fps) {
        Bundle result = new Bundle();
        result.putInt(RESULT_FPS, fps);
        getParentFragmentManager().setFragmentResult(
                RESULT_KEY, result);
        dismiss();
    }

    private void showToast(@StringRes int messageRes) {
        UiToast.makeText(
                getActivity(), messageRes, UiToast.LENGTH_SHORT).show();
    }

    public static void registerResultListener(
            Fragment owner,
            Listener listener) {
        owner.getParentFragmentManager()
                .setFragmentResultListener(
                        RESULT_KEY,
                        owner,
                        (requestKey, result) ->
                                listener.onFpsSelected(
                                        result.getInt(RESULT_FPS)));
    }

    public interface Listener {
        void onFpsSelected(int fps);
    }
}
