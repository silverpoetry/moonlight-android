package com.limelight.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.GameManager;
import android.app.GameState;
import android.app.UiModeManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.limelight.LimeLog;
import com.limelight.R;
import com.limelight.settings.android.AndroidStreamUiSettingsLoader;

import java.util.Locale;

public class UiHelper {

    private static final int TV_VERTICAL_PADDING_DP = 15;
    private static final int TV_HORIZONTAL_PADDING_DP = 15;

    private static final class ViewPadding {
        final int left;
        final int top;
        final int right;
        final int bottom;

        ViewPadding(View view) {
            left = view.getPaddingLeft();
            top = view.getPaddingTop();
            right = view.getPaddingRight();
            bottom = view.getPaddingBottom();
        }

        void apply(View view, int extraLeft, int extraTop,
                   int extraRight, int extraBottom) {
            view.setPadding(left + extraLeft, top + extraTop,
                    right + extraRight, bottom + extraBottom);
        }
    }

    private static void setGameModeStatus(
            Context context,
            boolean gameModeIntegrationDisabled,
            boolean streaming,
            boolean interruptible) {
        if (gameModeIntegrationDisabled) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                GameManager gameManager = context.getSystemService(GameManager.class);
                if (gameManager == null) {
                    return; // Not supported on this device (e.g., Meta Quest)
                }

                if (streaming) {
                    gameManager.setGameState(new GameState(false, interruptible ? GameState.MODE_GAMEPLAY_INTERRUPTIBLE : GameState.MODE_GAMEPLAY_UNINTERRUPTIBLE));
                } else {
                    gameManager.setGameState(new GameState(false, GameState.MODE_NONE));
                }
            } catch (Throwable t) {
                // Swallow any failure. Some OEM builds ship partial/incompatible GameManager impls.
            }
        }
    }

    public static void notifyStreamConnecting(
            Context context,
            boolean gameModeIntegrationDisabled) {
        setGameModeStatus(
                context,
                gameModeIntegrationDisabled,
                true,
                true);
    }

    public static void notifyStreamConnected(
            Context context,
            boolean gameModeIntegrationDisabled) {
        setGameModeStatus(
                context,
                gameModeIntegrationDisabled,
                true,
                false);
    }

    public static void notifyStreamEnteringPiP(
            Context context,
            boolean gameModeIntegrationDisabled) {
        setGameModeStatus(
                context,
                gameModeIntegrationDisabled,
                true,
                true);
    }

    public static void notifyStreamExitingPiP(
            Context context,
            boolean gameModeIntegrationDisabled) {
        setGameModeStatus(
                context,
                gameModeIntegrationDisabled,
                true,
                false);
    }

    public static void notifyStreamEnded(
            Context context,
            boolean gameModeIntegrationDisabled) {
        setGameModeStatus(
                context,
                gameModeIntegrationDisabled,
                false,
                false);
    }

    public static boolean isColorOS() {
        String manufacturer = String.valueOf(Build.MANUFACTURER).toLowerCase(Locale.US);
        String brand = String.valueOf(Build.BRAND).toLowerCase(Locale.US);
        String model = String.valueOf(Build.MODEL).toLowerCase(Locale.US);

        return manufacturer.contains("oppo") || brand.contains("oppo") || model.contains("oppo") ||
                manufacturer.contains("oneplus") || brand.contains("oneplus") || model.contains("oneplus") ||
                manufacturer.contains("realme") || brand.contains("realme") || model.contains("realme");
    }

    public static void notifyHdrWindowStatus(
            final Activity activity,
            final boolean hdrEnabled,
            final boolean hdrHighBrightnessEnabled) {
        if (activity == null) {
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean colorOs = isColorOS();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && colorOs) {
                        activity.getWindow().setColorMode(hdrEnabled
                                ? ActivityInfo.COLOR_MODE_HDR
                                : ActivityInfo.COLOR_MODE_DEFAULT);
                    }

                    WindowManager.LayoutParams params = activity.getWindow().getAttributes();
                    params.screenBrightness = hdrEnabled &&
                            hdrHighBrightnessEnabled
                            ? WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                            : WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
                    activity.getWindow().setAttributes(params);

                    LimeLog.info("HDR window status updated: enabled=" + hdrEnabled
                            + ", highBrightness=" +
                            hdrHighBrightnessEnabled
                            + ", colorOs=" + colorOs);
                } catch (Throwable t) {
                    LimeLog.warning("Failed to update HDR window status: " + t.getMessage());
                }
            }
        });
    }

    public static void applyStatusBarPadding(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // This applies the bottom safe area intentionally omitted from the content
            // root by notifyNewRootView(), while preserving the view's own padding.
            final int initialLeft = view.getPaddingLeft();
            final int initialTop = view.getPaddingTop();
            final int initialRight = view.getPaddingRight();
            final int initialBottom = view.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(
                    view,
                    (target, windowInsets) -> {
                        int bottomInset = getSafeContentInsets(
                                windowInsets).bottom;
                        target.setPadding(initialLeft,
                                initialTop,
                                initialRight,
                                initialBottom + bottomInset);
                        return windowInsets;
                    });
            ViewCompat.requestApplyInsets(view);
        }
    }

    private static WindowInsetsPolicy.EdgeInsets getSafeContentInsets(
            WindowInsetsCompat windowInsets) {
        WindowInsetsPolicy.EdgeInsets systemBars =
                toEdgeInsets(windowInsets.getInsets(
                        WindowInsetsCompat.Type.systemBars()));
        WindowInsetsPolicy.EdgeInsets displayCutout =
                toEdgeInsets(windowInsets.getInsets(
                        WindowInsetsCompat.Type.displayCutout()));
        return WindowInsetsPolicy.resolveSafeContentInsets(systemBars, displayCutout);
    }

    private static WindowInsetsPolicy.EdgeInsets getTappableInsets(
            WindowInsetsCompat windowInsets) {
        return toEdgeInsets(windowInsets.getInsets(
                WindowInsetsCompat.Type.tappableElement()));
    }

    private static WindowInsetsPolicy.EdgeInsets toEdgeInsets(Insets insets) {
        return new WindowInsetsPolicy.EdgeInsets(
                insets.left, insets.top, insets.right, insets.bottom);
    }

    private static void configureNonStreamingCutoutMode(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams layoutParams = activity.getWindow().getAttributes();
            layoutParams.layoutInDisplayCutoutMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                    ? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    : WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            activity.getWindow().setAttributes(layoutParams);
        }
    }

    private static UiModeManager prepareNonStreamingWindow(Activity activity) {
        setGameModeStatus(
                activity,
                AndroidStreamUiSettingsLoader.load(activity)
                        .isGameModeIntegrationDisabled(),
                false,
                false);
        configureNonStreamingCutoutMode(activity);
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity.getWindow().setNavigationBarContrastEnforced(false);
        }
        return (UiModeManager) activity.getSystemService(Context.UI_MODE_SERVICE);
    }

    private static boolean applyTelevisionPadding(Activity activity,
                                                  View rootView,
                                                  UiModeManager modeManager) {
        if (modeManager.getCurrentModeType() !=
                Configuration.UI_MODE_TYPE_TELEVISION) {
            return false;
        }

        float scale = activity.getResources().getDisplayMetrics().density;
        int verticalPaddingPixels =
                (int) (TV_VERTICAL_PADDING_DP * scale + 0.5f);
        int horizontalPaddingPixels =
                (int) (TV_HORIZONTAL_PADDING_DP * scale + 0.5f);
        rootView.setPadding(horizontalPaddingPixels, verticalPaddingPixels,
                horizontalPaddingPixels, verticalPaddingPixels);
        return true;
    }

    private static void updateNavigationBarScrim(Activity activity,
                                                  int tappableBottom) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity.getWindow().setNavigationBarContrastEnforced(
                    tappableBottom != 0);
        }
    }

    /**
     * Configures the stream content rectangle for Android 15's enforced edge-to-edge mode.
     *
     * Insets are applied only to the stream content coordinate space. The activity
     * root remains edge-to-edge so full-window input overlays can use physical display
     * coordinates independently of the stream's cutout policy.
     */
    public static void configureStreamWindowInsets(final Activity activity,
                                                   final boolean allowDisplayCutout,
                                                   final View streamContentRoot) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && allowDisplayCutout) {
            WindowManager.LayoutParams layoutParams = activity.getWindow().getAttributes();
            layoutParams.layoutInDisplayCutoutMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                    ? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    : WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            activity.getWindow().setAttributes(layoutParams);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return;
        }

        WindowCompat.setDecorFitsSystemWindows(
                activity.getWindow(),
                false);
        final View contentView = activity.findViewById(android.R.id.content);
        contentView.setPadding(0, 0, 0, 0);
        ViewCompat.setOnApplyWindowInsetsListener(
                contentView,
                (view, windowInsets) -> {
                    WindowInsetsPolicy.EdgeInsets systemBars =
                            toEdgeInsets(windowInsets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()));
                    WindowInsetsPolicy.EdgeInsets displayCutout =
                            toEdgeInsets(windowInsets.getInsets(
                                    WindowInsetsCompat.Type.displayCutout()));
                    WindowInsetsPolicy.EdgeInsets contentInsets =
                            WindowInsetsPolicy.resolveStreamInsets(
                                    activity.isInMultiWindowMode(),
                                    allowDisplayCutout,
                                    systemBars,
                                    displayCutout);
                    streamContentRoot.setPadding(
                            contentInsets.left, contentInsets.top,
                            contentInsets.right, contentInsets.bottom);
                    return windowInsets;
                });
        ViewCompat.requestApplyInsets(contentView);
    }

    public static void refreshStreamWindowInsets(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            ViewCompat.requestApplyInsets(
                    activity.findViewById(android.R.id.content));
        }
    }

    public static void notifyNewRootView(final Activity activity)
    {
        View rootView = activity.findViewById(android.R.id.content);
        UiModeManager modeManager = prepareNonStreamingWindow(activity);
        if (applyTelevisionPadding(activity, rootView, modeManager) ||
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }

        final ViewPadding initialPadding = new ViewPadding(rootView);
        ViewCompat.setOnApplyWindowInsetsListener(
                rootView,
                (view, windowInsets) -> {
                    WindowInsetsPolicy.EdgeInsets contentInsets =
                            getSafeContentInsets(windowInsets);
                    int tappableBottom =
                            getTappableInsets(windowInsets).bottom;
                    initialPadding.apply(view, contentInsets.left,
                            contentInsets.top, contentInsets.right, 0);
                    updateNavigationBarScrim(activity, tappableBottom);

                    return windowInsets;
                });
        ViewCompat.requestApplyInsets(rootView);
    }

    /**
     * Configures an edge-to-edge non-streaming screen whose background should fill
     * the physical display while its foreground controls remain inside safe areas.
     *
     * The content root itself is deliberately never inset. Applying safe-area
     * padding there would also move the screen background and expose the decor
     * behind a landscape display cutout. Instead, the header and body receive
     * their applicable insets from their immutable layout padding.
     */
    public static void notifyNewEdgeToEdgeRootView(final Activity activity,
                                                   int headerViewId,
                                                   int bodyViewId) {
        final View contentRoot = activity.findViewById(android.R.id.content);
        final View headerView = activity.findViewById(headerViewId);
        final View bodyView = activity.findViewById(bodyViewId);
        if (headerView == null || bodyView == null) {
            throw new IllegalArgumentException(
                    "Edge-to-edge foreground views must exist in the activity layout");
        }

        UiModeManager modeManager = prepareNonStreamingWindow(activity);
        if (applyTelevisionPadding(activity, contentRoot, modeManager) ||
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }

        // android.R.id.content survives setContentView(), so clear any inset padding
        // left by an earlier layout before installing the new foreground policy.
        contentRoot.setPadding(0, 0, 0, 0);
        final ViewPadding headerPadding = new ViewPadding(headerView);
        final ViewPadding bodyPadding = new ViewPadding(bodyView);
        ViewCompat.setOnApplyWindowInsetsListener(
                contentRoot,
                (view, windowInsets) -> {
                    WindowInsetsPolicy.EdgeInsets contentInsets =
                            getSafeContentInsets(windowInsets);
                    int tappableBottom =
                            getTappableInsets(windowInsets).bottom;

                    headerPadding.apply(headerView,
                            contentInsets.left, contentInsets.top,
                            contentInsets.right, 0);
                    bodyPadding.apply(bodyView,
                            contentInsets.left, 0,
                            contentInsets.right, contentInsets.bottom);
                    updateNavigationBarScrim(activity, tappableBottom);
                    return windowInsets;
                });
        ViewCompat.requestApplyInsets(contentRoot);
    }

    public static void displayQuitConfirmationDialog(Activity parent, final Runnable onYes, final Runnable onNo) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        if (onYes != null) {
                            onYes.run();
                        }
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        if (onNo != null) {
                            onNo.run();
                        }
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(parent);
        builder.setMessage(parent.getResources().getString(R.string.applist_quit_confirmation))
                .setPositiveButton(parent.getResources().getString(R.string.yes), dialogClickListener)
                .setNegativeButton(parent.getResources().getString(R.string.no), dialogClickListener)
                .show();
    }

    public static void displayDeletePcConfirmationDialog(
            Activity parent,
            CharSequence hostName,
            final Runnable onYes,
            final Runnable onNo) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        if (onYes != null) {
                            onYes.run();
                        }
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        if (onNo != null) {
                            onNo.run();
                        }
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(parent);
        builder.setMessage(parent.getResources().getString(R.string.delete_pc_msg))
                .setTitle(hostName)
                .setPositiveButton(parent.getResources().getString(R.string.yes), dialogClickListener)
                .setNegativeButton(parent.getResources().getString(R.string.no), dialogClickListener)
                .show();
    }

    public static int dpToPx(Context context, float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static void setStatusBarLightMode(@NonNull final Window window,
                                             final boolean isLightMode) {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(
                        window,
                        window.getDecorView());
        controller.setAppearanceLightStatusBars(isLightMode);
    }

}
