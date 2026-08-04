package com.limelight.preferences;

import android.content.Context;

import com.limelight.R;

import java.util.Objects;

/** Android resource adapter for display capability labels. */
final class AndroidSettingsDisplayText implements SettingsDisplayText {
    private final Context context;

    AndroidSettingsDisplayText(Context context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public CharSequence customResolutionName(
            SettingsDisplayPolicy.ResolutionOption option) {
        return context.getString(
                R.string.settings_custom_resolution_entry,
                option.getWidth(),
                option.getHeight());
    }

    @Override
    public CharSequence nativeResolutionName(
            SettingsDisplayPolicy.ResolutionOption option) {
        StringBuilder name = new StringBuilder(
                context.getString(option.isFullscreen()
                        ? R.string.resolution_prefix_native_fullscreen
                        : R.string.resolution_prefix_native));
        switch (option.getOrientationLabel()) {
            case PORTRAIT:
                name.append(' ').append(context.getString(
                        R.string.resolution_prefix_native_portrait));
                break;
            case LANDSCAPE:
                name.append(' ').append(context.getString(
                        R.string.resolution_prefix_native_landscape));
                break;
            case NONE:
                break;
            default:
                throw new AssertionError(
                        "Unhandled native resolution orientation");
        }
        return name.append(" (")
                .append(option.getWidth())
                .append('x')
                .append(option.getHeight())
                .append(')');
    }

    @Override
    public CharSequence nativeFrameRateName(String frameRateValue) {
        return context.getString(R.string.resolution_prefix_native) +
                " (" + frameRateValue + " " +
                context.getString(R.string.fps_suffix_fps) + ")";
    }

    @Override
    public CharSequence hdrFirmwareRequired() {
        return context.getString(R.string.settings_hdr_firmware_required);
    }
}
