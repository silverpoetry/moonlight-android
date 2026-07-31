package com.limelight.preferences;

import androidx.annotation.RequiresApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.MediaCodecInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.appcompat.widget.SwitchCompat;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Range;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.limelight.utils.UiToast;

import com.limelight.AboutActivity;
import com.limelight.LimeLog;
import com.limelight.PcView;
import com.limelight.R;
import com.limelight.binding.video.MediaCodecHelper;
import com.limelight.computers.ComputerDatabaseManager;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.settings.android.SharedPreferencesSettingsRepository;
import com.limelight.settings.virtualcontrols.VirtualControlSettings;
import com.limelight.settings.virtualcontrols.VirtualControlSettingsLoader;
import com.limelight.utils.BackNavigationRegistration;
import com.limelight.utils.Dialog;
import com.limelight.utils.FileUriUtils;
import com.limelight.utils.HelpLauncher;
import com.limelight.utils.UiHelper;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutKey;
import com.limelight.virtualcontrols.layout.VirtualControlLayoutOrientation;
import com.limelight.virtualcontrols.layout.android.AndroidVirtualControlLayoutRepository;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE;

public class StreamSettings extends Activity {
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    private static final String SEEKBAR_NS = "http://schemas.moonlight-stream.com/apk/res/seekbar";

    private static final int READ_REQUEST_CODE = 1001;
    private static final int GAMEPAD_READ_REQUEST_CODE = 1002;
    private static final int READ_DATABASE_REQUEST_CODE = 1003;
    private static final int READ_DATA_CRT_REQUEST_CODE = 1004;
    private static final int READ_DATA_KEY_REQUEST_CODE = 1005;
    private static final int READ_REQUEST_SWITCH_BUTTON_CODE = 1007;
    private static final int READ_REQUEST_SCREEN_IMAGE_CODE = 1008;
    private static final int CLIPBOARD_DIRECTORY_REQUEST_CODE = 1009;
    private static final int MAX_BITRATE_KBPS = 50000;
    private static final int FEATURED_SECTION_INDEX = -1;
    private static final String EXTRA_SECTION_INDEX = "com.limelight.preferences.StreamSettings.SECTION_INDEX";
    private static final String[] ROOT_FEATURED_SETTING_KEYS = new String[] {
            PreferenceConfiguration.RESOLUTION_PREF_STRING,
            PreferenceConfiguration.RESOLUTION_ASPECT_RATIO_PREF_STRING,
            PreferenceConfiguration.FPS_PREF_STRING,
            PreferenceConfiguration.BITRATE_PREF_STRING,
            "list_fsr_target",
            "list_fsr_sharpness",
            "mouse_model_list_axi",
            PreferenceConfiguration.CLIPBOARD_SYNC_PREF_STRING,
    };

    private PreferenceConfiguration previousPrefs;
    private int previousDisplayPixelCount;
    private SettingsStore store;
    private ArrayList<SettingsSection> sections = new ArrayList<>();
    private FrameLayout mainContainer;
    private LinearLayout outerContainer;
    private LinearLayout wideSectionList;
    private FrameLayout wideItemContainer;
    private TextView titleView;
    private TextView subtitleView;
    private ImageButton backButton;
    private int selectedSectionIndex = -1;
    private int nativeResolutionStartIndex = Integer.MAX_VALUE;
    private boolean nativeFramerateShown;
    private boolean wideLayout;
    private boolean sectionActivity;
    private BackNavigationRegistration backNavigationRegistration;
    private AndroidVirtualControlLayoutRepository
            virtualControlLayoutRepository;

    // HACK for Android 9
    static DisplayCutout displayCutoutP;

    void reloadSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Display.Mode mode = getWindowManager().getDefaultDisplay().getMode();
            previousDisplayPixelCount = mode.getPhysicalWidth() * mode.getPhysicalHeight();
        }

        sections = SettingsRegistry.load(this);
        store = new SettingsStore(this);
        linkDependencyDefaults();
        nativeResolutionStartIndex = Integer.MAX_VALUE;
        nativeFramerateShown = false;
        initializeRuntimeSettings();
        removeEmptySections();
        selectedSectionIndex = clampSelectedSection(selectedSectionIndex);
        render();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !PreferenceConfiguration.readPreferences(this).uiThemeColorWhite) {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);

        previousPrefs = PreferenceConfiguration.readPreferences(this);
        UiHelper.setLocale(this);
        store = new SettingsStore(this);
        virtualControlLayoutRepository =
                new AndroidVirtualControlLayoutRepository(this);
        sectionActivity = getIntent().hasExtra(EXTRA_SECTION_INDEX);
        selectedSectionIndex = getIntent().getIntExtra(EXTRA_SECTION_INDEX, -1);

        setContentView(createRootView());
        configureImmersiveSettingsWindow();

        if (previousPrefs.uiThemeColorWhite) {
            UiHelper.setStatusBarLightMode(getWindow(), true);
        }

        registerBackCallback();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
            WindowInsets insets = getWindow().getDecorView().getRootWindowInsets();
            if (insets != null) {
                displayCutoutP = insets.getDisplayCutout();
            }
        }

        reloadSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mainContainer != null && sections != null && !sections.isEmpty()) {
            reloadSettings();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Display.Mode mode = getWindowManager().getDefaultDisplay().getMode();
            if (mode.getPhysicalWidth() * mode.getPhysicalHeight() != previousDisplayPixelCount) {
                reloadSettings();
                return;
            }
        }

        render();
    }

    @Override
    public void onBackPressed() {
        handleBackNavigation();
    }

    @Override
    protected void onDestroy() {
        if (backNavigationRegistration != null) {
            backNavigationRegistration.unregister();
            backNavigationRegistration = null;
        }
        super.onDestroy();
    }

    private void registerBackCallback() {
        backNavigationRegistration =
                BackNavigationRegistration.register(this, this::handleBackNavigation);
    }

    private void handleBackNavigation() {
        finishAndApplyLanguage();
    }

    private void finishAndApplyLanguage() {
        finish();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            PreferenceConfiguration newPrefs = PreferenceConfiguration.readPreferences(this);
            if (!newPrefs.language.equals(previousPrefs.language)) {
                Intent intent = new Intent(this, PcView.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent, null);
            }
        }
    }

    private View createRootView() {
        FrameLayout root = new FrameLayout(this);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackgroundResource(R.drawable.bg_gradient_axi_main);

        outerContainer = new LinearLayout(this);
        outerContainer.setOrientation(LinearLayout.VERTICAL);
        applySettingsWindowPadding();
        root.addView(outerContainer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        outerContainer.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        backButton = new ImageButton(this);
        backButton.setImageResource(R.drawable.ic_axi_back);
        backButton.setBackgroundResource(R.drawable.ic_game_menu_btn_transparent);
        backButton.setPadding(dp(9), dp(9), dp(9), dp(9));
        header.addView(backButton, new LinearLayout.LayoutParams(dp(44), dp(44)));
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleBackNavigation();
            }
        });

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        titleParams.leftMargin = dp(10);
        header.addView(titleBlock, titleParams);

        titleView = new TextView(this);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(24);
        titleView.setTypeface(null, Typeface.BOLD);
        titleBlock.addView(titleView);

        subtitleView = new TextView(this);
        subtitleView.setTextColor(0xCCFFFFFF);
        subtitleView.setTextSize(12);
        subtitleView.setSingleLine(true);
        subtitleView.setEllipsize(TextUtils.TruncateAt.END);
        titleBlock.addView(subtitleView);

        mainContainer = new FrameLayout(this);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        contentParams.topMargin = dp(14);
        outerContainer.addView(mainContainer, contentParams);
        return root;
    }

    private void configureImmersiveSettingsWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.layoutInDisplayCutoutMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                    ? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    : WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(layoutParams);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().setNavigationBarDividerColor(Color.TRANSPARENT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
        getWindow().getDecorView().setSystemUiVisibility(
                SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        applySettingsWindowPadding();
    }

    private void render() {
        if (mainContainer == null || sections == null) {
            return;
        }

        wideLayout = getAvailableWidthDp() >= 720;
        LinearLayout page = createPageContainer();

        if (wideLayout) {
            renderWide(page);
        }
        else if (sectionActivity && selectedSectionIndex >= 0) {
            renderSectionDetail(page, selectedSectionIndex);
        }
        else {
            renderSectionList(page);
        }

        mainContainer.removeAllViews();
        mainContainer.addView(page, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private LinearLayout createPageContainer() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setAlpha(1f);
        page.setTranslationX(0f);
        page.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return page;
    }

    private void renderWide(LinearLayout page) {
        titleView.setText(R.string.settings_title);
        subtitleView.setText(getCurrentProfileSummary());
        backButton.setVisibility(View.VISIBLE);

        LinearLayout columns = new LinearLayout(this);
        columns.setOrientation(LinearLayout.HORIZONTAL);
        page.addView(columns, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        ScrollView sectionScroll = createScrollView();
        LinearLayout sectionList = createVerticalList();
        wideSectionList = sectionList;
        sectionScroll.addView(sectionList);
        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(dp(300), ViewGroup.LayoutParams.MATCH_PARENT);
        columns.addView(sectionScroll, sectionParams);

        sectionList.addView(createFeaturedSectionRow(selectedSectionIndex == FEATURED_SECTION_INDEX));
        for (int i = 0; i < sections.size(); i++) {
            sectionList.addView(createSectionRow(sections.get(i), i, i == selectedSectionIndex));
        }

        wideItemContainer = new FrameLayout(this);
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
        itemParams.leftMargin = dp(14);
        columns.addView(wideItemContainer, itemParams);

        renderWideItemContent(false);
    }

    private void renderWideItemContent(boolean animate) {
        if (wideItemContainer == null) {
            return;
        }

        LinearLayout page = createPageContainer();
        if (selectedSectionIndex == FEATURED_SECTION_INDEX) {
            renderFeaturedSettings(page);
        }
        else {
            renderSectionDetail(page, selectedSectionIndex);
        }

        wideItemContainer.removeAllViews();
        wideItemContainer.addView(page, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        if (animate) {
            page.setAlpha(0f);
            page.animate().alpha(1f).setDuration(100).start();
        }
    }

    private void refreshWideSectionSelection() {
        if (wideSectionList == null) {
            return;
        }

        for (int i = 0; i < wideSectionList.getChildCount(); i++) {
            View child = wideSectionList.getChildAt(i);
            Object tag = child.getTag();
            if (tag instanceof Integer) {
                int sectionIndex = (Integer) tag;
                child.setBackgroundResource(sectionIndex == selectedSectionIndex ?
                        R.drawable.bg_settings_selected_card :
                        R.drawable.ic_game_menu_btn_selector);
            }
        }
    }

    private void renderSectionList(LinearLayout page) {
        titleView.setText(R.string.settings_title);
        subtitleView.setText(getCurrentProfileSummary());
        backButton.setVisibility(View.VISIBLE);

        ScrollView scroll = createScrollView();
        LinearLayout list = createVerticalList();
        scroll.addView(list);
        page.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        addRootFeaturedItems(list);
        addListHeader(list, R.string.settings_more_settings);
        for (int i = 0; i < sections.size(); i++) {
            list.addView(createSectionRow(sections.get(i), i, false));
        }
    }

    private void renderFeaturedSettings(LinearLayout page) {
        titleView.setText(R.string.settings_featured_settings);
        subtitleView.setText(getCurrentProfileSummary());
        backButton.setVisibility(View.VISIBLE);

        ScrollView scroll = createScrollView();
        LinearLayout list = createVerticalList();
        scroll.addView(list);
        page.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        addRootFeaturedItems(list);
    }

    private void addRootFeaturedItems(LinearLayout list) {
        boolean hasFeaturedItems = false;
        for (String key : ROOT_FEATURED_SETTING_KEYS) {
            SettingsItem item = findItem(key);
            if (item == null || !item.visible) {
                continue;
            }
            if (!hasFeaturedItems) {
                addListHeader(list, R.string.settings_featured_settings);
                hasFeaturedItems = true;
            }
            list.addView(createItemRow(item));
        }
    }

    private void addListHeader(LinearLayout list, int titleResId) {
        TextView header = new TextView(this);
        header.setText(titleResId);
        header.setTextColor(0xBFFFFFFF);
        header.setTextSize(13);
        header.setTypeface(null, Typeface.BOLD);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(4), dp(10), dp(4), dp(8));
        list.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void renderSectionDetail(LinearLayout page, int sectionIndex) {
        SettingsSection section = sections.get(sectionIndex);
        titleView.setText(section.title);
        subtitleView.setText(getResources().getQuantityString(
                R.plurals.settings_item_count, section.visibleItems().size(), section.visibleItems().size()));
        backButton.setVisibility(View.VISIBLE);

        ScrollView scroll = createScrollView();
        LinearLayout list = createVerticalList();
        scroll.addView(list);
        page.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        addSectionItems(list, section);
    }

    private void applySettingsWindowPadding() {
        if (outerContainer == null) {
            return;
        }

        final int horizontalPadding = dp(18);
        final int topPadding = dp(12);
        final int bottomPadding = dp(12);

        outerContainer.setPadding(horizontalPadding, topPadding,
                horizontalPadding, bottomPadding);

        outerContainer.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                int leftInset = 0;
                int topInset;
                int rightInset = 0;
                int bottomInset;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Insets safeInsets = insets.getInsets(
                            WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                    leftInset = safeInsets.left;
                    topInset = safeInsets.top;
                    rightInset = safeInsets.right;
                    bottomInset = safeInsets.bottom;
                }
                else {
                    topInset = insets.getSystemWindowInsetTop();
                    bottomInset = 0;
                }
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    bottomInset = insets.getTappableElementInsets().bottom;
                }
                v.setPadding(horizontalPadding + leftInset, topPadding + topInset,
                        horizontalPadding + rightInset, bottomPadding + bottomInset);
                return insets;
            }
        });
        outerContainer.requestApplyInsets();
    }

    private ScrollView createScrollView() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setHorizontalScrollBarEnabled(false);
        return scrollView;
    }

    private LinearLayout createVerticalList() {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, 0, 0, dp(12));
        return list;
    }

    private View createSectionRow(final SettingsSection section, final int index, boolean selected) {
        LinearLayout row = new LinearLayout(this);
        row.setTag(index);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(selected ? R.drawable.bg_settings_selected_card : R.drawable.ic_game_menu_btn_selector);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));

        ImageView icon = new ImageView(this);
        icon.setImageResource(section.iconRes);
        icon.setAlpha(0.88f);
        row.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));

        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        textParams.leftMargin = dp(12);
        row.addView(textBlock, textParams);

        TextView title = new TextView(this);
        title.setText(section.title);
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        textBlock.addView(title);

        TextView summary = new TextView(this);
        summary.setText(getResources().getQuantityString(
                R.plurals.settings_item_count, section.visibleItems().size(), section.visibleItems().size()));
        summary.setTextColor(0xBFFFFFFF);
        summary.setTextSize(12);
        textBlock.addView(summary);

        TextView arrow = new TextView(this);
        arrow.setText(">");
        arrow.setTextColor(0xCCFFFFFF);
        arrow.setTextSize(20);
        row.addView(arrow);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(8);
        row.setLayoutParams(params);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (wideLayout) {
                    selectedSectionIndex = index;
                    refreshWideSectionSelection();
                    renderWideItemContent(true);
                    return;
                }
                Intent intent = new Intent(StreamSettings.this, StreamSettings.class);
                intent.putExtra(EXTRA_SECTION_INDEX, index);
                startActivity(intent);
            }
        });
        return row;
    }

    private View createFeaturedSectionRow(boolean selected) {
        final SettingsSection section = new SettingsSection(
                "featured_settings",
                getText(R.string.settings_featured_settings),
                R.drawable.ic_axi_quick);
        LinearLayout row = (LinearLayout) createSectionRow(section, FEATURED_SECTION_INDEX, selected);
        row.setTag(FEATURED_SECTION_INDEX);
        return row;
    }

    private void addSectionItems(LinearLayout list, SettingsSection section) {
        ArrayList<SettingsItem> items = section.visibleItems();
        if (items.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.settings_no_items);
            empty.setTextColor(0xCCFFFFFF);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(18), dp(28), dp(18), dp(28));
            list.addView(empty);
            return;
        }

        for (SettingsItem item : items) {
            list.addView(createItemRow(item));
        }
    }

    private View createItemRow(final SettingsItem item) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(58));
        row.setPadding(dp(14), dp(9), dp(14), dp(9));
        row.setBackgroundResource(R.drawable.ic_game_menu_btn_selector);

        ImageView icon = new ImageView(this);
        icon.setImageResource(item.iconRes);
        icon.setAlpha(item.isEnabled(store) ? 0.86f : 0.35f);
        row.addView(icon, new LinearLayout.LayoutParams(dp(22), dp(22)));

        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        textParams.leftMargin = dp(12);
        row.addView(textBlock, textParams);

        TextView title = new TextView(this);
        title.setText(item.title);
        title.setTextColor(item.isEnabled(store) ? Color.WHITE : 0x80FFFFFF);
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        textBlock.addView(title);

        if (!TextUtils.isEmpty(item.summary)) {
            TextView summary = new TextView(this);
            summary.setText(item.summary);
            summary.setTextColor(item.isEnabled(store) ? 0xBFFFFFFF : 0x66FFFFFF);
            summary.setTextSize(12);
            summary.setMaxLines(2);
            summary.setEllipsize(TextUtils.TruncateAt.END);
            textBlock.addView(summary);
        }

        final View control = createControlView(item);
        if (control != null) {
            LinearLayout.LayoutParams controlParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            controlParams.leftMargin = dp(10);
            row.addView(control, controlParams);
        }

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = dp(7);
        row.setLayoutParams(rowParams);
        row.setEnabled(item.isEnabled(store));
        row.setAlpha(item.isEnabled(store) ? 1.0f : 0.55f);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!item.isEnabled(store)) {
                    return;
                }
                if (item.type == SettingsItem.Type.SWITCH &&
                        control instanceof SwitchCompat) {
                    control.performClick();
                    return;
                }
                handleItemClick(item);
            }
        });
        return row;
    }

    private View createControlView(final SettingsItem item) {
        if (item.type == SettingsItem.Type.SWITCH) {
            SwitchCompat switchView = new SwitchCompat(this);
            switchView.setShowText(false);
            switchView.setChecked(store.getBoolean(item));
            switchView.setEnabled(item.isEnabled(store));
            tintSwitch(switchView);
            switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    store.putBoolean(item.key, isChecked);
                    afterItemChanged(item, isChecked, true);
                }
            });
            return switchView;
        }

        TextView value = new TextView(this);
        value.setTextColor(0xE6FFFFFF);
        value.setTextSize(13);
        value.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        value.setMaxWidth(dp(180));
        value.setSingleLine(true);
        value.setEllipsize(TextUtils.TruncateAt.END);

        switch (item.type) {
            case LIST:
                value.setText(item.getSelectedEntry(store));
                break;
            case SLIDER:
                value.setText(item.formatSliderValue(item.round(store.getInt(item))));
                break;
            case TEXT:
                value.setText(store.getString(item));
                break;
            case ACTION:
            case WEB:
                value.setText(R.string.settings_action_open);
                break;
            default:
                return null;
        }
        return value;
    }

    private void handleItemClick(SettingsItem item) {
        switch (item.type) {
            case SWITCH:
                store.putBoolean(item.key, !store.getBoolean(item));
                afterItemChanged(item, store.getBoolean(item), true);
                break;
            case LIST:
                if ("list_languages".equals(item.key) &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launchNativeLanguageSettings();
                }
                else {
                    showListDialog(item);
                }
                break;
            case SLIDER:
                showSliderDialog(item);
                break;
            case TEXT:
                showTextDialog(item);
                break;
            case ACTION:
                performAction(item.key);
                break;
            case WEB:
                if ("about".equals(item.url)) {
                    startActivity(new Intent(this, AboutActivity.class));
                }
                else {
                    HelpLauncher.launchUrl(this, item.url);
                }
                break;
        }
    }

    private void showListDialog(final SettingsItem item) {
        final AlertDialog dialog = new AlertDialog.Builder(this).create();
        LinearLayout panel = createDialogPanel(item.title);
        String current = store.getString(item);

        for (int i = 0; i < item.entryValues.length; i++) {
            final String value = item.entryValues[i].toString();
            TextView row = createDialogRow(item.entries[i], value.equals(current));
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (beforeListValueChanged(item, value)) {
                        store.putString(item.key, value);
                        afterItemChanged(item, value, false);
                    }
                    dialog.dismiss();
                }
            });
            panel.addView(row);
        }

        addDialogCancel(panel, dialog);
        showCustomDialog(dialog, panel);
    }

    private void showSliderDialog(final SettingsItem item) {
        final AlertDialog dialog = new AlertDialog.Builder(this).create();
        LinearLayout panel = createDialogPanel(item.title);

        if (!TextUtils.isEmpty(item.dialogMessage)) {
            TextView message = new TextView(this);
            message.setText(item.dialogMessage);
            message.setTextColor(0xCCFFFFFF);
            message.setTextSize(13);
            message.setPadding(0, 0, 0, dp(10));
            panel.addView(message);
        }

        final TextView valueText = new TextView(this);
        valueText.setGravity(Gravity.CENTER);
        valueText.setTextColor(Color.WHITE);
        valueText.setTextSize(28);
        valueText.setTypeface(null, Typeface.BOLD);
        panel.addView(valueText);

        final SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(item.max);
        if (item.keyStep > 0) {
            seekBar.setKeyProgressIncrement(item.keyStep);
        }
        seekBar.setProgress(item.round(store.getInt(item)));
        panel.addView(seekBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int rounded = item.round(progress);
                if (rounded != progress) {
                    seekBar.setProgress(rounded);
                    return;
                }
                valueText.setText(item.formatSliderValue(rounded));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        valueText.setText(item.formatSliderValue(seekBar.getProgress()));

        LinearLayout buttons = createDialogButtonRow();
        TextView cancel = createDialogButton(getString(R.string.settings_cancel));
        TextView ok = createDialogButton(getString(R.string.settings_ok));
        buttons.addView(cancel);
        buttons.addView(ok);
        panel.addView(buttons);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int progress = item.round(seekBar.getProgress());
                store.putInt(item.key, progress);
                afterItemChanged(item, progress, false);
                dialog.dismiss();
            }
        });

        showCustomDialog(dialog, panel);
    }

    private void showTextDialog(final SettingsItem item) {
        final AlertDialog dialog = new AlertDialog.Builder(this).create();
        LinearLayout panel = createDialogPanel(item.title);

        if (!TextUtils.isEmpty(item.dialogMessage)) {
            TextView message = new TextView(this);
            message.setText(item.dialogMessage);
            message.setTextColor(0xCCFFFFFF);
            message.setTextSize(13);
            message.setPadding(0, 0, 0, dp(10));
            panel.addView(message);
        }

        final EditText input = new EditText(this);
        input.setText(store.getString(item));
        input.setSingleLine(true);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(0x88FFFFFF);
        input.setSelectAllOnFocus(true);
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        if ("edit_diy_bitrate".equals(item.key)) {
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});
        }
        panel.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout buttons = createDialogButtonRow();
        TextView cancel = createDialogButton(getString(R.string.settings_cancel));
        TextView ok = createDialogButton(getString(R.string.settings_ok));
        buttons.addView(cancel);
        buttons.addView(ok);
        panel.addView(buttons);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = input.getText().toString();
                if (beforeTextValueChanged(item, value)) {
                    store.putString(item.key, value);
                    afterItemChanged(item, value, false);
                    dialog.dismiss();
                }
            }
        });

        showCustomDialog(dialog, panel);
        input.requestFocus();
    }

    private LinearLayout createDialogPanel(CharSequence title) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundResource(R.drawable.bg_update_dialog_panel);
        panel.setPadding(dp(18), dp(16), dp(18), dp(14));

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(19);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setPadding(0, 0, 0, dp(12));
        panel.addView(titleView);
        return panel;
    }

    private TextView createDialogRow(CharSequence text, boolean selected) {
        TextView row = new TextView(this);
        row.setText(selected ? "✓  " + text : "    " + text);
        row.setTextColor(Color.WHITE);
        row.setTextSize(15);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setSingleLine(false);
        row.setBackgroundResource(selected ? R.drawable.bg_settings_selected_card : R.drawable.ic_game_menu_btn_selector);
        row.setPadding(dp(12), dp(11), dp(12), dp(11));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(7);
        row.setLayoutParams(params);
        return row;
    }

    private LinearLayout createDialogButtonRow() {
        LinearLayout buttons = new LinearLayout(this);
        buttons.setGravity(Gravity.END);
        buttons.setPadding(0, dp(14), 0, 0);
        return buttons;
    }

    private TextView createDialogButton(String text) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setTypeface(null, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackgroundResource(R.drawable.ic_game_menu_btn_selector);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(92), dp(40));
        params.setMarginStart(dp(10));
        button.setLayoutParams(params);
        return button;
    }

    private void addDialogCancel(LinearLayout panel, final AlertDialog dialog) {
        LinearLayout buttons = createDialogButtonRow();
        TextView cancel = createDialogButton(getString(R.string.settings_cancel));
        buttons.addView(cancel);
        panel.addView(buttons);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    private void showCustomDialog(AlertDialog dialog, View panel) {
        dialog.setView(panel);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private boolean beforeListValueChanged(SettingsItem item, String value) {
        if (PreferenceConfiguration.RESOLUTION_PREF_STRING.equals(item.key)) {
            boolean isNativeRes = true;
            for (int i = 0; i < item.entryValues.length; i++) {
                if (value.equals(item.entryValues[i].toString()) && i < nativeResolutionStartIndex) {
                    isNativeRes = false;
                    break;
                }
            }
            store.putString(PreferenceConfiguration.RESOLUTION_SELECTION_PREF_STRING,
                    isNativeRes ?
                            PreferenceConfiguration.RESOLUTION_SELECTION_CUSTOM_OR_NATIVE :
                            PreferenceConfiguration.RESOLUTION_SELECTION_PRESET);
        }

        if (PreferenceConfiguration.FPS_PREF_STRING.equals(item.key) &&
                nativeFramerateShown &&
                item.entryValues.length > 0 &&
                item.entryValues[item.entryValues.length - 1].toString().equals(value)) {
            Dialog.displayDialog(this,
                    getResources().getString(R.string.title_native_fps_dialog),
                    getResources().getString(R.string.text_native_res_dialog),
                    false);
        }
        return true;
    }

    private boolean beforeTextValueChanged(SettingsItem item, String value) {
        if ("edit_diy_bitrate".equals(item.key)) {
            if (TextUtils.isEmpty(value)) {
                UiToast.makeText(this, "请输入0-9999的数值。", UiToast.LENGTH_SHORT).show();
                return false;
            }
            try {
                float bitrateValue = Float.valueOf(value) * 1000;
                store.putInt(PreferenceConfiguration.BITRATE_PREF_STRING, (int) bitrateValue);
            } catch (NumberFormatException e) {
                UiToast.makeText(this, "请输入0-9999的数值。", UiToast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private void afterItemChanged(SettingsItem item, Object value, boolean allowSwitchAnimation) {
        if (PreferenceConfiguration.BAROMETER_FORCE_PRESS_PREF_STRING.equals(
                item.key)) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isFinishing()) {
                        reloadSettings();
                    }
                }
            }, allowSwitchAnimation ? 180 : 0);
            return;
        }

        if (PreferenceConfiguration.UNLOCK_FPS_STRING.equals(item.key)) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isFinishing()) {
                        reloadSettings();
                    }
                }
            }, 500);
            return;
        }

        if (allowSwitchAnimation) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isFinishing()) {
                        refreshAfterItemChanged();
                    }
                }
            }, 180);
        }
        else {
            refreshAfterItemChanged();
        }
    }

    private void refreshAfterItemChanged() {
        if (wideLayout && wideItemContainer != null) {
            renderWideItemContent(false);
        }
        else {
            render();
        }
    }

    private void tintSwitch(SwitchCompat switchView) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
        };
        switchView.setThumbTintList(new ColorStateList(states, new int[]{
                0xFFFFFFFF,
                0xFFE8E3F2
        }));
        switchView.setTrackTintList(new ColorStateList(states, new int[]{
                0xFF24C46B,
                0xFF37324D
        }));
    }

    private void performAction(String key) {
        if ("import_keyboard_file".equals(key)) {
            openDocument("text/plain", READ_REQUEST_CODE);
        }
        else if ("import_gamepad_file".equals(key)) {
            openDocument("text/plain", GAMEPAD_READ_REQUEST_CODE);
        }
        else if ("import_computers_data_file".equals(key)) {
            openDocument("*/*", READ_DATABASE_REQUEST_CODE);
        }
        else if ("import_https_data_crt_file".equals(key)) {
            openDocument("*/*", READ_DATA_CRT_REQUEST_CODE);
        }
        else if ("import_https_data_key_file".equals(key)) {
            openDocument("*/*", READ_DATA_KEY_REQUEST_CODE);
        }
        else if ("import_switch_button_file".equals(key)) {
            openDocument("application/json", READ_REQUEST_SWITCH_BUTTON_CODE);
        }
        else if ("import_image_file_key".equals(key)) {
            openDocument("image/*", READ_REQUEST_SCREEN_IMAGE_CODE);
        }
        else if (PreferenceConfiguration.CLIPBOARD_FILE_DIRECTORY_PREF_STRING.equals(key)) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
            startActivityForResult(intent, CLIPBOARD_DIRECTORY_REQUEST_CODE);
        }
        else if ("export_keyboard_file".equals(key)) {
            exportKeyboard(false);
        }
        else if ("export_gamepad_file".equals(key)) {
            exportKeyboard(true);
        }
        else if ("export_computers_data_file".equals(key)) {
            exportFile(getDatabasePath(ComputerDatabaseManager.COMPUTER_DB_NAME), "*/*");
        }
        else if ("export_https_data_crt_file".equals(key)) {
            exportFile(new File(getFilesDir(), "client.crt"), "*/*");
        }
        else if ("export_https_data_key_file".equals(key)) {
            exportFile(new File(getFilesDir(), "client.key"), "*/*");
        }
    }

    private void openDocument(String type, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(type);
        startActivityForResult(intent, requestCode);
    }

    private void exportKeyboard(boolean gamepad) {
        VirtualControlLayoutKey layoutKey =
                getSelectedLayoutKey(gamepad);
        Uri uri = virtualControlLayoutRepository
                .getShareUri(layoutKey);
        if (uri == null) {
            UiToast.makeText(
                    this,
                    R.string.virtual_control_layout_export_missing,
                    UiToast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setType("text/plain");
        startActivity(Intent.createChooser(intent, "保存配置文件"));
    }

    private VirtualControlLayoutKey getSelectedLayoutKey(
            boolean gamepad) {
        VirtualControlSettings settings =
                VirtualControlSettingsLoader.load(
                        new SharedPreferencesSettingsRepository(
                                store.prefs));
        return gamepad
                ? VirtualControlLayoutKey.gamepad(
                        settings.getGamepadLayoutId(),
                        VirtualControlLayoutOrientation.LANDSCAPE)
                : VirtualControlLayoutKey.keyboard(
                        settings.getKeyboardLayoutId(),
                        VirtualControlLayoutOrientation.LANDSCAPE);
    }

    private void exportFile(File file, String type) {
        if (!file.exists()) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setType(type);
        startActivity(Intent.createChooser(intent, "保存数据文件"));
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void launchNativeLanguageSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APP_LOCALE_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent, null);
        } catch (ActivityNotFoundException e) {
            SettingsItem item = findItem("list_languages");
            if (item != null) {
                showListDialog(item);
            }
        }
    }

    private void initializeRuntimeSettings() {
        initializeBitrateSetting();
        applyDeviceVisibility();
        addCustomResolution();
        initializeDisplayCapabilities();
        initializeClipboardDirectory();
    }

    private void initializeClipboardDirectory() {
        SettingsItem item = findItem(
                PreferenceConfiguration.CLIPBOARD_FILE_DIRECTORY_PREF_STRING);
        if (item == null) {
            return;
        }
        String value = store.prefs.getString(
                PreferenceConfiguration.CLIPBOARD_FILE_DIRECTORY_PREF_STRING, "");
        if (TextUtils.isEmpty(value)) {
            return;
        }
        try {
            DocumentFile directory = DocumentFile.fromTreeUri(this, Uri.parse(value));
            String name = directory == null ? null : directory.getName();
            item.summary = getString(R.string.clipboard_file_save_directory_selected,
                    TextUtils.isEmpty(name) ? value : name);
        } catch (Throwable ignored) {
        }
    }

    private void initializeBitrateSetting() {
        SettingsItem bitrate = findItem(PreferenceConfiguration.BITRATE_PREF_STRING);
        if (bitrate == null) {
            return;
        }

        bitrate.defaultInt = PreferenceConfiguration.getDefaultBitrate(this);
        bitrate.max = MAX_BITRATE_KBPS;
        if (bitrate.keyStep <= 0) {
            bitrate.keyStep = 1000;
        }
    }

    private void applyDeviceVisibility() {
        PackageManager pm = getPackageManager();

        if (!pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)) {
            hideSection("category_onscreen_controls");
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                pm.hasSystemFeature("com.nvidia.feature.shield")) {
            hideItem("checkbox_absolute_mouse_mode");
        }

        SensorManager sensorManager =
                (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        boolean hasPressureSensor = sensorManager != null &&
                sensorManager.getDefaultSensor(
                        Sensor.TYPE_PRESSURE, false) != null;
        if (!hasPressureSensor) {
            hideItem(PreferenceConfiguration.BAROMETER_FORCE_PRESS_PREF_STRING);
            hideItem(PreferenceConfiguration.BAROMETER_FORCE_PRESS_THRESHOLD_PREF_STRING);
            hideItem(PreferenceConfiguration.BAROMETER_FORCE_PRESS_MIN_DURATION_PREF_STRING);
        }
        else if (!store.getBoolean(
                PreferenceConfiguration.BAROMETER_FORCE_PRESS_PREF_STRING,
                false)) {
            hideItem(PreferenceConfiguration.BAROMETER_FORCE_PRESS_THRESHOLD_PREF_STRING);
            hideItem(PreferenceConfiguration.BAROMETER_FORCE_PRESS_MIN_DURATION_PREF_STRING);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            hideItem("checkbox_gamepad_motion_sensors");
        }

        if (!pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER) &&
                !pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE)) {
            hideItem("checkbox_gamepad_motion_fallback");
        }

        if (!pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)) {
            hideItem("checkbox_usb_bind_all");
            hideItem("checkbox_usb_driver");
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                !pm.hasSystemFeature("android.software.picture_in_picture") ||
                pm.hasSystemFeature("com.amazon.software.fireos")) {
            hideItem("checkbox_enable_pip");
        }

        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            hideItem("checkbox_vibrate_fallback");
            hideItem("seekbar_vibrate_fallback_strength");
            hideItem("checkbox_enable_audio_haptics");
            hideItem("seekbar_audio_haptics_strength");
            hideItem("list_audio_haptics_voice_filter");
            hideItem("checkbox_vibrate_osc");
        }
        else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !vibrator.hasAmplitudeControl()) {
            hideItem("seekbar_vibrate_fallback_strength");
        }
    }

    private void addCustomResolution() {
        String diy = store.prefs.getString("edit_diy_w_h", "");
        if (!TextUtils.isEmpty(diy)) {
            String[] diys = diy.split("x");
            if (diys.length == 2) {
                try {
                    addNativeResolutionEntries(Integer.parseInt(diys[0]), Integer.parseInt(diys[1]), false);
                } catch (Exception e) {
                    LimeLog.warning("Invalid custom resolution: " + diy);
                }
            }
        }
    }

    private void initializeDisplayCapabilities() {
        Display display = getWindowManager().getDefaultDisplay();
        float maxSupportedFps = display.getRefreshRate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int maxSupportedResW = 0;
            boolean hasInsets = false;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                DisplayCutout cutout = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
                        display.getCutout() : displayCutoutP;

                if (cutout != null) {
                    int widthInsets = cutout.getSafeInsetLeft() + cutout.getSafeInsetRight();
                    int heightInsets = cutout.getSafeInsetBottom() + cutout.getSafeInsetTop();

                    if (widthInsets != 0 || heightInsets != 0) {
                        DisplayMetrics metrics = new DisplayMetrics();
                        display.getRealMetrics(metrics);

                        int width = Math.max(metrics.widthPixels - widthInsets, metrics.heightPixels - heightInsets);
                        int height = Math.min(metrics.widthPixels - widthInsets, metrics.heightPixels - heightInsets);

                        addNativeResolutionEntries(width, height, false);
                        hasInsets = true;
                    }
                }
            }

            for (Display.Mode candidate : display.getSupportedModes()) {
                int width = Math.max(candidate.getPhysicalWidth(), candidate.getPhysicalHeight());
                int height = Math.min(candidate.getPhysicalWidth(), candidate.getPhysicalHeight());

                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEVISION) ||
                        (width > 3840 || height > 2160)) {
                    addNativeResolutionEntries(width, height, hasInsets);
                }

                if ((width >= 3840 || height >= 2160) && maxSupportedResW < 3840) {
                    maxSupportedResW = 3840;
                }
                else if ((width >= 2560 || height >= 1440) && maxSupportedResW < 2560) {
                    maxSupportedResW = 2560;
                }
                else if ((width >= 1920 || height >= 1080) && maxSupportedResW < 1920) {
                    maxSupportedResW = 1920;
                }

                if (candidate.getRefreshRate() > maxSupportedFps) {
                    maxSupportedFps = candidate.getRefreshRate();
                }
            }

            MediaCodecHelper.initialize(this, GlPreferences.readPreferences(this).glRenderer);
            maxSupportedResW = updateMaxResolutionFromDecoder(maxSupportedResW, "video/avc");
            maxSupportedResW = updateMaxResolutionFromDecoder(maxSupportedResW, "video/hevc");

            if (maxSupportedResW != 0) {
                if (maxSupportedResW < 3840) {
                    removeValue(PreferenceConfiguration.RESOLUTION_PREF_STRING, PreferenceConfiguration.RES_4K,
                            PreferenceConfiguration.RES_1440P);
                }
                if (maxSupportedResW < 2560) {
                    removeValue(PreferenceConfiguration.RESOLUTION_PREF_STRING, PreferenceConfiguration.RES_1440P,
                            PreferenceConfiguration.RES_1080P);
                }
                if (maxSupportedResW < 1920) {
                    removeValue(PreferenceConfiguration.RESOLUTION_PREF_STRING, PreferenceConfiguration.RES_1080P,
                            PreferenceConfiguration.RES_720P);
                }
            }
        }
        else {
            DisplayMetrics metrics = new DisplayMetrics();
            display.getRealMetrics(metrics);
            int width = Math.max(metrics.widthPixels, metrics.heightPixels);
            int height = Math.min(metrics.widthPixels, metrics.heightPixels);
            addNativeResolutionEntries(width, height, false);
        }

        if (!PreferenceConfiguration.readPreferences(this).unlockFps) {
            if (maxSupportedFps < 118) {
                removeValue(PreferenceConfiguration.FPS_PREF_STRING, "120", "90");
            }
            if (maxSupportedFps < 88) {
                removeValue(PreferenceConfiguration.FPS_PREF_STRING, "90", "60");
            }
        }
        addNativeFrameRateEntry(maxSupportedFps);
        initializeHdrVisibility(display);
    }

    private int updateMaxResolutionFromDecoder(int maxSupportedResW, String mimeType) {
        MediaCodecInfo decoder = MediaCodecHelper.findProbableSafeDecoder(mimeType, -1);
        if (decoder == null) {
            return maxSupportedResW;
        }

        Range<Integer> widthRange = decoder.getCapabilitiesForType(mimeType)
                .getVideoCapabilities().getSupportedWidths();
        LimeLog.info(mimeType + " supported width range: " + widthRange.getLower() + " - " + widthRange.getUpper());
        if (widthRange.contains(1280)) {
            if (widthRange.contains(3840) && maxSupportedResW < 3840) {
                return 3840;
            }
            else if (widthRange.contains(1920) && maxSupportedResW < 1920) {
                return 1920;
            }
            else if (maxSupportedResW < 1280) {
                return 1280;
            }
        }
        return maxSupportedResW;
    }

    private void initializeHdrVisibility(Display display) {
        SettingsItem hdrItem = findItem("checkbox_enable_hdr");
        if (hdrItem == null) {
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            hideItem("checkbox_enable_hdr");
            return;
        }

        Display.HdrCapabilities hdrCaps = display.getHdrCapabilities();
        boolean foundHdr10 = false;
        if (hdrCaps != null) {
            for (int hdrType : hdrCaps.getSupportedHdrTypes()) {
                if (hdrType == Display.HdrCapabilities.HDR_TYPE_HDR10) {
                    foundHdr10 = true;
                    break;
                }
            }
        }

        if (!foundHdr10) {
            hideItem("checkbox_enable_hdr");
        }
        else if (PreferenceConfiguration.isShieldAtvFirmwareWithBrokenHdr()) {
            hdrItem.enabled = false;
            store.putBoolean("checkbox_enable_hdr", false);
            hdrItem.summary = "Update the firmware on your NVIDIA SHIELD Android TV to enable HDR";
        }
    }

    private void addNativeResolutionEntries(int nativeWidth, int nativeHeight, boolean insetsRemoved) {
        if (PreferenceConfiguration.isSquarishScreen(nativeWidth, nativeHeight)) {
            addNativeResolutionEntry(nativeHeight, nativeWidth, insetsRemoved, true);
        }
        addNativeResolutionEntry(nativeWidth, nativeHeight, insetsRemoved, false);
    }

    private void addNativeResolutionEntry(int nativeWidth, int nativeHeight, boolean insetsRemoved, boolean portrait) {
        SettingsItem item = findItem(PreferenceConfiguration.RESOLUTION_PREF_STRING);
        if (item == null) {
            return;
        }

        String newName = getResources().getString(insetsRemoved ?
                R.string.resolution_prefix_native_fullscreen :
                R.string.resolution_prefix_native);

        if (PreferenceConfiguration.isSquarishScreen(nativeWidth, nativeHeight)) {
            newName += " " + getResources().getString(portrait ?
                    R.string.resolution_prefix_native_portrait :
                    R.string.resolution_prefix_native_landscape);
        }

        newName += " (" + nativeWidth + "x" + nativeHeight + ")";
        String newValue = nativeWidth + "x" + nativeHeight;

        for (CharSequence value : item.entryValues) {
            if (newValue.equals(value.toString())) {
                return;
            }
        }

        if (item.entryValues.length < nativeResolutionStartIndex) {
            nativeResolutionStartIndex = item.entryValues.length;
        }
        item.appendEntry(newName, newValue);
    }

    private void addNativeFrameRateEntry(float framerate) {
        int frameRateRounded = Math.round(framerate);
        if (frameRateRounded == 0) {
            return;
        }

        SettingsItem item = findItem(PreferenceConfiguration.FPS_PREF_STRING);
        if (item == null) {
            return;
        }

        String fpsValue = Integer.toString(frameRateRounded);
        for (CharSequence value : item.entryValues) {
            if (fpsValue.equals(value.toString())) {
                nativeFramerateShown = false;
                return;
            }
        }

        String fpsName = getResources().getString(R.string.resolution_prefix_native) +
                " (" + fpsValue + " " + getResources().getString(R.string.fps_suffix_fps) + ")";
        item.appendEntry(fpsName, fpsValue);
        nativeFramerateShown = true;
    }

    private void removeValue(String preferenceKey, String value, String fallbackValue) {
        SettingsItem item = findItem(preferenceKey);
        if (item == null || item.entryValues.length == 0) {
            return;
        }

        ArrayList<CharSequence> entries = new ArrayList<>();
        ArrayList<CharSequence> values = new ArrayList<>();
        for (int i = 0; i < item.entryValues.length; i++) {
            if (!value.equalsIgnoreCase(item.entryValues[i].toString())) {
                entries.add(item.entries[i]);
                values.add(item.entryValues[i]);
            }
        }
        item.entries = entries.toArray(new CharSequence[0]);
        item.entryValues = values.toArray(new CharSequence[0]);

        if (value.equalsIgnoreCase(store.getString(item))) {
            if (PreferenceConfiguration.RESOLUTION_PREF_STRING.equals(preferenceKey)) {
                store.putString(PreferenceConfiguration.RESOLUTION_SELECTION_PREF_STRING,
                        PreferenceConfiguration.isStandardResolutionPreset(fallbackValue) ?
                                PreferenceConfiguration.RESOLUTION_SELECTION_PRESET :
                                PreferenceConfiguration.RESOLUTION_SELECTION_CUSTOM_OR_NATIVE);
            }
            store.putString(preferenceKey, fallbackValue);
        }
    }

    private SettingsItem findItem(String key) {
        if (key == null) {
            return null;
        }
        for (SettingsSection section : sections) {
            for (SettingsItem item : section.items) {
                if (key.equals(item.key)) {
                    return item;
                }
            }
        }
        return null;
    }

    private void hideItem(String key) {
        SettingsItem item = findItem(key);
        if (item != null) {
            item.visible = false;
        }
    }

    private void hideSection(String key) {
        for (SettingsSection section : sections) {
            if (key != null && key.equals(section.key)) {
                section.visible = false;
            }
        }
    }

    private void removeEmptySections() {
        ArrayList<SettingsSection> filtered = new ArrayList<>();
        for (SettingsSection section : sections) {
            if (section.visible && !section.visibleItems().isEmpty()) {
                filtered.add(section);
            }
        }
        sections = filtered;
    }

    private void linkDependencyDefaults() {
        for (SettingsSection section : sections) {
            for (SettingsItem item : section.items) {
                if (!TextUtils.isEmpty(item.dependency)) {
                    SettingsItem dependency = findItem(item.dependency);
                    if (dependency != null) {
                        item.dependencyDefault = dependency.defaultBoolean;
                        item.dependencyItemRef = dependency;
                    }
                }
            }
        }
    }

    private int clampSelectedSection(int selected) {
        if (sections.isEmpty()) {
            return -1;
        }
        if (selected < 0) {
            return -1;
        }
        return Math.min(selected, sections.size() - 1);
    }

    private String getCurrentProfileSummary() {
        SettingsItem resolution = findItem(PreferenceConfiguration.RESOLUTION_PREF_STRING);
        SettingsItem fps = findItem(PreferenceConfiguration.FPS_PREF_STRING);
        SettingsItem bitrate = findItem(PreferenceConfiguration.BITRATE_PREF_STRING);

        String resolutionText = resolution == null ? "" : resolution.getSelectedEntry(store).toString();
        String fpsText = fps == null ? "" : fps.getSelectedEntry(store).toString();
        String bitrateText = bitrate == null ? "" : bitrate.formatSliderValue(bitrate.round(store.getInt(bitrate)));
        return getString(R.string.settings_current_profile, resolutionText, fpsText, bitrateText);
    }

    private int getAvailableWidthDp() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        return (int) (metrics.widthPixels / metrics.density);
    }

    private int dp(float value) {
        return UiHelper.dpToPx(this, value);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CLIPBOARD_DIRECTORY_REQUEST_CODE &&
                resultCode == Activity.RESULT_OK && data != null &&
                data.getData() != null) {
            Uri directory = data.getData();
            try {
                if (!FileUriUtils.persistUriPermission(this, data, directory)) {
                    throw new SecurityException(
                            "Document provider returned no persistable URI permission");
                }
                store.prefs.edit()
                        .putString(
                                PreferenceConfiguration.CLIPBOARD_FILE_DIRECTORY_PREF_STRING,
                                directory.toString())
                        .apply();
                reloadSettings();
            } catch (SecurityException error) {
                UiToast.makeText(this, "无法保留该目录的访问权限",
                        UiToast.LENGTH_SHORT).show();
            }
            return;
        }
        if ((requestCode == READ_REQUEST_CODE || requestCode == GAMEPAD_READ_REQUEST_CODE) && resultCode == Activity.RESULT_OK && data.getData() != null) {
            try {
                Uri uri = data.getData();
                virtualControlLayoutRepository.importFrom(
                        getContentResolver(),
                        uri,
                        getSelectedLayoutKey(
                                requestCode ==
                                        GAMEPAD_READ_REQUEST_CODE));
                UiToast.makeText(
                        this,
                        R.string.virtual_control_layout_import_succeeded,
                        UiToast.LENGTH_SHORT).show();
            } catch (IOException | IllegalArgumentException error) {
                LimeLog.warning(
                        "Unable to import virtual-control layout: " +
                                error.getMessage());
                UiToast.makeText(
                        this,
                        R.string.virtual_control_layout_import_failed,
                        UiToast.LENGTH_SHORT).show();
            }
            return;
        }

        if (requestCode == READ_DATABASE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data.getData() != null) {
            try {
                Uri uri = data.getData();
                File dataBaseFile;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    dataBaseFile = FileUriUtils.uriToFileApiQ(uri, this);
                }
                else {
                    String displayName = System.currentTimeMillis() + Math.round((Math.random() + 1) * 1000) + ".db";
                    dataBaseFile = new File(getCacheDir(), displayName);
                    FileUriUtils.copyUriToInternalStorage(this, uri, dataBaseFile);
                }
                ComputerDatabaseManager importManager = new ComputerDatabaseManager(this, dataBaseFile);
                List<ComputerDetails> importComputers = importManager.getAllComputers();
                ComputerDatabaseManager manager = new ComputerDatabaseManager(this);
                for (ComputerDetails computer : importComputers) {
                    manager.updateComputer(computer);
                }
                UiToast.makeText(this, "导入成功,重新打开APP生效！", UiToast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                UiToast.makeText(this, "出错啦~" + e.getMessage(), UiToast.LENGTH_SHORT).show();
            }
            return;
        }

        if (requestCode == READ_DATA_CRT_REQUEST_CODE && resultCode == Activity.RESULT_OK && data.getData() != null) {
            importFile(data.getData(), "client.crt");
            return;
        }

        if (requestCode == READ_DATA_KEY_REQUEST_CODE && resultCode == Activity.RESULT_OK && data.getData() != null) {
            importFile(data.getData(), "client.key");
            return;
        }

        if (requestCode == READ_REQUEST_SWITCH_BUTTON_CODE && resultCode == Activity.RESULT_OK && data.getData() != null) {
            importFile(data.getData(), "axi_switch_keyboard.json");
            return;
        }

        if (requestCode == READ_REQUEST_SCREEN_IMAGE_CODE && resultCode == Activity.RESULT_OK && data.getData() != null) {
            try {
                String displayName = "axi_screen_bg_" + System.currentTimeMillis() + ".png";
                File imageFile = new File(getFilesDir(), displayName);
                FileUriUtils.copyUriToInternalStorage(this, data.getData(), imageFile);
                store.prefs.edit().putString("screen_bg_file_name", displayName).apply();
            } catch (Exception e) {
                e.printStackTrace();
                UiToast.makeText(this, "出错啦~" + e.getMessage(), UiToast.LENGTH_SHORT).show();
            }
        }
    }

    private void importFile(Uri uri, String displayName) {
        try {
            File file = new File(getFilesDir(), displayName);
            FileUriUtils.copyUriToInternalStorage(this, uri, file);
            UiToast.makeText(this, "导入成功!", UiToast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            UiToast.makeText(this, "出错啦~" + e.getMessage(), UiToast.LENGTH_SHORT).show();
        }
    }

    private static final class SettingsStore {
        final SharedPreferences prefs;

        SettingsStore(Context context) {
            prefs = PreferenceManager.getDefaultSharedPreferences(context);
        }

        boolean getBoolean(SettingsItem item) {
            try {
                return prefs.getBoolean(item.key, item.defaultBoolean);
            } catch (ClassCastException e) {
                return Boolean.parseBoolean(prefs.getString(item.key, Boolean.toString(item.defaultBoolean)));
            }
        }

        boolean getBoolean(String key, boolean defaultValue) {
            try {
                return prefs.getBoolean(key, defaultValue);
            } catch (ClassCastException e) {
                return Boolean.parseBoolean(prefs.getString(key, Boolean.toString(defaultValue)));
            }
        }

        int getInt(SettingsItem item) {
            try {
                return prefs.getInt(item.key, item.defaultInt);
            } catch (ClassCastException e) {
                try {
                    return Integer.parseInt(prefs.getString(item.key, Integer.toString(item.defaultInt)));
                } catch (NumberFormatException ignored) {
                    return item.defaultInt;
                }
            }
        }

        String getString(SettingsItem item) {
            try {
                return prefs.getString(item.key, item.defaultString);
            } catch (ClassCastException e) {
                try {
                    return Integer.toString(prefs.getInt(item.key, item.defaultInt));
                } catch (ClassCastException ignored) {
                    return Boolean.toString(prefs.getBoolean(item.key, item.defaultBoolean));
                }
            }
        }

        void putBoolean(String key, boolean value) {
            prefs.edit().putBoolean(key, value).apply();
        }

        void putInt(String key, int value) {
            prefs.edit().putInt(key, value).apply();
        }

        void putString(String key, String value) {
            prefs.edit().putString(key, value).apply();
        }
    }

    private static final class SettingsSection {
        final String key;
        final CharSequence title;
        final int iconRes;
        final ArrayList<SettingsItem> items = new ArrayList<>();
        boolean visible = true;

        SettingsSection(String key, CharSequence title, int iconRes) {
            this.key = key;
            this.title = title;
            this.iconRes = iconRes;
        }

        ArrayList<SettingsItem> visibleItems() {
            ArrayList<SettingsItem> visibleItems = new ArrayList<>();
            for (SettingsItem item : items) {
                if (item.visible) {
                    visibleItems.add(item);
                }
            }
            return visibleItems;
        }
    }

    private static final class SettingsItem {
        enum Type {
            SWITCH,
            LIST,
            SLIDER,
            TEXT,
            ACTION,
            WEB
        }

        String key;
        Type type;
        CharSequence title;
        CharSequence summary;
        String dependency;
        boolean dependencyDefault;
        SettingsItem dependencyItemRef;
        String url;
        boolean visible = true;
        boolean enabled = true;
        boolean defaultBoolean;
        int defaultInt;
        String defaultString;
        int min;
        int max;
        int step;
        int keyStep;
        int divisor;
        int decimalPlaces;
        CharSequence suffix;
        CharSequence dialogMessage;
        CharSequence[] entries = new CharSequence[0];
        CharSequence[] entryValues = new CharSequence[0];
        int iconRes = R.drawable.ic_axi_opt;

        boolean isEnabled(SettingsStore store) {
            if (!enabled) {
                return false;
            }
            if (TextUtils.isEmpty(dependency)) {
                return true;
            }
            if (dependencyItemRef != null && dependencyItemRef.type == Type.LIST) {
                String value = store.getString(dependencyItemRef);
                return !TextUtils.isEmpty(value) && !"off".equals(value) && !"false".equals(value) && !"0".equals(value);
            }
            return store.getBoolean(dependency, dependencyDefault);
        }

        CharSequence getSelectedEntry(SettingsStore store) {
            String selected = store.getString(this);
            for (int i = 0; i < entryValues.length; i++) {
                if (selected.equals(entryValues[i].toString())) {
                    return entries[i];
                }
            }
            return selected;
        }

        String formatSliderValue(int value) {
            String text;
            if (divisor != 1) {
                text = String.format((Locale) null,
                        "%." + decimalPlaces + "f",
                        value / (float) divisor);
            }
            else {
                text = Integer.toString(value);
            }
            return TextUtils.isEmpty(suffix) ? text : text + (suffix.length() > 1 ? " " : "") + suffix;
        }

        int round(int value) {
            if (value < min) {
                return min;
            }
            if (value > max) {
                return max;
            }
            if (step <= 1) {
                return value;
            }
            return ((value + (step - 1)) / step) * step;
        }

        void appendEntry(CharSequence entry, CharSequence value) {
            entries = Arrays.copyOf(entries, entries.length + 1);
            entryValues = Arrays.copyOf(entryValues, entryValues.length + 1);
            entries[entries.length - 1] = entry;
            entryValues[entryValues.length - 1] = value;
        }
    }

    private static final class SettingsRegistry {
        static ArrayList<SettingsSection> load(Context context) {
            ArrayList<SettingsSection> sections = new ArrayList<>();
            SettingsSection currentSection = null;
            Resources res = context.getResources();
            XmlResourceParser parser = res.getXml(R.xml.preferences);

            try {
                int event;
                while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                    if (event != XmlPullParser.START_TAG) {
                        continue;
                    }

                    String tag = parser.getName();
                    if ("PreferenceCategory".equals(tag)) {
                        String key = attrString(context, parser, "key");
                        CharSequence title = attrText(context, parser, "title");
                        if (TextUtils.isEmpty(key)) {
                            key = "category_" + sections.size();
                        }
                        currentSection = new SettingsSection(key, title, iconForSection(key, title, sections.size()));
                        sections.add(currentSection);
                    }
                    else if (currentSection != null) {
                        SettingsItem item = parseItem(context, parser, tag);
                        if (item != null && !TextUtils.isEmpty(item.key)) {
                            currentSection.items.add(item);
                        }
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException("Unable to load settings", e);
            } finally {
                parser.close();
            }
            return sections;
        }

        private static SettingsItem parseItem(Context context, XmlResourceParser parser, String tag) {
            SettingsItem item = new SettingsItem();
            item.key = attrString(context, parser, "key");
            item.title = attrText(context, parser, "title");
            item.summary = attrText(context, parser, "summary");
            item.dependency = attrString(context, parser, "dependency");
            item.defaultString = attrString(context, parser, "defaultValue");
            item.defaultBoolean = Boolean.parseBoolean(item.defaultString);
            item.defaultInt = parseInt(item.defaultString, 0);
            item.url = parser.getAttributeValue(null, "url");
            item.iconRes = iconForItem(item.key);

            if (tag.endsWith("SmallIconCheckboxPreference")) {
                item.type = SettingsItem.Type.SWITCH;
                item.defaultBoolean = PreferenceConfiguration.getDefaultSmallMode(context);
            }
            else if (tag.endsWith("CheckBoxPreference")) {
                item.type = SettingsItem.Type.SWITCH;
            }
            else if (tag.endsWith("LanguagePreference") || tag.endsWith("ListPreference")) {
                item.type = SettingsItem.Type.LIST;
                int entriesId = parser.getAttributeResourceValue(ANDROID_NS, "entries", 0);
                int valuesId = parser.getAttributeResourceValue(ANDROID_NS, "entryValues", 0);
                if (entriesId != 0) {
                    item.entries = context.getResources().getTextArray(entriesId);
                }
                if (valuesId != 0) {
                    item.entryValues = context.getResources().getTextArray(valuesId);
                }
            }
            else if (tag.endsWith("SeekBarPreference")) {
                item.type = SettingsItem.Type.SLIDER;
                item.min = parser.getAttributeIntValue(SEEKBAR_NS, "min", 0);
                item.max = parser.getAttributeIntValue(ANDROID_NS, "max", 100);
                item.step = parser.getAttributeIntValue(SEEKBAR_NS, "step", 1);
                item.keyStep = parser.getAttributeIntValue(SEEKBAR_NS, "keyStep", 0);
                item.divisor = parser.getAttributeIntValue(SEEKBAR_NS, "divisor", 1);
                item.decimalPlaces = Math.max(0, Math.min(4,
                        parser.getAttributeIntValue(SEEKBAR_NS, "decimals", 1)));
                item.suffix = attrText(context, parser, "text");
                item.dialogMessage = attrText(context, parser, "dialogMessage");
            }
            else if (tag.endsWith("EditTextPreference")) {
                item.type = SettingsItem.Type.TEXT;
                item.dialogMessage = attrText(context, parser, "dialogMessage");
            }
            else if (tag.endsWith("WebLauncherPreference")) {
                item.type = SettingsItem.Type.WEB;
            }
            else if ("Preference".equals(tag)) {
                item.type = SettingsItem.Type.ACTION;
            }
            else {
                return null;
            }
            return item;
        }

        private static CharSequence attrText(Context context, XmlResourceParser parser, String name) {
            int resId = parser.getAttributeResourceValue(ANDROID_NS, name, 0);
            if (resId != 0) {
                return context.getText(resId);
            }
            return parser.getAttributeValue(ANDROID_NS, name);
        }

        private static String attrString(Context context, XmlResourceParser parser, String name) {
            CharSequence text = attrText(context, parser, name);
            return text == null ? null : text.toString();
        }

        private static int parseInt(String value, int fallback) {
            if (TextUtils.isEmpty(value)) {
                return fallback;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return fallback;
            }
        }

        private static int iconForSection(String key, CharSequence title, int index) {
            String titleText = title == null ? "" : title.toString();
            if (key.contains("basic")) return R.drawable.ic_axi_screen;
            if (key.contains("audio")) return R.drawable.ic_axi_mic;
            if (key.contains("gamepad")) return R.drawable.ic_axi_game_pad;
            if (key.contains("input")) return R.drawable.ic_axi_mouse_left;
            if (key.contains("onscreen")) return R.drawable.ic_axi_game_control_dpad;
            if (titleText.contains("虚拟按键")) return R.drawable.ic_axi_vkeyboard;
            if (key.contains("keyboard")) return R.drawable.ic_axi_keyboard;
            if (key.contains("host")) return R.drawable.ic_axi_computer;
            if (key.contains("ui")) return R.drawable.ic_axi_app_setting;
            if (key.contains("screen")) return R.drawable.ic_axi_desktop;
            if (key.contains("advanced")) return R.drawable.ic_axi_other_setting;
            if (key.contains("back")) return R.drawable.ic_axi_clipboard_send;
            if (key.contains("about")) return R.drawable.ic_axi_app_about;
            if (key.contains("axixi")) return R.drawable.ic_axi_quick;
            return index % 2 == 0 ? R.drawable.ic_axi_opt : R.drawable.ic_axi_menu;
        }

        private static int iconForItem(String key) {
            if (key == null) return R.drawable.ic_axi_opt;
            int exactIcon = exactIconForItem(key);
            if (exactIcon != 0) return exactIcon;
            if (key.contains("resolution")) return R.drawable.ic_axi_game_pad_display;
            if (key.contains("fps")) return R.drawable.ic_axi_game_pad_fps;
            if (key.contains("bitrate")) return R.drawable.ic_axi_game_pad_bitrate;
            if (key.contains("hdr")) return R.drawable.ic_axi_hdr;
            if (key.contains("fsr") || key.contains("sharpness")) return R.drawable.ic_axi_zoom;
            if (key.contains("audio") || key.contains("haptics")) return R.drawable.ic_axi_mic;
            if (key.contains("rumble") || key.contains("vibrate")) return R.drawable.ic_axi_vibrate;
            if (key.contains("gamepad") || key.contains("controller")) return R.drawable.ic_axi_game_pad;
            if (key.contains("mouse")) return R.drawable.ic_axi_mouse_left;
            if (key.contains("touch")) return R.drawable.ic_axi_touch;
            if (key.contains("keyboard")) return R.drawable.ic_axi_keyboard;
            if (key.contains("import")) return R.drawable.ic_axi_down;
            if (key.contains("export")) return R.drawable.ic_axi_clipboard_send;
            if (key.contains("language")) return R.drawable.ic_axi_app_setting;
            if (key.contains("delete") || key.contains("disable")) return R.drawable.ic_axi_delete;
            if (key.contains("clipboard")) return R.drawable.ic_axi_clipboard_send;
            if (key.contains("pip") || key.contains("window")) return R.drawable.ic_axi_window;
            if (key.contains("host") || key.contains("computer")) return R.drawable.ic_axi_computer;
            if (key.contains("screen")) return R.drawable.ic_axi_desktop;
            return R.drawable.ic_axi_opt;
        }

        private static int exactIconForItem(String key) {
            if ("list_resolution".equals(key)) return R.drawable.ic_axi_game_pad_display;
            if ("list_resolution_aspect_ratio".equals(key)) return R.drawable.ic_axi_game_pad_zoom;
            if ("list_fps".equals(key)) return R.drawable.ic_axi_game_pad_fps;
            if (PreferenceConfiguration.BITRATE_PREF_STRING.equals(key) || "edit_diy_bitrate".equals(key)) {
                return R.drawable.ic_axi_game_pad_bitrate;
            }
            if ("frame_pacing".equals(key) || "enable_lowLatency_experiment".equals(key)) return R.drawable.ic_axi_performance;
            if ("list_fsr_target".equals(key) || "list_fsr_sharpness".equals(key)) return R.drawable.ic_axi_zoom;
            if ("list_fsr_hdr_output".equals(key) || "checkbox_enable_hdr".equals(key)) return R.drawable.ic_axi_hdr;
            if ("checkbox_stretch_video".equals(key) || "screen_gravity_list".equals(key)) return R.drawable.ic_axi_win_center;
            if ("checkbox_cutout_mode_video".equals(key)) return R.drawable.ic_axi_win_p;
            if ("checkbox_auto_screen_orientation".equals(key)) return R.drawable.ic_axi_switch_screen;
            if ("checkbox_ui_theme_white".equals(key) || "list_languages".equals(key)) return R.drawable.ic_axi_app_setting;

            if ("list_audio_config".equals(key) || "checkbox_enable_audiofx".equals(key)) return R.drawable.ic_axi_mic;
            if ("seekbar_deadzone".equals(key) || "checkbox_disable_trigger_deadzone".equals(key)) return R.drawable.ic_axi_joystick;
            if ("checkbox_multi_controller".equals(key)) return R.drawable.ic_axi_app_game_pad;
            if ("checkbox_usb_driver".equals(key) || "checkbox_usb_bind_all".equals(key)) return R.drawable.ic_axi_game_pad_xbox;
            if ("checkbox_mouse_emulation".equals(key)) return R.drawable.ic_axi_mouse_left;
            if ("analog_scrolling".equals(key)) return R.drawable.ic_axi_mouse_down;
            if ("checkbox_vibrate_fallback".equals(key) || "seekbar_vibrate_fallback_strength".equals(key)) return R.drawable.ic_axi_vibrate;
            if ("checkbox_flip_face_buttons".equals(key)) return R.drawable.ic_axi_game_pad_move;
            if ("checkbox_flip_rumble_ff".equals(key)) return R.drawable.ic_axi_virtual_gamepad_rumble;
            if ("checkbox_gamepad_touchpad_as_mouse".equals(key)) return R.drawable.ic_axi_touch;
            if ("checkbox_gamepad_motion_sensors".equals(key) || "checkbox_gamepad_motion_fallback".equals(key)) return R.drawable.ic_axi_game_pad_senser;

            if ("mouse_model_list_axi".equals(key)) return R.drawable.ic_axi_touch_all;
            if ("checkbox_mouse_local_cursor".equals(key)) return R.drawable.ic_axi_mouse_left_s;
            if ("checkbox_mouse_nav_buttons".equals(key)) return R.drawable.ic_axi_mouse_right;
            if ("checkbox_absolute_mouse_mode".equals(key)) return R.drawable.ic_axi_touch_center;
            if (PreferenceConfiguration.BAROMETER_FORCE_PRESS_PREF_STRING.equals(key)) return R.drawable.ic_axi_touch;
            if (PreferenceConfiguration.BAROMETER_FORCE_PRESS_THRESHOLD_PREF_STRING.equals(key)) return R.drawable.ic_axi_touch_sensitivity;
            if (PreferenceConfiguration.BAROMETER_FORCE_PRESS_MIN_DURATION_PREF_STRING.equals(key)) return R.drawable.ic_axi_touch_sensitivity;
            if ("checkbox_clipboard_sync".equals(key)) return R.drawable.ic_axi_clipboard_send;

            if ("checkbox_show_onscreen_controls".equals(key)) return R.drawable.ic_axi_game_control_dpad;
            if ("gamepad_axi_list".equals(key)) return R.drawable.ic_axi_game_pad_active;
            if ("checkbox_vibrate_osc".equals(key) || "checkbox_vibrate_keyboard".equals(key)) return R.drawable.ic_axi_vibrate;
            if ("seekbar_osc_opacity".equals(key)) return R.drawable.ic_axi_touch_sensitivity;
            if ("checkbox_rocker_click_L3R3".equals(key)) return R.drawable.ic_axi_free_rocker;
            if ("import_gamepad_file".equals(key)) return R.drawable.ic_axi_down;
            if ("export_gamepad_file".equals(key)) return R.drawable.ic_axi_clipboard_send;

            if ("checkbox_enable_sops".equals(key)) return R.drawable.ic_axi_performance;
            if ("checkbox_host_audio".equals(key)) return R.drawable.ic_axi_mic;
            if ("checkbox_enable_pip".equals(key)) return R.drawable.ic_axi_window;
            if ("checkbox_small_icon_mode".equals(key)) return R.drawable.ic_axi_app_setting;
            if ("checkbox_unlock_fps".equals(key) || "checkbox_reduce_refresh_rate".equals(key)) return R.drawable.ic_axi_game_pad_fps;
            if ("checkbox_disable_warnings".equals(key)) return R.drawable.ic_axi_delete;
            if ("video_format".equals(key) || "checkbox_full_range".equals(key)) return R.drawable.ic_axi_screen;
            if ("checkbox_enable_perf_overlay".equals(key) || "checkbox_enable_perf_overlay_lite".equals(key) ||
                    "checkbox_enable_perf_overlay_lite_dialog".equals(key) || "checkbox_enable_perf_overlay_lite_ext".equals(key) ||
                    "performance_overlayLite_magin_top".equals(key)) return R.drawable.ic_axi_performance;
            if ("checkbox_enable_post_stream_toast".equals(key)) return R.drawable.ic_axi_app_about;

            if ("checkbox_enable_quit_dialog".equals(key)) return R.drawable.ic_axi_menu;
            if ("edit_diy_w_h".equals(key)) return R.drawable.ic_axi_game_pad_display;
            if ("checkbox_enable_portrait".equals(key)) return R.drawable.ic_axi_switch_screen;
            if ("checkbox_enable_joyconfix".equals(key)) return R.drawable.ic_axi_ns;
            if ("checkbox_gamepad_enable_battery_report".equals(key)) return R.drawable.ic_axi_game_pad_battery;
            if ("checkbox_enable_ax_floating".equals(key)) return R.drawable.ic_axi_quick;
            if ("seekbar_keyboard_axi_opacity".equals(key)) return R.drawable.ic_axi_touch_sensitivity;
            if ("seekbar_keyboard_axi_height".equals(key)) return R.drawable.ic_axi_keyboard;
            if ("checkbox_enable_keyboard_axi_combination".equals(key)) return R.drawable.ic_axi_keyboard_list;
            if ("checkbox_enable_exdisplay".equals(key)) return R.drawable.ic_axi_desktop;
            if ("checkbox_enable_device_rumble".equals(key)) return R.drawable.ic_axi_vibrate;
            if ("checkbox_enable_virtual_motion".equals(key)) return R.drawable.ic_axi_game_pad_senser;
            if ("checkbox_enable_clear_default_special_button".equals(key)) return R.drawable.ic_axi_delete;
            if ("checkbox_enable_game_manager_quest".equals(key)) return R.drawable.ic_axi_game_pad_disable;
            if ("import_switch_button_file".equals(key)) return R.drawable.ic_axi_down;
            if ("checkbox_enable_accessibility_show_log".equals(key)) return R.drawable.ic_axi_keyboard_list;

            if ("checkbox_enable_keyboard".equals(key)) return R.drawable.ic_axi_vkeyboard;
            if ("keyboard_axi_list".equals(key)) return R.drawable.ic_axi_keyboard_list;
            if ("import_keyboard_file".equals(key)) return R.drawable.ic_axi_down;
            if ("export_keyboard_file".equals(key)) return R.drawable.ic_axi_clipboard_send;

            if ("checkbox_enable_audio_haptics".equals(key) || "seekbar_audio_haptics_strength".equals(key)) return R.drawable.ic_axi_vibrate;
            if ("list_audio_haptics_output_target".equals(key)) return R.drawable.ic_axi_game_pad_device;
            if ("list_audio_haptics_voice_filter".equals(key)) return R.drawable.ic_axi_mic;
            if ("checkbox_audio_haptics_keep_controller_rumble".equals(key)) return R.drawable.ic_axi_virtual_gamepad_rumble;

            if ("export_computers_data_file".equals(key) || "import_computers_data_file".equals(key)) return R.drawable.ic_axi_computer;
            if ("export_https_data_crt_file".equals(key) || "import_https_data_crt_file".equals(key) ||
                    "export_https_data_key_file".equals(key) || "import_https_data_key_file".equals(key)) return R.drawable.ic_axi_lock_screen;
            if ("checkbox_enable_screen_bg".equals(key) || "import_image_file_key".equals(key)) return R.drawable.ic_axi_desktop;
            if ("checkbox_enable_screen_obscure".equals(key)) return R.drawable.ic_axi_zoom;
            if ("change_screen_label_key".equals(key)) return R.drawable.ic_axi_keyboard;
            if ("checkbox_enable_pass_menu".equals(key)) return R.drawable.ic_axi_game_pad_pass;
            return 0;
        }
    }
}
