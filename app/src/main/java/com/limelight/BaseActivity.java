package com.limelight;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import android.view.WindowManager;

import com.limelight.settings.android.AndroidAppLocale;

public class BaseActivity extends FragmentActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(
                AndroidAppLocale.wrapBaseContext(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidAppLocale.migrateToPlatformLocales(this);
        if (!shouldEnableEdgeToEdge()) {
            return;
        }
        EdgeToEdge.enable(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            getWindow().setAttributes(layoutParams);
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(layoutParams);
        }
    }

    /** Streaming owns a separate system-bar and coordinate-space policy. */
    protected boolean shouldEnableEdgeToEdge() {
        return true;
    }
}
