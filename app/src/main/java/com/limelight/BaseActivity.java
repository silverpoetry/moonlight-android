package com.limelight;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.View;
import android.view.WindowManager;

public class BaseActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decorView = getWindow().getDecorView();
        int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN     // 延伸到状态栏
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION // 延伸到导航栏 (关键！)
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;         // 保持布局稳定
        decorView.setSystemUiVisibility(option);
        // 必须同时将导航栏颜色设为透明，否则可能会有黑色底色
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        // Full-screen
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //填充刘海
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            getWindow().setAttributes(layoutParams);
            getWindow().setDecorFitsSystemWindows(false);
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(layoutParams);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false); // 去除对比度保护
        }
    }
}
