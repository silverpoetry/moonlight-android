package com.limelight.ui.gamemenu;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.limelight.R;
import com.limelight.settings.audio.StreamAudioSettings;
import com.limelight.settings.audio.StreamAudioSettingsUpdate;
import com.limelight.settings.stream.StreamDecoderSettings.VideoFormat;
import com.limelight.settings.stream.StreamVideoSettings;
import com.limelight.settings.stream.StreamVideoSettings.ScreenOnPolicy;
import com.limelight.settings.stream.StreamVideoSettings.VirtualDisplayMode;
import com.limelight.settings.stream.StreamVideoSettingsUpdate;
import com.limelight.ui.BaseFragmentDialog.BaseGameMenuDialog;
import com.limelight.utils.UiHelper;
import com.limelight.utils.UiToast;

/**
 * Edits the next stream's video configuration through typed intents.
 *
 * <p>Resolution, FPS, bitrate, orientation, and external-display mode form
 * one explicit Apply transaction. Independent radio settings preserve
 * their historical immediate-persistence behavior.</p>
 */
public final class GameDisplayFragment
        extends BaseGameMenuDialog
        implements View.OnClickListener,
        GameDisplayResolutionFragment.Listener,
        GameDisplayBitrateFragment.Listener,
        GameDisplayFpsFragment.Listener {
    private static final String ARG_SHOW_LOCK = "show_lock";

    private boolean showLock;
    private GameMenuHostProvider hostProvider;
    private GameDisplayHost host;
    private StreamVideoSettings videoSettings;
    private StreamVideoSettings draft;
    private StreamAudioSettings audioSettings;
    private TextView resolutionSummary;
    private TextView bitrateSummary;
    private TextView fpsSummary;
    private TextView orientationSummary;
    private TextView externalDisplaySummary;
    private RadioGroup screenOnPolicy;
    private RadioGroup videoFormat;
    private RadioGroup playHostAudio;
    private RadioGroup hdr;
    private RadioGroup virtualDisplayMode;
    private RadioGroup enforceDisplayMode;
    private RadioGroup lowLatency;
    private RadioGroup ignoreHdrCapability;
    private View hdrHighBrightnessContainer;
    private RadioGroup hdrHighBrightness;

    public static GameDisplayFragment newInstance(
            boolean showLock) {
        GameDisplayFragment fragment =
                new GameDisplayFragment();
        Bundle arguments = new Bundle();
        arguments.putBoolean(ARG_SHOW_LOCK, showLock);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof GameMenuHostProvider) {
            hostProvider = (GameMenuHostProvider) activity;
        }
        else if (activity instanceof GameDisplayHost) {
            host = (GameDisplayHost) activity;
        }
        else {
            throw new IllegalStateException(
                    "GameDisplayFragment host must provide " +
                            "GameMenuHost or implement GameDisplayHost");
        }
    }

    @Override
    public void onDetach() {
        host = null;
        hostProvider = null;
        videoSettings = null;
        draft = null;
        audioSettings = null;
        super.onDetach();
    }

    @Override
    public int getLayoutRes() {
        return R.layout.dialog_game_menu_display;
    }

    @Override
    public void bindView(View view) {
        super.bindView(view);
        if (hostProvider != null) {
            host = hostProvider.getGameMenuHost();
        }
        requireHost();
        Bundle arguments = getArguments();
        showLock = arguments == null ||
                arguments.getBoolean(ARG_SHOW_LOCK, true);
        videoSettings = host.getStreamVideoSettings();
        draft = videoSettings;
        audioSettings = host.getStreamAudioSettings();
        bindControls(view);
        ((TextView) view.findViewById(R.id.tx_title))
                .setText(R.string.game_menu_display_title);
        ((Button) view.findViewById(R.id.btn_right))
                .setText(R.string.game_menu_apply_configuration);
        view.findViewById(R.id.lv_display_lock)
                .setVisibility(
                        showLock ? View.VISIBLE : View.GONE);
        renderAll();
        bindListeners(view);
    }

    private void requireHost() {
        if (host == null) {
            throw new IllegalStateException(
                    "Display settings host is not attached");
        }
    }

    private void bindControls(View view) {
        resolutionSummary =
                view.findViewById(R.id.tx_game_display_screen);
        bitrateSummary =
                view.findViewById(R.id.tx_game_display_bit);
        fpsSummary =
                view.findViewById(R.id.tx_game_display_fps);
        orientationSummary =
                view.findViewById(
                        R.id.tx_game_display_direction);
        externalDisplaySummary =
                view.findViewById(R.id.tx_game_display_ex);
        screenOnPolicy =
                view.findViewById(R.id.rg_game_display_lock);
        videoFormat =
                view.findViewById(
                        R.id.rg_game_display_video_format);
        playHostAudio =
                view.findViewById(R.id.rg_game_display_audio);
        hdr = view.findViewById(R.id.rg_game_display_hdr);
        virtualDisplayMode =
                view.findViewById(R.id.rg_game_display_vd);
        enforceDisplayMode =
                view.findViewById(
                        R.id.rg_game_display_enforce);
        lowLatency =
                view.findViewById(
                        R.id.rg_game_display_lowlatency);
        ignoreHdrCapability =
                view.findViewById(
                        R.id.rg_game_display_ignore_hdr);
        hdrHighBrightnessContainer =
                view.findViewById(
                        R.id.v_game_display_hdr_high_brightness);
        hdrHighBrightness =
                view.findViewById(
                        R.id.rg_game_display_hdr_high_brightness);
    }

    private void renderAll() {
        renderDraftSummaries();
        renderScreenOnPolicy();
        renderVideoFormat();
        playHostAudio.check(
                audioSettings.shouldPlayHostAudio()
                        ? R.id.rbt_game_display_audio_2
                        : R.id.rbt_game_display_audio_1);
        hdr.check(
                videoSettings.isHdrEnabled()
                        ? R.id.rbt_game_display_hdr_1
                        : R.id.rbt_game_display_hdr_2);
        hdrHighBrightness.check(
                videoSettings.isHdrHighBrightnessEnabled()
                        ? R.id.rbt_game_display_hdr_high_brightness_1
                        : R.id.rbt_game_display_hdr_high_brightness_2);
        updateHdrHighBrightnessVisibility(
                videoSettings.isHdrEnabled());
        ignoreHdrCapability.check(
                videoSettings.shouldIgnoreHdrCapability()
                        ? R.id.rbt_game_display_ignore_hdr_1
                        : R.id.rbt_game_display_ignore_hdr_2);
        lowLatency.check(
                videoSettings
                        .isLowLatencyExperimentEnabled()
                        ? R.id.rbt_game_display_lowlatency_1
                        : R.id.rbt_game_display_lowlatency_2);
        enforceDisplayMode.check(
                videoSettings.shouldEnforceDisplayMode()
                        ? R.id.rbt_game_display_enforce_1
                        : R.id.rbt_game_display_enforce_2);
        renderVirtualDisplayMode();
    }

    private void renderDraftSummaries() {
        resolutionSummary.setText(getString(
                R.string.game_menu_resolution_summary,
                draft.getWidth(),
                draft.getHeight()));
        bitrateSummary.setText(getString(
                R.string.game_menu_bitrate_summary,
                draft.getBitrateKbps() / 1000));
        fpsSummary.setText(getString(
                R.string.game_menu_fps_summary,
                draft.getFps()));
        orientationSummary.setText(getString(
                R.string.game_menu_direction_summary,
                getString(
                        draft.isPortrait()
                                ? R.string
                                        .game_menu_orientation_portrait
                                : R.string
                                        .game_menu_orientation_landscape)));
        externalDisplaySummary.setText(getString(
                R.string.game_menu_mode_summary,
                getString(
                        draft.isExternalDisplay()
                                ? R.string
                                        .game_menu_display_mode_external
                                : R.string
                                        .game_menu_display_mode_normal)));
    }

    private void renderScreenOnPolicy() {
        int id;
        switch (videoSettings.getScreenOnPolicy()) {
            case CURRENT_SESSION:
                id = R.id.rbt_game_display_lock_2;
                break;
            case ALWAYS:
                id = R.id.rbt_game_display_lock_3;
                break;
            case DISABLED:
            default:
                id = R.id.rbt_game_display_lock_1;
                break;
        }
        screenOnPolicy.check(id);
    }

    private void renderVideoFormat() {
        int id;
        switch (videoSettings.getVideoFormat()) {
            case FORCE_H264:
                id = R.id.rbt_game_display_video_format_2;
                break;
            case FORCE_HEVC:
                id = R.id.rbt_game_display_video_format_3;
                break;
            case FORCE_AV1:
                id = R.id.rbt_game_display_video_format_4;
                break;
            case AUTO:
            default:
                id = R.id.rbt_game_display_video_format_1;
                break;
        }
        videoFormat.check(id);
    }

    private void renderVirtualDisplayMode() {
        int id;
        switch (videoSettings.getVirtualDisplayMode()) {
            case EXTENDED:
                id = R.id.rbt_game_display_vd_2;
                break;
            case VIRTUAL_ONLY:
                id = R.id.rbt_game_display_vd_3;
                break;
            case DISABLED:
            default:
                id = R.id.rbt_game_display_vd_1;
                break;
        }
        virtualDisplayMode.check(id);
    }

    private void updateHdrHighBrightnessVisibility(
            boolean hdrEnabled) {
        hdrHighBrightnessContainer.setVisibility(
                hdrEnabled ? View.VISIBLE : View.GONE);
    }

    private void bindListeners(View view) {
        view.findViewById(R.id.ibtn_back)
                .setOnClickListener(this);
        view.findViewById(R.id.btn_right)
                .setOnClickListener(this);
        view.findViewById(R.id.bt_display_screen)
                .setOnClickListener(this);
        view.findViewById(R.id.bt_display_exchange)
                .setOnClickListener(this);
        view.findViewById(R.id.bt_display_direction)
                .setOnClickListener(this);
        view.findViewById(R.id.bt_display_bitrate)
                .setOnClickListener(this);
        view.findViewById(R.id.bt_display_fps)
                .setOnClickListener(this);
        view.findViewById(R.id.bt_display_ex)
                .setOnClickListener(this);

        screenOnPolicy.setOnCheckedChangeListener(
                (group, checkedId) -> {
                    ScreenOnPolicy policy;
                    if (checkedId ==
                            R.id.rbt_game_display_lock_2) {
                        policy = ScreenOnPolicy.CURRENT_SESSION;
                    }
                    else if (checkedId ==
                            R.id.rbt_game_display_lock_3) {
                        policy = ScreenOnPolicy.ALWAYS;
                    }
                    else if (checkedId ==
                            R.id.rbt_game_display_lock_1) {
                        policy = ScreenOnPolicy.DISABLED;
                    }
                    else {
                        return;
                    }
                    dispatchVideo(
                            StreamVideoSettingsUpdate
                                    .screenOnPolicy(policy));
                    dismiss();
                });
        videoFormat.setOnCheckedChangeListener(
                (group, checkedId) -> {
                    VideoFormat format;
                    if (checkedId ==
                            R.id.rbt_game_display_video_format_2) {
                        format = VideoFormat.FORCE_H264;
                    }
                    else if (checkedId ==
                            R.id.rbt_game_display_video_format_3) {
                        format = VideoFormat.FORCE_HEVC;
                    }
                    else if (checkedId ==
                            R.id.rbt_game_display_video_format_4) {
                        format = VideoFormat.FORCE_AV1;
                    }
                    else if (checkedId ==
                            R.id.rbt_game_display_video_format_1) {
                        format = VideoFormat.AUTO;
                    }
                    else {
                        return;
                    }
                    dispatchVideo(
                            StreamVideoSettingsUpdate
                                    .videoFormat(format));
                });
        playHostAudio.setOnCheckedChangeListener(
                (group, checkedId) -> {
                    if (checkedId ==
                            R.id.rbt_game_display_audio_1) {
                        dispatchAudio(
                                StreamAudioSettingsUpdate
                                        .playHostAudio(false));
                    }
                    else if (checkedId ==
                            R.id.rbt_game_display_audio_2) {
                        dispatchAudio(
                                StreamAudioSettingsUpdate
                                        .playHostAudio(true));
                    }
                });
        hdr.setOnCheckedChangeListener(
                (group, checkedId) -> {
                    if (checkedId ==
                            R.id.rbt_game_display_hdr_1) {
                        dispatchVideo(
                                StreamVideoSettingsUpdate
                                        .hdrEnabled(true));
                        updateHdrHighBrightnessVisibility(true);
                    }
                    else if (checkedId ==
                            R.id.rbt_game_display_hdr_2) {
                        dispatchVideo(
                                StreamVideoSettingsUpdate
                                        .hdrEnabled(false));
                        updateHdrHighBrightnessVisibility(false);
                    }
                });
        virtualDisplayMode.setOnCheckedChangeListener(
                (group, checkedId) -> {
                    VirtualDisplayMode mode;
                    if (checkedId ==
                            R.id.rbt_game_display_vd_2) {
                        mode = VirtualDisplayMode.EXTENDED;
                    }
                    else if (checkedId ==
                            R.id.rbt_game_display_vd_3) {
                        mode = VirtualDisplayMode.VIRTUAL_ONLY;
                    }
                    else if (checkedId ==
                            R.id.rbt_game_display_vd_1) {
                        mode = VirtualDisplayMode.DISABLED;
                    }
                    else {
                        return;
                    }
                    dispatchVideo(
                            StreamVideoSettingsUpdate
                                    .virtualDisplayMode(mode));
                });
        enforceDisplayMode.setOnCheckedChangeListener(
                (group, checkedId) -> {
                    if (checkedId ==
                            R.id.rbt_game_display_enforce_1) {
                        dispatchVideo(
                                StreamVideoSettingsUpdate
                                        .enforceDisplayMode(true));
                    }
                    else if (checkedId ==
                            R.id.rbt_game_display_enforce_2) {
                        dispatchVideo(
                                StreamVideoSettingsUpdate
                                        .enforceDisplayMode(false));
                    }
                });
        lowLatency.setOnCheckedChangeListener(
                (group, checkedId) -> {
                    if (checkedId ==
                            R.id.rbt_game_display_lowlatency_1) {
                        dispatchVideo(
                                StreamVideoSettingsUpdate
                                        .lowLatencyExperimentEnabled(
                                                true));
                    }
                    else if (checkedId ==
                            R.id.rbt_game_display_lowlatency_2) {
                        dispatchVideo(
                                StreamVideoSettingsUpdate
                                        .lowLatencyExperimentEnabled(
                                                false));
                    }
                });
        ignoreHdrCapability.setOnCheckedChangeListener(
                (group, checkedId) -> {
                    if (checkedId ==
                            R.id.rbt_game_display_ignore_hdr_1) {
                        dispatchVideo(
                                StreamVideoSettingsUpdate
                                        .ignoreHdrCapability(true));
                    }
                    else if (checkedId ==
                            R.id.rbt_game_display_ignore_hdr_2) {
                        dispatchVideo(
                                StreamVideoSettingsUpdate
                                        .ignoreHdrCapability(false));
                    }
                });
        hdrHighBrightness.setOnCheckedChangeListener(
                (group, checkedId) -> {
                    if (checkedId ==
                            R.id.rbt_game_display_hdr_high_brightness_1) {
                        dispatchVideo(
                                StreamVideoSettingsUpdate
                                        .hdrHighBrightness(true));
                    }
                    else if (checkedId ==
                            R.id.rbt_game_display_hdr_high_brightness_2) {
                        dispatchVideo(
                                StreamVideoSettingsUpdate
                                        .hdrHighBrightness(false));
                    }
                });
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.ibtn_back) {
            dismiss();
            return;
        }
        if (id == R.id.btn_right) {
            applyDisplayConfiguration();
            return;
        }
        if (id == R.id.bt_display_screen) {
            showResolutionDialog();
            return;
        }
        if (id == R.id.bt_display_exchange) {
            draft = draft.toBuilder()
                    .setDimensions(
                            draft.getHeight(),
                            draft.getWidth())
                    .build();
            renderDraftSummaries();
            return;
        }
        if (id == R.id.bt_display_direction) {
            draft = draft.toBuilder()
                    .setPortrait(!draft.isPortrait())
                    .build();
            renderDraftSummaries();
            return;
        }
        if (id == R.id.bt_display_bitrate) {
            showBitrateDialog();
            return;
        }
        if (id == R.id.bt_display_fps) {
            showFpsDialog();
            return;
        }
        if (id == R.id.bt_display_ex) {
            draft = draft.toBuilder()
                    .setExternalDisplay(
                            !draft.isExternalDisplay())
                    .build();
            renderDraftSummaries();
        }
    }

    private void applyDisplayConfiguration() {
        if (draft.getWidth() <= 0 ||
                draft.getHeight() <= 0 ||
                draft.getBitrateKbps() <= 0 ||
                draft.getFps() <= 0) {
            UiToast.makeText(
                    getActivity(),
                    R.string.game_menu_invalid_display_configuration,
                    UiToast.LENGTH_SHORT)
                    .show();
            return;
        }
        dispatchVideo(
                StreamVideoSettingsUpdate
                        .displayConfiguration(draft));
        dismiss();
        host.onDisplayConfigurationApplied();
    }

    private void showResolutionDialog() {
        GameDisplayResolutionFragment fragment =
                new GameDisplayResolutionFragment();
        fragment.setWidth(
                UiHelper.dpToPx(getActivity(), 364));
        fragment.setTargetFragment(this, 0);
        fragment.show(getFragmentManager());
    }

    private void showBitrateDialog() {
        GameDisplayBitrateFragment fragment =
                new GameDisplayBitrateFragment();
        fragment.setWidth(
                UiHelper.dpToPx(getActivity(), 364));
        fragment.setTargetFragment(this, 0);
        fragment.show(getFragmentManager());
    }

    private void showFpsDialog() {
        GameDisplayFpsFragment fragment =
                GameDisplayFpsFragment.newInstance(
                        videoSettings.isFpsUnlocked());
        fragment.setWidth(
                UiHelper.dpToPx(getActivity(), 364));
        fragment.setTargetFragment(this, 0);
        fragment.show(getFragmentManager());
    }

    private void dispatchVideo(
            StreamVideoSettingsUpdate update) {
        videoSettings = update.applyTo(videoSettings);
        draft = update.applyTo(draft);
        host.applyStreamVideoSettingsUpdate(update);
    }

    private void dispatchAudio(
            StreamAudioSettingsUpdate update) {
        audioSettings = update.applyTo(audioSettings);
        host.applyStreamAudioSettingsUpdate(update);
    }

    @Override
    public void onResolutionSelected(int width, int height) {
        draft = draft.toBuilder()
                .setDimensions(width, height)
                .build();
        renderDraftSummaries();
    }

    @Override
    public void onBitrateSelected(int bitrateMbps) {
        draft = draft.toBuilder()
                .setBitrateKbps(bitrateMbps * 1000)
                .build();
        renderDraftSummaries();
    }

    @Override
    public void onFpsSelected(int fps) {
        draft = draft.toBuilder()
                .setFps(fps)
                .build();
        renderDraftSummaries();
    }
}
