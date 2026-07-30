package com.limelight.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.GameManager;
import android.app.GameState;
import android.app.LocaleManager;
import android.app.UiModeManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.os.Build;
import android.os.LocaleList;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.view.WindowCompat;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;

import com.limelight.LimeLog;
import com.limelight.R;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.preferences.PreferenceConfiguration;

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

    private static void setGameModeStatus(Context context, boolean streaming, boolean interruptible) {
        //禁用游戏模式
        if(PreferenceConfiguration.readPreferences(context).enableGameManagerQuest){
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

    public static void notifyStreamConnecting(Context context) {
        setGameModeStatus(context, true, true);
    }

    public static void notifyStreamConnected(Context context) {
        setGameModeStatus(context, true, false);
    }

    public static void notifyStreamEnteringPiP(Context context) {
        setGameModeStatus(context, true, true);
    }

    public static void notifyStreamExitingPiP(Context context) {
        setGameModeStatus(context, true, false);
    }

    public static void notifyStreamEnded(Context context) {
        setGameModeStatus(context, false, false);
    }

    public static boolean isColorOS() {
        String manufacturer = String.valueOf(Build.MANUFACTURER).toLowerCase(Locale.US);
        String brand = String.valueOf(Build.BRAND).toLowerCase(Locale.US);
        String model = String.valueOf(Build.MODEL).toLowerCase(Locale.US);

        return manufacturer.contains("oppo") || brand.contains("oppo") || model.contains("oppo") ||
                manufacturer.contains("oneplus") || brand.contains("oneplus") || model.contains("oneplus") ||
                manufacturer.contains("realme") || brand.contains("realme") || model.contains("realme");
    }

    public static void notifyHdrWindowStatus(final Activity activity, final boolean hdrEnabled) {
        if (activity == null) {
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean colorOs = isColorOS();
                    boolean enableHdrHighBrightness = PreferenceConfiguration.readPreferences(activity).enableHdrHighBrightness;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && colorOs) {
                        activity.getWindow().setColorMode(hdrEnabled
                                ? ActivityInfo.COLOR_MODE_HDR
                                : ActivityInfo.COLOR_MODE_DEFAULT);
                    }

                    WindowManager.LayoutParams params = activity.getWindow().getAttributes();
                    params.screenBrightness = hdrEnabled && enableHdrHighBrightness
                            ? WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                            : WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
                    activity.getWindow().setAttributes(params);

                    LimeLog.info("HDR window status updated: enabled=" + hdrEnabled
                            + ", highBrightness=" + enableHdrHighBrightness
                            + ", colorOs=" + colorOs);
                } catch (Throwable t) {
                    LimeLog.warning("Failed to update HDR window status: " + t.getMessage());
                }
            }
        });
    }

    public static void setLocale(Activity activity)
    {
        String locale = PreferenceConfiguration.readPreferences(activity).language;
        if (!locale.equals(PreferenceConfiguration.DEFAULT_LANGUAGE)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // On Android 13, migrate this non-default language setting into the OS native API
                LocaleManager localeManager = activity.getSystemService(LocaleManager.class);
                localeManager.setApplicationLocales(LocaleList.forLanguageTags(locale));
                PreferenceConfiguration.completeLanguagePreferenceMigration(activity);
            }
            else {
                Configuration config = new Configuration(activity.getResources().getConfiguration());

                // Some locales include both language and country which must be separated
                // before calling the Locale constructor.
                if (locale.contains("-"))
                {
                    config.locale = new Locale(locale.substring(0, locale.indexOf('-')),
                            locale.substring(locale.indexOf('-') + 1));
                }
                else
                {
                    config.locale = new Locale(locale);
                }

                activity.getResources().updateConfiguration(config, activity.getResources().getDisplayMetrics());
            }
        }
    }

    public static void applyStatusBarPadding(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // This applies the bottom safe area intentionally omitted from the content
            // root by notifyNewRootView(), while preserving the view's own padding.
            final int initialLeft = view.getPaddingLeft();
            final int initialTop = view.getPaddingTop();
            final int initialRight = view.getPaddingRight();
            final int initialBottom = view.getPaddingBottom();
            view.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                    int bottomInset;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        bottomInset = getSafeContentInsets(windowInsets).bottom;
                    }
                    else {
                        bottomInset = windowInsets.getTappableElementInsets().bottom;
                    }
                    view.setPadding(initialLeft,
                            initialTop,
                            initialRight,
                            initialBottom + bottomInset);
                    return windowInsets;
                }
            });
            view.requestApplyInsets();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private static WindowInsetsPolicy.EdgeInsets getSafeContentInsets(WindowInsets windowInsets) {
        WindowInsetsPolicy.EdgeInsets systemBars =
                toEdgeInsets(windowInsets.getInsets(WindowInsets.Type.systemBars()));
        WindowInsetsPolicy.EdgeInsets displayCutout =
                toEdgeInsets(windowInsets.getInsets(WindowInsets.Type.displayCutout()));
        return WindowInsetsPolicy.resolveSafeContentInsets(systemBars, displayCutout);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private static WindowInsetsPolicy.EdgeInsets getTappableInsets(WindowInsets windowInsets) {
        return toEdgeInsets(windowInsets.getInsets(WindowInsets.Type.tappableElement()));
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
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
        setGameModeStatus(activity, false, false);
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
        if (tappableBottom != 0) {
            activity.getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
        else {
            activity.getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
    }

    /**
     * Configures the stream content rectangle for Android 15's enforced edge-to-edge mode.
     *
     * The insets are applied to the activity content root rather than to individual
     * overlays. This guarantees that the stream view, local cursor, and touch coordinate
     * space stay aligned. Earlier Android versions retain their platform-managed inset
     * behavior.
     */
    public static void configureStreamWindowInsets(final Activity activity,
                                                   final boolean allowDisplayCutout) {
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

        activity.getWindow().setDecorFitsSystemWindows(false);
        final View contentView = activity.findViewById(android.R.id.content);
        contentView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                WindowInsetsPolicy.EdgeInsets systemBars =
                        toEdgeInsets(windowInsets.getInsets(WindowInsets.Type.systemBars()));
                WindowInsetsPolicy.EdgeInsets displayCutout =
                        toEdgeInsets(windowInsets.getInsets(WindowInsets.Type.displayCutout()));
                WindowInsetsPolicy.EdgeInsets contentInsets =
                        WindowInsetsPolicy.resolveStreamInsets(
                                activity.isInMultiWindowMode(),
                                allowDisplayCutout,
                                systemBars,
                                displayCutout);
                view.setPadding(contentInsets.left, contentInsets.top,
                        contentInsets.right, contentInsets.bottom);
                return windowInsets;
            }
        });
        contentView.requestApplyInsets();
    }

    public static void refreshStreamWindowInsets(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            activity.findViewById(android.R.id.content).requestApplyInsets();
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
        rootView.setOnApplyWindowInsetsListener(
                new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                    WindowInsetsPolicy.EdgeInsets contentInsets;
                    int tappableBottom;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        contentInsets = getSafeContentInsets(windowInsets);
                        tappableBottom = getTappableInsets(windowInsets).bottom;
                    }
                    else {
                        Insets tappableInsets = windowInsets.getTappableElementInsets();
                        contentInsets = new WindowInsetsPolicy.EdgeInsets(
                                tappableInsets.left, tappableInsets.top,
                                tappableInsets.right, tappableInsets.bottom);
                        tappableBottom = tappableInsets.bottom;
                    }
                    initialPadding.apply(view, contentInsets.left,
                            contentInsets.top, contentInsets.right, 0);
                    updateNavigationBarScrim(activity, tappableBottom);

                    return windowInsets;
                }
        });
        rootView.requestApplyInsets();
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
        contentRoot.setOnApplyWindowInsetsListener(
                new View.OnApplyWindowInsetsListener() {
                    @Override
                    public WindowInsets onApplyWindowInsets(View view,
                                                            WindowInsets windowInsets) {
                        WindowInsetsPolicy.EdgeInsets contentInsets;
                        int tappableBottom;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            contentInsets = getSafeContentInsets(windowInsets);
                            tappableBottom = getTappableInsets(windowInsets).bottom;
                        }
                        else {
                            Insets tappableInsets =
                                    windowInsets.getTappableElementInsets();
                            contentInsets = new WindowInsetsPolicy.EdgeInsets(
                                    tappableInsets.left, tappableInsets.top,
                                    tappableInsets.right, tappableInsets.bottom);
                            tappableBottom = tappableInsets.bottom;
                        }

                        headerPadding.apply(headerView,
                                contentInsets.left, contentInsets.top,
                                contentInsets.right, 0);
                        bodyPadding.apply(bodyView,
                                contentInsets.left, 0,
                                contentInsets.right, contentInsets.bottom);
                        updateNavigationBarScrim(activity, tappableBottom);
                        return windowInsets;
                    }
                });
        contentRoot.requestApplyInsets();
    }

    public static void showDecoderCrashDialog(Activity activity) {
        final SharedPreferences prefs = activity.getSharedPreferences("DecoderTombstone", 0);
        final int crashCount = prefs.getInt("CrashCount", 0);
        int lastNotifiedCrashCount = prefs.getInt("LastNotifiedCrashCount", 0);

        // Remember the last crash count we notified at, so we don't
        // display the crash dialog every time the app is started until
        // they stream again
        if (crashCount != 0 && crashCount != lastNotifiedCrashCount) {
            if (crashCount % 3 == 0) {
                // At 3 consecutive crashes, we'll forcefully reset their settings
                PreferenceConfiguration.resetStreamingSettings(activity);
                Dialog.displayDialog(activity,
                        activity.getResources().getString(R.string.title_decoding_reset),
                        activity.getResources().getString(R.string.message_decoding_reset),
                        new Runnable() {
                            @Override
                            public void run() {
                                // Mark notification as acknowledged on dismissal
                                prefs.edit().putInt("LastNotifiedCrashCount", crashCount).apply();
                            }
                        });
            }
            else {
                Dialog.displayDialog(activity,
                        activity.getResources().getString(R.string.title_decoding_error),
                        activity.getResources().getString(R.string.message_decoding_error),
                        new Runnable() {
                            @Override
                            public void run() {
                                // Mark notification as acknowledged on dismissal
                                prefs.edit().putInt("LastNotifiedCrashCount", crashCount).apply();
                            }
                        });
            }
        }
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

    public static void displayDeletePcConfirmationDialog(Activity parent, ComputerDetails computer, final Runnable onYes, final Runnable onNo) {
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
                .setTitle(computer.name)
                .setPositiveButton(parent.getResources().getString(R.string.yes), dialogClickListener)
                .setNegativeButton(parent.getResources().getString(R.string.no), dialogClickListener)
                .show();
    }

    public static int dpToPx(Context context, float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static void setStatusBarLightMode(@NonNull final Window window,
                                             final boolean isLightMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            View decorView = window.getDecorView();
            int vis = decorView.getSystemUiVisibility();
            if (isLightMode) {
                vis |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                vis &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            decorView.setSystemUiVisibility(vis);
        }
    }

}
