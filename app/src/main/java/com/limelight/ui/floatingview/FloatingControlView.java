package com.limelight.ui.floatingview;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.limelight.R;

/** Visual surface for the in-stream floating control. */
public final class FloatingControlView extends FloatingMagnetView {
    public FloatingControlView(@NonNull Context context) {
        super(context);
        inflate(
                context,
                R.layout.stream_floating_control,
                this);
    }

    public static FrameLayout.LayoutParams
            createDefaultLayoutParams() {
        FrameLayout.LayoutParams parameters =
                new FrameLayout.LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT);
        parameters.gravity = Gravity.TOP | Gravity.START;
        parameters.setMargins(
                8,
                220,
                parameters.rightMargin,
                parameters.bottomMargin);
        return parameters;
    }
}
