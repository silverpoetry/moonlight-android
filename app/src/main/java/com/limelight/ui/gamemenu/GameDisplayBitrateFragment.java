package com.limelight.ui.gamemenu;

import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.limelight.R;
import com.limelight.ui.BaseFragmentDialog.BaseGameMenuDialog;
import com.limelight.utils.UiToast;

public class GameDisplayBitrateFragment
        extends BaseGameMenuDialog implements View.OnClickListener {
    private static final int MIN_BITRATE_MBPS = 1;
    private static final int MAX_BITRATE_MBPS = 99_999;
    private static final String RESULT_KEY =
            GameDisplayBitrateFragment.class.getName() + ".result";
    private static final String RESULT_BITRATE = "bitrate";
    private static final int[] PRESET_BITRATES_MBPS = {
            20, 50, 60, 100, 150, 200, 300
    };

    private EditText bitrateInput;

    @Override
    public int getLayoutRes() {
        return R.layout.dialog_game_menu_display_bitrate;
    }

    @Override
    public void bindView(View view) {
        super.bindView(view);

        TextView titleView = view.findViewById(R.id.tx_title);
        titleView.setText(R.string.game_menu_bitrate);
        bitrateInput = view.findViewById(R.id.edt_bitrate);

        view.findViewById(R.id.ibtn_back).setOnClickListener(this);
        view.findViewById(R.id.btn_right).setOnClickListener(this);
        for (int index = 0;
             index < PRESET_BITRATES_MBPS.length;
             index++) {
            TextView preset = view.findViewWithTag(
                    Integer.toString(index));
            final int bitrate = PRESET_BITRATES_MBPS[index];
            preset.setOnClickListener(
                    clickedView -> selectBitrate(bitrate));
        }
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

        String input = bitrateInput.getText().toString().trim();
        if (TextUtils.isEmpty(input)) {
            showToast(R.string.game_menu_bitrate_required);
            return;
        }

        Integer bitrate = BoundedIntegerParser.parse(
                input,
                MIN_BITRATE_MBPS,
                MAX_BITRATE_MBPS);
        if (bitrate == null) {
            showToast(R.string.game_menu_bitrate_invalid);
            return;
        }
        selectBitrate(bitrate);
    }

    private void selectBitrate(int bitrate) {
        Bundle result = new Bundle();
        result.putInt(RESULT_BITRATE, bitrate);
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
                                listener.onBitrateSelected(
                                        result.getInt(
                                                RESULT_BITRATE)));
    }

    public interface Listener {
        void onBitrateSelected(int bitrate);
    }
}
