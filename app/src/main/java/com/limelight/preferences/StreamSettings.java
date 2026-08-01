package com.limelight.preferences;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.WindowInsets;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;

import com.limelight.AboutActivity;
import com.limelight.PcView;
import com.limelight.R;
import com.limelight.settings.android.AndroidAppLocale;
import com.limelight.settings.android.AndroidAppPresentationSettingsLoader;
import com.limelight.settings.app.AppPresentationSettingKeys;
import com.limelight.settings.app.AppPresentationSettings;
import com.limelight.settings.input.InputSettingKeys;
import com.limelight.settings.stream.StreamResolutionSettingKeys;
import com.limelight.settings.stream.StreamVideoSettingKeys;
import com.limelight.utils.BackNavigationRegistration;
import com.limelight.utils.Dialog;
import com.limelight.utils.HelpLauncher;
import com.limelight.utils.UiHelper;
import com.limelight.virtualcontrols.layout.android.AndroidVirtualControlLayoutRepository;

import java.util.ArrayList;

import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE;

public class StreamSettings extends Activity {
    private static final String STATE_SECTION_KEY =
            "settings.selected_section_key";
    private static final String STATE_SECTION_DETAIL_VISIBLE =
            "settings.section_detail_visible";

    private AppPresentationSettings previousPresentationSettings;
    private int previousDisplayPixelCount;
    private SettingsStore store;
    private ArrayList<SettingsSection> sections = new ArrayList<>();
    private SettingsScreenModel screenModel =
            new SettingsScreenModel(sections);
    private int selectedSectionIndex = -1;
    private String selectedSectionKey;
    private String nativeFrameRateValue;
    private boolean sectionDetailVisible;
    private BackNavigationRegistration backNavigationRegistration;
    private SettingsDocumentController documentController;
    private SettingsMutationController mutationController;
    private SettingsChangeEffectScheduler changeEffectScheduler;
    private SettingsDialogPresenter dialogPresenter;
    private SettingsScreenRenderer screenRenderer;

    // Android 9 exposes the cutout only after the window is attached.
    static DisplayCutout displayCutoutP;

    void reloadSettings() {
        Integer previousScrollY = screenRenderer == null
                ? null
                : screenRenderer.captureScrollY();
        if (screenRenderer != null && screenRenderer.hasContent()) {
            selectedSectionKey = screenModel.getSectionKey(
                    screenRenderer.getSelectedSectionIndex());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Display.Mode mode = getWindowManager()
                    .getDefaultDisplay()
                    .getMode();
            previousDisplayPixelCount =
                    mode.getPhysicalWidth() * mode.getPhysicalHeight();
        }

        sections = SettingsRegistry.load(this);
        screenModel = new SettingsScreenModel(sections);
        screenModel.linkDependencyDefaults();
        nativeFrameRateValue = null;
        initializeRuntimeSettings();
        screenModel.removeEmptySections();
        selectedSectionIndex =
                screenModel.findSectionIndex(selectedSectionKey);
        if (sectionDetailVisible && selectedSectionIndex < 0) {
            sectionDetailVisible = false;
        }
        if (screenRenderer != null) {
            screenRenderer.setContent(
                    createScreenState(),
                    selectedSectionIndex,
                    sectionDetailVisible,
                    getCurrentProfileSummary());
            screenRenderer.render();
            screenRenderer.restoreScrollY(previousScrollY);
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
        mutationController = new SettingsMutationController(store);
        changeEffectScheduler = new SettingsChangeEffectScheduler(
                () -> {
                    if (!isFinishing()) {
                        reloadSettings();
                    }
                },
                () -> {
                    if (!isFinishing()) {
                        refreshAfterItemChanged();
                    }
                });
        dialogPresenter = createDialogPresenter();
        documentController = new SettingsDocumentController(
                this,
                store.repository,
                layoutRepository,
                this::reloadSettings);
        if (savedInstanceState != null) {
            selectedSectionKey = savedInstanceState.getString(
                    STATE_SECTION_KEY);
            sectionDetailVisible = savedInstanceState.getBoolean(
                    STATE_SECTION_DETAIL_VISIBLE,
                    false);
        }
        screenRenderer = createScreenRenderer();

        setContentView(screenRenderer.createRootView());
        configureImmersiveSettingsWindow();
        if (previousPresentationSettings.usesLightTheme()) {
            UiHelper.setStatusBarLightMode(getWindow(), true);
        }
        registerBackCallback();
    }

    private SettingsDialogPresenter createDialogPresenter() {
        return new SettingsDialogPresenter(
                this,
                store,
                new SettingsDialogPresenter.Listener() {
                    @Override
                    public void onListValueSelected(
                            SettingsItem item,
                            String value) {
                        handleListValueSelected(item, value);
                    }

                    @Override
                    public void onSliderValueSelected(
                            SettingsItem item,
                            int value) {
                        applyChangeResult(
                                mutationController.changeInteger(
                                        item,
                                        value));
                    }

                    @Override
                    public CharSequence onTextValueSubmitted(
                            SettingsItem item,
                            String value) {
                        return handleTextValueSubmitted(item, value);
                    }
                });
    }

    private SettingsScreenRenderer createScreenRenderer() {
        return new SettingsScreenRenderer(
                this,
                new SettingsScreenRenderer.Listener() {
                    @Override
                    public void onBackRequested() {
                        handleBackNavigation();
                    }

                    @Override
                    public void onSectionRequested(int sectionIndex) {
                        openSection(sectionIndex);
                    }

                    @Override
                    public void onItemRequested(String itemId) {
                        handleItemClick(requireItem(itemId));
                    }

                    @Override
                    public void onSwitchChanged(
                            String itemId,
                            boolean checked) {
                        applyChangeResult(
                                mutationController.changeBoolean(
                                        requireItem(itemId),
                                        checked,
                                        true));
                    }
                });
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
            WindowInsets insets = getWindow()
                    .getDecorView()
                    .getRootWindowInsets();
            if (insets != null) {
                displayCutoutP = insets.getDisplayCutout();
            }
        }
        reloadSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (screenRenderer != null && screenRenderer.hasContent()) {
            reloadSettings();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Display.Mode mode = getWindowManager()
                    .getDefaultDisplay()
                    .getMode();
            int displayPixelCount =
                    mode.getPhysicalWidth() * mode.getPhysicalHeight();
            if (displayPixelCount != previousDisplayPixelCount) {
                reloadSettings();
                return;
            }
        }
        if (screenRenderer != null) {
            screenRenderer.render();
            if (screenRenderer.isWideLayout() &&
                    sectionDetailVisible) {
                sectionDetailVisible = false;
                screenRenderer.setContent(
                        createScreenState(),
                        selectedSectionIndex,
                        false,
                        getCurrentProfileSummary());
            }
        }
    }

    @Override
    public void onBackPressed() {
        handleBackNavigation();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SECTION_KEY, selectedSectionKey);
        outState.putBoolean(
                STATE_SECTION_DETAIL_VISIBLE,
                sectionDetailVisible);
    }

    @Override
    protected void onDestroy() {
        if (screenRenderer != null) {
            screenRenderer.destroy();
            screenRenderer = null;
        }
        if (dialogPresenter != null) {
            dialogPresenter.destroy();
            dialogPresenter = null;
        }
        if (changeEffectScheduler != null) {
            changeEffectScheduler.destroy();
            changeEffectScheduler = null;
        }
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
                BackNavigationRegistration.register(
                        this,
                        this::handleBackNavigation);
    }

    private void handleBackNavigation() {
        if (sectionDetailVisible &&
                screenRenderer != null &&
                !screenRenderer.isWideLayout()) {
            sectionDetailVisible = false;
            selectedSectionKey = null;
            selectedSectionIndex = -1;
            screenRenderer.setContent(
                    createScreenState(),
                    selectedSectionIndex,
                    false,
                    getCurrentProfileSummary());
            screenRenderer.render();
            return;
        }
        finishAndApplyLanguage();
    }

    private void openSection(int sectionIndex) {
        if (sectionIndex ==
                SettingsScreenRenderer.FEATURED_SECTION_INDEX) {
            selectedSectionKey = null;
            selectedSectionIndex =
                    SettingsScreenRenderer.FEATURED_SECTION_INDEX;
            sectionDetailVisible = false;
            screenRenderer.setContent(
                    createScreenState(),
                    selectedSectionIndex,
                    false,
                    getCurrentProfileSummary());
            screenRenderer.render();
            return;
        }
        String sectionKey = screenModel.getSectionKey(sectionIndex);
        if (sectionKey == null) {
            return;
        }
        selectedSectionKey = sectionKey;
        selectedSectionIndex = sectionIndex;
        sectionDetailVisible = !screenRenderer.isWideLayout();
        screenRenderer.setContent(
                createScreenState(),
                selectedSectionIndex,
                sectionDetailVisible,
                getCurrentProfileSummary());
        screenRenderer.render();
    }

    private void finishAndApplyLanguage() {
        finish();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            AppPresentationSettings newSettings =
                    AndroidAppPresentationSettingsLoader.load(this);
            if (!newSettings.getLanguage().equals(
                    previousPresentationSettings.getLanguage())) {
                Intent intent = new Intent(this, PcView.class);
                intent.setFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TASK |
                                Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent, null);
            }
        }
    }

    private void configureImmersiveSettingsWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams layoutParams =
                    getWindow().getAttributes();
            layoutParams.layoutInDisplayCutoutMode =
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                            ? WindowManager.LayoutParams
                                    .LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                            : WindowManager.LayoutParams
                                    .LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(layoutParams);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().setNavigationBarDividerColor(
                    Color.TRANSPARENT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
        getWindow().getDecorView().setSystemUiVisibility(
                SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        screenRenderer.applyWindowPadding();
    }

    private void handleItemClick(SettingsItem item) {
        switch (item.type) {
            case SWITCH:
                boolean checked = !store.getBoolean(item);
                applyChangeResult(
                        mutationController.changeBoolean(
                                item,
                                checked,
                                true));
                break;
            case LIST:
            case INTEGER_LIST:
                if (AppPresentationSettingKeys.LANGUAGE
                        .getName()
                        .equals(item.key) &&
                        Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.TIRAMISU) {
                    launchNativeLanguageSettings();
                }
                else {
                    dialogPresenter.showList(item);
                }
                break;
            case SLIDER:
                dialogPresenter.showSlider(item);
                break;
            case TEXT:
                dialogPresenter.showText(item);
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

    private void handleListValueSelected(
            SettingsItem item,
            String value) {
        SettingsMutationController.ChangeResult result;
        if (item.type == SettingsItem.Type.INTEGER_LIST) {
            result = mutationController.changeInteger(
                    item,
                    Integer.parseInt(value));
        }
        else {
            result = mutationController.changeList(
                    item,
                    value,
                    nativeFrameRateValue);
        }
        if (result.shouldShowNativeFrameRateWarning()) {
            Dialog.displayDialog(
                    this,
                    getString(R.string.title_native_fps_dialog),
                    getString(R.string.text_native_res_dialog),
                    false);
        }
        applyChangeResult(result);
    }

    private CharSequence handleTextValueSubmitted(
            SettingsItem item,
            String value) {
        SettingsMutationController.ChangeResult result =
                mutationController.changeText(item, value);
        if (!result.isAccepted()) {
            return getText(R.string.settings_invalid_bitrate);
        }
        applyChangeResult(result);
        return null;
    }

    private void applyChangeResult(
            SettingsMutationController.ChangeResult result) {
        if (!result.isAccepted()) {
            throw new IllegalArgumentException(
                    "Cannot apply a rejected settings change");
        }
        if (changeEffectScheduler != null) {
            changeEffectScheduler.schedule(
                    result.getEffect());
        }
    }

    private void refreshAfterItemChanged() {
        if (screenRenderer != null) {
            screenRenderer.updateState(
                    createScreenState(),
                    getCurrentProfileSummary());
        }
    }

    private void performAction(String key) {
        if (documentController != null) {
            documentController.perform(key);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void launchNativeLanguageSettings() {
        try {
            Intent intent = new Intent(
                    Settings.ACTION_APP_LOCALE_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(Uri.parse(
                    "package:" + getPackageName()));
            startActivity(intent, null);
        }
        catch (ActivityNotFoundException error) {
            SettingsItem item = findItem(
                    AppPresentationSettingKeys.LANGUAGE.getName());
            if (item != null) {
                dialogPresenter.showList(item);
            }
        }
    }

    private void initializeRuntimeSettings() {
        new SettingsRuntimeScreenController(
                screenModel,
                new AndroidSettingsRuntimeText(this))
                .apply(AndroidSettingsRuntimeValues.collect(
                        this,
                        store.repository));
        applyDeviceVisibility();
        initializeDisplayCapabilities();
    }

    private void applyDeviceVisibility() {
        SettingsVisibilityPolicy.Result visibility =
                SettingsVisibilityPolicy.evaluate(
                        AndroidSettingsDeviceCapabilities.collect(this),
                        store.get(
                                InputSettingKeys.BAROMETER_FORCE_PRESS));
        screenModel.applyVisibility(visibility);
    }

    private void initializeDisplayCapabilities() {
        SettingsDisplayController.Result result =
                new SettingsDisplayController(
                        store,
                        screenModel,
                        new AndroidSettingsDisplayText(this))
                        .apply(AndroidSettingsDisplayCapabilities.collect(
                                this,
                                displayCutoutP));
        nativeFrameRateValue = result.getNativeFrameRateValue();
    }

    private SettingsItem findItem(String key) {
        return screenModel.findItem(key);
    }

    private SettingsItem requireItem(String key) {
        SettingsItem item = findItem(key);
        if (item == null) {
            throw new IllegalStateException(
                    "Rendered settings item is missing: " + key);
        }
        return item;
    }

    private SettingsScreenState createScreenState() {
        return SettingsScreenStateFactory.create(
                sections,
                store,
                getText(R.string.settings_action_open));
    }

    private String getCurrentProfileSummary() {
        SettingsItem resolution = findItem(
                StreamResolutionSettingKeys.RESOLUTION.getName());
        SettingsItem fps = findItem(
                StreamResolutionSettingKeys.FPS.getName());
        SettingsItem bitrate = findItem(
                StreamVideoSettingKeys.BITRATE_KBPS.getName());

        String resolutionText = resolution == null
                ? ""
                : resolution.getSelectedEntry(store).toString();
        String fpsText = fps == null
                ? ""
                : fps.getSelectedEntry(store).toString();
        String bitrateText = bitrate == null
                ? ""
                : bitrate.formatSliderValue(
                        bitrate.round(store.getInt(bitrate)));
        return getString(
                R.string.settings_current_profile,
                resolutionText,
                fpsText,
                bitrateText);
    }

    @Override
    public void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (documentController != null) {
            documentController.handleActivityResult(
                    requestCode,
                    resultCode,
                    data);
        }
    }
}
