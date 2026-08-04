package com.limelight.ui.BaseFragmentDialog;

import android.app.Dialog;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.LayoutRes;
import androidx.activity.ComponentDialog;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.limelight.R;


public abstract class BaseGameMenuFragmentDialog extends DialogFragment {

    private static final String TAG = "base_bottom_dialog";

    private static final float DEFAULT_DIM = 0.3f;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.BottomDialog);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new ComponentDialog(requireContext(), getTheme());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Window window = requireDialog().getWindow();
        if (window == null) {
            throw new IllegalStateException(
                    "Game menu dialog window is unavailable");
        }
        window.requestFeature(Window.FEATURE_NO_TITLE);
        applyImmersiveSystemBars(window);
        requireDialog().setCanceledOnTouchOutside(getCancelOutside());

        View v = inflater.inflate(getLayoutRes(), container, false);
        if (usesLegacyViewChrome()) {
            applyMaterialPanelChrome(v);
            tintLegacyIcons(v);
        }
        bindView(v);
        return v;
    }

    /** Shared Material shell for the remaining view-backed menu sections. */
    private void applyMaterialPanelChrome(View root) {
        root.setBackgroundResource(R.drawable.bg_game_menu_panel);
        if (!(root instanceof LinearLayout)) {
            return;
        }
        View handle = new View(requireContext());
        int width = dpToPixels(32);
        int height = dpToPixels(4);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.topMargin = dpToPixels(4);
        params.bottomMargin = dpToPixels(8);
        handle.setLayoutParams(params);
        android.graphics.drawable.GradientDrawable background =
                new android.graphics.drawable.GradientDrawable();
        background.setColor(ContextCompat.getColor(
                requireContext(),
                R.color.game_menu_material_outline));
        background.setCornerRadius(dpToPixels(2));
        handle.setBackground(background);
        handle.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        ((LinearLayout) root).addView(handle, 0);
    }

    private int dpToPixels(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    /** Compose surfaces own their complete background and icon treatment. */
    protected boolean usesLegacyViewChrome() {
        return true;
    }

    /** Keeps fixed-white legacy vectors legible on the Material light surface. */
    private void tintLegacyIcons(View view) {
        int color = ContextCompat.getColor(
                requireContext(),
                R.color.game_menu_material_on_surface_variant);
        if (view instanceof ImageView) {
            ((ImageView) view).setColorFilter(color);
        } else if (view instanceof TextView) {
            TextView textView = (TextView) view;
            android.graphics.drawable.Drawable[] drawables =
                    textView.getCompoundDrawablesRelative();
            for (android.graphics.drawable.Drawable drawable : drawables) {
                if (drawable != null) {
                    DrawableCompat.setTint(
                            DrawableCompat.wrap(drawable.mutate()),
                            color);
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                tintLegacyIcons(group.getChildAt(index));
            }
        }
    }

    @LayoutRes
    public abstract int getLayoutRes();

    public abstract void bindView(View v);

    @Override
    public void onStart() {
        super.onStart();

        Window window = getDialog().getWindow();
        if (window == null) {
            throw new IllegalStateException(
                    "Game menu dialog window is unavailable");
        }
        applyImmersiveSystemBars(window);
        WindowManager.LayoutParams params = window.getAttributes();

        params.dimAmount = getDimAmount();

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            params.height = WindowManager.LayoutParams.MATCH_PARENT;
            if (getViewSize() > 0) {
                params.width = getViewSize();
            } else {
                params.width = WindowManager.LayoutParams.WRAP_CONTENT;
            }
            params.gravity = Gravity.END;
        }else{
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            if (getViewSize() > 0) {
                params.height = getViewSize();
            } else {
                params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            }
            params.gravity = Gravity.BOTTOM;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = window.getAttributes();
            // 设置为 SHORT_EDGES，允许内容延伸进刘海
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(lp);
        }
        window.setAttributes(params);

    }

    private static void applyImmersiveSystemBars(Window window) {
        WindowCompat.setDecorFitsSystemWindows(window, false);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(
                        window,
                        window.getDecorView());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat
                        .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        controller.hide(WindowInsetsCompat.Type.systemBars());
    }

    public int getViewSize() {
        return -1;
    }

    public float getDimAmount() {
        return DEFAULT_DIM;
    }

    public boolean getCancelOutside() {
        return true;
    }

    public String getFragmentTag() {
        return TAG;
    }

    public void show(FragmentManager fragmentManager) {
        show(fragmentManager, getFragmentTag());
    }
}
