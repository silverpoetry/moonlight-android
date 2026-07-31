package com.limelight.preferences;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaCodecInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
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
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;

import com.limelight.AboutActivity;
import com.limelight.LimeLog;
import com.limelight.PcView;
import com.limelight.R;
import com.limelight.binding.video.MediaCodecHelper;
import com.limelight.settings.SettingsScreenIds;
import com.limelight.settings.android.AndroidAppLocale;
import com.limelight.settings.android.AndroidAppPresentationDefaults;
import com.limelight.settings.android.AndroidAppPresentationSettingsLoader;
import com.limelight.settings.android.AndroidHdrCompatibility;
import com.limelight.settings.android.AndroidStreamDefaults;
import com.limelight.settings.app.AppPresentationSettings;
import com.limelight.settings.app.AppPresentationSettingKeys;
import com.limelight.settings.input.InputSettingKeys;
import com.limelight.settings.stream.StreamDisplayGeometry;
import com.limelight.settings.stream.StreamResolutionCodec;
import com.limelight.settings.stream.StreamResolutionSettingKeys;
import com.limelight.settings.stream.StreamVideoSettingKeys;
import com.limelight.settings.transfer.TransferSettingKeys;
import com.limelight.settings.transfer.TransferSettings;
import com.limelight.settings.transfer.TransferSettingsLoader;
import com.limelight.settings.virtualcontrols.VirtualControlSettingKeys;
import com.limelight.utils.BackNavigationRegistration;
import com.limelight.utils.Dialog;
import com.limelight.utils.HelpLauncher;
import com.limelight.utils.UiHelper;
import com.limelight.utils.UiToast;
import com.limelight.virtualcontrols.layout.android.AndroidVirtualControlLayoutRepository;

import java.math.BigDecimal;
import java.util.ArrayList;

import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE;

public class StreamSettings extends Activity {
    private static final int MAX_BITRATE_KBPS = 50000;
    private static final int FEATURED_SECTION_INDEX = -1;
    private static final String CUSTOM_BITRATE_EDITOR_KEY =
            SettingsScreenIds.EDITOR_VIDEO_BITRATE_MBPS;
    private static final String EXTRA_SECTION_INDEX = "com.limelight.preferences.StreamSettings.SECTION_INDEX";
    private static final String[] ROOT_FEATURED_SETTING_KEYS = new String[] {
            StreamResolutionSettingKeys.RESOLUTION.getName(),
            StreamResolutionSettingKeys.ASPECT_RATIO.getName(),
            StreamResolutionSettingKeys.FPS.getName(),
            StreamVideoSettingKeys.BITRATE_KBPS.getName(),
            InputSettingKeys.TOUCH_MODE.getName(),
            TransferSettingKeys.CLIPBOARD_SYNC.getName(),
    };

    private AppPresentationSettings previousPresentationSettings;
    private int previousDisplayPixelCount;
    private SettingsStore store;
    private ArrayList<SettingsSection> sections = new ArrayList<>();
    private SettingsScreenModel screenModel =
            new SettingsScreenModel(sections);
    private FrameLayout mainContainer;
    private LinearLayout outerContainer;
    private LinearLayout wideSectionList;
    private FrameLayout wideItemContainer;
    private TextView titleView;
    private TextView subtitleView;
    private ImageButton backButton;
    private ScrollView activeContentScrollView;
    private final ArrayList<RenderedSettingsRow>
            renderedSettingsRows = new ArrayList<>();
    private int selectedSectionIndex = -1;
    private int nativeResolutionStartIndex = Integer.MAX_VALUE;
    private boolean nativeFramerateShown;
    private boolean wideLayout;
    private boolean sectionActivity;
    private BackNavigationRegistration backNavigationRegistration;
    private SettingsDocumentController documentController;

    // HACK for Android 9
    static DisplayCutout displayCutoutP;

    void reloadSettings() {
        int previousScrollY = activeContentScrollView == null
                ? 0
                : activeContentScrollView.getScrollY();
        boolean restoreScroll =
                activeContentScrollView != null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Display.Mode mode = getWindowManager().getDefaultDisplay().getMode();
            previousDisplayPixelCount = mode.getPhysicalWidth() * mode.getPhysicalHeight();
        }

        sections = SettingsRegistry.load(this);
        screenModel = new SettingsScreenModel(sections);
        store = new SettingsStore(this);
        screenModel.linkDependencyDefaults();
        nativeResolutionStartIndex = Integer.MAX_VALUE;
        nativeFramerateShown = false;
        initializeRuntimeSettings();
        screenModel.removeEmptySections();
        selectedSectionIndex =
                screenModel.clampSelectedSection(selectedSectionIndex);
        render();
        if (restoreScroll && activeContentScrollView != null) {
            ScrollView restoredScrollView =
                    activeContentScrollView;
            restoredScrollView.post(
                    () -> restoredScrollView.scrollTo(
                            0,
                            previousScrollY));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        previousPresentationSettings =
                AndroidAppPresentationSettingsLoader.load(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                !previousPresentationSettings.usesLightTheme()) {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);

        AndroidAppLocale.apply(this);
        store = new SettingsStore(this);
        AndroidVirtualControlLayoutRepository layoutRepository =
                new AndroidVirtualControlLayoutRepository(this);
        documentController = new SettingsDocumentController(
                this,
                store.repository,
                layoutRepository,
                this::reloadSettings);
        sectionActivity = getIntent().hasExtra(EXTRA_SECTION_INDEX);
        selectedSectionIndex = getIntent().getIntExtra(EXTRA_SECTION_INDEX, -1);

        setContentView(createRootView());
        configureImmersiveSettingsWindow();

        if (previousPresentationSettings.usesLightTheme()) {
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
        if (documentController != null) {
            documentController.destroy();
            documentController = null;
        }
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
            AppPresentationSettings newSettings =
                    AndroidAppPresentationSettingsLoader.load(this);
            if (!newSettings.getLanguage().equals(
                    previousPresentationSettings.getLanguage())) {
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
        renderedSettingsRows.clear();
        titleView.setText(R.string.settings_title);
        subtitleView.setText(getCurrentProfileSummary());
        backButton.setVisibility(View.VISIBLE);

        ScrollView scroll = createScrollView();
        activeContentScrollView = scroll;
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
        renderedSettingsRows.clear();
        titleView.setText(R.string.settings_featured_settings);
        subtitleView.setText(getCurrentProfileSummary());
        backButton.setVisibility(View.VISIBLE);

        ScrollView scroll = createScrollView();
        activeContentScrollView = scroll;
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
        renderedSettingsRows.clear();
        SettingsSection section = sections.get(sectionIndex);
        titleView.setText(section.title);
        subtitleView.setText(getResources().getQuantityString(
                R.plurals.settings_item_count, section.visibleItems().size(), section.visibleItems().size()));
        backButton.setVisibility(View.VISIBLE);

        ScrollView scroll = createScrollView();
        activeContentScrollView = scroll;
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
                        control instanceof CompoundButton) {
                    control.performClick();
                    return;
                }
                handleItemClick(item);
            }
        });
        renderedSettingsRows.add(
                new RenderedSettingsRow(
                        item,
                        row,
                        icon,
                        title,
                        control));
        return row;
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private View createControlView(final SettingsItem item) {
        if (item.type == SettingsItem.Type.SWITCH) {
            Switch switchView = new Switch(this);
            switchView.setShowText(false);
            switchView.setMinimumWidth(dp(52));
            switchView.setChecked(store.getBoolean(item));
            switchView.setEnabled(item.isEnabled(store));
            tintSwitch(switchView);
            switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    store.putBoolean(item, isChecked);
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
                value.setText(store.getText(item));
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
                boolean checked = !store.getBoolean(item);
                store.putBoolean(item, checked);
                afterItemChanged(item, checked, true);
                break;
            case LIST:
                if (AppPresentationSettingKeys.LANGUAGE
                        .getName()
                        .equals(item.key) &&
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
                        store.putString(item, value);
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
                store.putInt(item, progress);
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
        input.setText(store.getText(item));
        input.setSingleLine(true);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(0x88FFFFFF);
        input.setSelectAllOnFocus(true);
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        if (item.isCustomBitrateEditor()) {
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
                if (commitTextValue(item, value)) {
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
        if (StreamResolutionSettingKeys.RESOLUTION
                .getName()
                .equals(item.key)) {
            boolean isNativeRes = true;
            for (int i = 0; i < item.entryValues.length; i++) {
                if (value.equals(item.entryValues[i].toString()) && i < nativeResolutionStartIndex) {
                    isNativeRes = false;
                    break;
                }
            }
            store.put(
                    StreamResolutionSettingKeys.SELECTION,
                    isNativeRes ?
                            StreamResolutionCodec
                                    .SELECTION_CUSTOM_OR_NATIVE :
                            StreamResolutionCodec
                                    .SELECTION_PRESET);
        }

        if (StreamResolutionSettingKeys.FPS
                .getName()
                .equals(item.key) &&
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

    private boolean commitTextValue(
            SettingsItem item,
            String value) {
        if (item.isCustomBitrateEditor()) {
            if (TextUtils.isEmpty(value)) {
                UiToast.makeText(this, "请输入0-9999的数值。", UiToast.LENGTH_SHORT).show();
                return false;
            }
            try {
                BigDecimal bitrateMbps =
                        new BigDecimal(value);
                if (bitrateMbps.signum() < 0 ||
                        bitrateMbps.compareTo(
                                BigDecimal.valueOf(9999)) > 0) {
                    throw new ArithmeticException(
                            "Bitrate is outside the editor range");
                }
                int bitrateKbps = bitrateMbps
                        .movePointRight(3)
                        .intValueExact();
                store.put(
                        StreamVideoSettingKeys.BITRATE_KBPS,
                        bitrateKbps);
            } catch (NumberFormatException |
                    ArithmeticException e) {
                UiToast.makeText(this, "请输入0-9999的数值。", UiToast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        }
        store.putString(item, value);
        return true;
    }

    private void afterItemChanged(SettingsItem item, Object value, boolean allowSwitchAnimation) {
        if (InputSettingKeys.BAROMETER_FORCE_PRESS
                .getName()
                .equals(item.key)) {
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

        if (StreamVideoSettingKeys.UNLOCK_FPS
                .getName()
                .equals(item.key)) {
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
        for (RenderedSettingsRow renderedRow :
                renderedSettingsRows) {
            renderedRow.refresh(store);
        }
    }

    // This Activity intentionally uses the framework Material theme. A
    // compat switch has no themed thumb or track in that environment.
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private void tintSwitch(Switch switchView) {
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
        if (documentController != null) {
            documentController.perform(key);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void launchNativeLanguageSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APP_LOCALE_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent, null);
        } catch (ActivityNotFoundException e) {
            SettingsItem item = findItem(
                    AppPresentationSettingKeys.LANGUAGE
                            .getName());
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
                TransferSettingKeys
                        .CLIPBOARD_FILE_DIRECTORY_URI
                        .getName());
        if (item == null) {
            return;
        }
        TransferSettings settings =
                TransferSettingsLoader.load(store.repository);
        String value = settings.getClipboardFileDirectoryUri();
        if (!settings.hasClipboardFileDirectory()) {
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
        SettingsItem bitrate = findItem(
                StreamVideoSettingKeys.BITRATE_KBPS.getName());
        if (bitrate == null) {
            return;
        }

        int defaultBitrateKbps =
                AndroidStreamDefaults.getDefaultBitrateKbps(this);
        bitrate.displayDefaultInteger = defaultBitrateKbps;
        SettingsItem customBitrateEditor =
                findItem(CUSTOM_BITRATE_EDITOR_KEY);
        if (customBitrateEditor != null) {
            customBitrateEditor.displayDefaultInteger =
                    defaultBitrateKbps;
        }
        bitrate.max = MAX_BITRATE_KBPS;
        if (bitrate.keyStep <= 0) {
            bitrate.keyStep = 1000;
        }
    }

    private void applyDeviceVisibility() {
        SettingsVisibilityPolicy.Result visibility =
                SettingsVisibilityPolicy.evaluate(
                        AndroidSettingsDeviceCapabilities.collect(this),
                        store.get(
                                InputSettingKeys
                                        .BAROMETER_FORCE_PRESS));
        screenModel.applyVisibility(visibility);
    }

    private void addCustomResolution() {
        String diy = store.get(
                StreamVideoSettingKeys.CUSTOM_RESOLUTION_TEXT);
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
                    removeValue(
                            StreamResolutionSettingKeys.RESOLUTION
                                    .getName(),
                            StreamResolutionCodec.RESOLUTION_4K,
                            StreamResolutionCodec.RESOLUTION_1440P);
                }
                if (maxSupportedResW < 2560) {
                    removeValue(
                            StreamResolutionSettingKeys.RESOLUTION
                                    .getName(),
                            StreamResolutionCodec.RESOLUTION_1440P,
                            StreamResolutionCodec.RESOLUTION_1080P);
                }
                if (maxSupportedResW < 1920) {
                    removeValue(
                            StreamResolutionSettingKeys.RESOLUTION
                                    .getName(),
                            StreamResolutionCodec.RESOLUTION_1080P,
                            StreamResolutionCodec.RESOLUTION_720P);
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

        if (!store.repository.get(
                StreamVideoSettingKeys.UNLOCK_FPS)) {
            if (maxSupportedFps < 118) {
                removeValue(
                        StreamResolutionSettingKeys.FPS.getName(),
                        "120",
                        "90");
            }
            if (maxSupportedFps < 88) {
                removeValue(
                        StreamResolutionSettingKeys.FPS.getName(),
                        "90",
                        "60");
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
        SettingsItem hdrItem = findItem(
                StreamVideoSettingKeys.HDR_ENABLED.getName());
        if (hdrItem == null) {
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            hideItem(StreamVideoSettingKeys.HDR_ENABLED.getName());
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
            hideItem(StreamVideoSettingKeys.HDR_ENABLED.getName());
        }
        else if (!AndroidHdrCompatibility
                .isHdrStreamingAllowed()) {
            hdrItem.enabled = false;
            store.put(
                    StreamVideoSettingKeys.HDR_ENABLED,
                    false);
            hdrItem.summary = "Update the firmware on your NVIDIA SHIELD Android TV to enable HDR";
        }
    }

    private void addNativeResolutionEntries(int nativeWidth, int nativeHeight, boolean insetsRemoved) {
        if (StreamDisplayGeometry.isSquarish(
                nativeWidth,
                nativeHeight)) {
            addNativeResolutionEntry(nativeHeight, nativeWidth, insetsRemoved, true);
        }
        addNativeResolutionEntry(nativeWidth, nativeHeight, insetsRemoved, false);
    }

    private void addNativeResolutionEntry(int nativeWidth, int nativeHeight, boolean insetsRemoved, boolean portrait) {
        SettingsItem item = findItem(
                StreamResolutionSettingKeys.RESOLUTION.getName());
        if (item == null) {
            return;
        }

        String newName = getResources().getString(insetsRemoved ?
                R.string.resolution_prefix_native_fullscreen :
                R.string.resolution_prefix_native);

        if (StreamDisplayGeometry.isSquarish(
                nativeWidth,
                nativeHeight)) {
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

        SettingsItem item = findItem(
                StreamResolutionSettingKeys.FPS.getName());
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
            if (StreamResolutionSettingKeys.RESOLUTION
                    .getName()
                    .equals(preferenceKey)) {
                store.put(
                        StreamResolutionSettingKeys.SELECTION,
                        StreamResolutionCodec
                                .isStandardResolutionPreset(
                                        fallbackValue) ?
                                StreamResolutionCodec
                                        .SELECTION_PRESET :
                                StreamResolutionCodec
                                        .SELECTION_CUSTOM_OR_NATIVE);
            }
            store.putString(item, fallbackValue);
        }
    }

    private SettingsItem findItem(String key) {
        return screenModel.findItem(key);
    }

    private void hideItem(String key) {
        screenModel.hideItem(key);
    }

    private String getCurrentProfileSummary() {
        SettingsItem resolution = findItem(
                StreamResolutionSettingKeys.RESOLUTION.getName());
        SettingsItem fps = findItem(
                StreamResolutionSettingKeys.FPS.getName());
        SettingsItem bitrate = findItem(
                StreamVideoSettingKeys.BITRATE_KBPS.getName());

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
        if (documentController != null) {
            documentController.handleActivityResult(
                    requestCode,
                    resultCode,
                    data);
        }
    }

    private static final class RenderedSettingsRow {
        private final SettingsItem item;
        private final View row;
        private final ImageView icon;
        private final TextView title;
        private final View control;

        RenderedSettingsRow(
                SettingsItem item,
                View row,
                ImageView icon,
                TextView title,
                View control) {
            this.item = item;
            this.row = row;
            this.icon = icon;
            this.title = title;
            this.control = control;
        }

        void refresh(SettingsStore store) {
            boolean enabled = item.isEnabled(store);
            row.setEnabled(enabled);
            row.setAlpha(enabled ? 1.0f : 0.55f);
            icon.setAlpha(enabled ? 0.86f : 0.35f);
            title.setTextColor(enabled
                    ? Color.WHITE
                    : 0x80FFFFFF);

            if (control == null) {
                return;
            }
            control.setEnabled(enabled);
            if (!(control instanceof TextView) ||
                    control instanceof CompoundButton) {
                return;
            }

            TextView valueView = (TextView) control;
            switch (item.type) {
                case LIST:
                    valueView.setText(
                            item.getSelectedEntry(store));
                    break;
                case SLIDER:
                    valueView.setText(item.formatSliderValue(
                            item.round(store.getInt(item))));
                    break;
                case TEXT:
                    valueView.setText(store.getText(item));
                    break;
                default:
                    break;
            }
        }
    }

}
