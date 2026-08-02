package com.limelight.preferences;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.WindowInsets;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.view.WindowCompat;

import com.limelight.AboutActivity;
import com.limelight.BaseActivity;
import com.limelight.PcView;
import com.limelight.R;
import com.limelight.binding.video.gl.GlDeviceSnapshotStore;
import com.limelight.binding.video.gl.android.SharedPreferencesGlDeviceSnapshotStore;
import com.limelight.settings.android.AndroidAppPresentationSettingsLoader;
import com.limelight.platform.AndroidDisplayCompat;
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

public class StreamSettings extends BaseActivity {
    private static final String EXTRA_SECTION_ID =
            "com.limelight.preferences.StreamSettings.SECTION_ID";
    private static final String STATE_SECTION_KEY =
            "settings.selected_section_key";
    private static final String STATE_ROOT_SCROLL_Y =
            "settings.root_scroll_y";
    private static final String STATE_SECTION_SCROLL_Y =
            "settings.section_scroll_y";
    private static final String STATE_SECTION_RAIL_SCROLL_Y =
            "settings.section_rail_scroll_y";
    private static final String STATE_DOCUMENT_REQUEST_CODE =
            "settings.document_request_code";

    private AppPresentationSettings previousPresentationSettings;
    private int previousDisplayPixelCount;
    private SettingsStore store;
    private GlDeviceSnapshotStore glDeviceSnapshotStore;
    private ArrayList<SettingsSection> sections = new ArrayList<>();
    private SettingsScreenModel screenModel =
            new SettingsScreenModel(sections);
    private int selectedSectionIndex = -1;
    private final SettingsNavigationState navigationState =
            new SettingsNavigationState();
    private String nativeFrameRateValue;
    private BackNavigationRegistration backNavigationRegistration;
    private SettingsDocumentController documentController;
    private SettingsMutationController mutationController;
    private SettingsChangeEffectScheduler changeEffectScheduler;
    private SettingsDialogPresenter dialogPresenter;
    private SettingsScreenRenderer screenRenderer;
    private boolean sectionActivity;
    private boolean sectionLaunchPending;
    private boolean reloadAfterPause;
    private final ActivityResultLauncher<Intent> documentLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> handleDocumentActivityResult(
                            result.getResultCode(),
                            result.getData()));

    // Android 9 exposes the cutout only after the window is attached.
    static DisplayCutout displayCutoutP;

    void handleDocumentActivityResult(int resultCode, Intent data) {
        if (documentController != null) {
            documentController.handleActivityResult(resultCode, data);
        }
    }

    void reloadSettings() {
        captureNavigationScroll();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Display.Mode mode = AndroidDisplayCompat
                    .getActivityDisplay(this)
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
        if (sectionActivity &&
                !navigationState.hasSelectedSection()) {
            finish();
            return;
        }
        selectedSectionIndex =
                screenModel.findSectionIndex(
                        navigationState.getSelectedSectionId());
        if (navigationState.hasSelectedSection() &&
                selectedSectionIndex < 0) {
            if (sectionActivity) {
                finish();
                return;
            }
            navigationState.selectFeatured();
        }
        if (screenRenderer != null) {
            renderSettings();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        previousPresentationSettings =
                AndroidAppPresentationSettingsLoader.load(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                !previousPresentationSettings.usesLightTheme()) {
            // Preserve the opaque settings launch background in dark mode.
            // Both root and detail instances still inherit the same platform
            // Activity motion from their presentation theme.
            setTheme(R.style.SettingsActivityDarkTheme);
        }
        super.onCreate(savedInstanceState);
        store = new SettingsStore(this);
        glDeviceSnapshotStore =
                new SharedPreferencesGlDeviceSnapshotStore(this);
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
        int pendingDocumentRequest = savedInstanceState == null
                ? SettingsDocumentController.NO_PENDING_REQUEST
                : savedInstanceState.getInt(
                        STATE_DOCUMENT_REQUEST_CODE,
                        SettingsDocumentController.NO_PENDING_REQUEST);
        documentController = new SettingsDocumentController(
                this,
                store.repository,
                layoutRepository,
                documentLauncher::launch,
                pendingDocumentRequest,
                this::reloadSettings);
        sectionActivity = getIntent().hasExtra(EXTRA_SECTION_ID);
        String requestedSectionId = sectionActivity
                ? getIntent().getStringExtra(EXTRA_SECTION_ID)
                : null;
        if (savedInstanceState != null) {
            String sectionId = savedInstanceState.getString(
                    STATE_SECTION_KEY);
            if (sectionId != null) {
                navigationState.selectSection(sectionId);
            }
            navigationState.setContentScroll(
                    null,
                    savedInstanceState.getInt(
                            STATE_ROOT_SCROLL_Y,
                            0));
            if (sectionId != null) {
                navigationState.setContentScroll(
                        sectionId,
                        savedInstanceState.getInt(
                                STATE_SECTION_SCROLL_Y,
                                0));
            }
            navigationState.setSectionRailScrollY(
                    savedInstanceState.getInt(
                            STATE_SECTION_RAIL_SCROLL_Y,
                            0));
        }
        if (sectionActivity &&
                !navigationState.hasSelectedSection() &&
                requestedSectionId != null &&
                !requestedSectionId.isEmpty()) {
            navigationState.selectSection(requestedSectionId);
        }
        screenRenderer = createScreenRenderer();

        setContentView(screenRenderer.createRootView());
        configureImmersiveSettingsWindow();
        if (previousPresentationSettings.usesLightTheme()) {
            UiHelper.setStatusBarLightMode(getWindow(), true);
        }
        registerBackCallback();
        // Build the first frame before Android starts the Activity window
        // transition. Waiting for attachment leaves the incoming window empty
        // for part of the animation and makes compact navigation flash.
        reloadSettings();
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
        if (screenRenderer != null) {
            screenRenderer.applyWindowPadding();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sectionLaunchPending = false;
        if (reloadAfterPause &&
                screenRenderer != null &&
                screenRenderer.hasContent()) {
            reloadSettings();
        }
        reloadAfterPause = false;
    }

    @Override
    protected void onPause() {
        reloadAfterPause = true;
        super.onPause();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Display.Mode mode = AndroidDisplayCompat
                    .getActivityDisplay(this)
                    .getMode();
            int displayPixelCount =
                    mode.getPhysicalWidth() * mode.getPhysicalHeight();
            if (displayPixelCount != previousDisplayPixelCount) {
                reloadSettings();
                return;
            }
        }
        if (screenRenderer != null) {
            captureNavigationScroll();
            renderSettings();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        captureNavigationScroll();
        String sectionId = navigationState.getSelectedSectionId();
        outState.putString(STATE_SECTION_KEY, sectionId);
        outState.putInt(
                STATE_ROOT_SCROLL_Y,
                navigationState.getContentScroll(null));
        if (sectionId != null) {
            outState.putInt(
                    STATE_SECTION_SCROLL_Y,
                    navigationState.getContentScroll(sectionId));
        }
        outState.putInt(
                STATE_SECTION_RAIL_SCROLL_Y,
                navigationState.getSectionRailScrollY());
        if (documentController != null) {
            outState.putInt(
                    STATE_DOCUMENT_REQUEST_CODE,
                    documentController.getPendingRequestCode());
        }
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
        finishAndApplyLanguage();
    }

    private void openSection(int sectionIndex) {
        captureNavigationScroll();
        if (screenRenderer == null) {
            return;
        }

        if (!screenRenderer.isWideLayout()) {
            openCompactSection(sectionIndex);
            return;
        }

        if (sectionIndex ==
                SettingsScreenRenderer.FEATURED_SECTION_INDEX) {
            navigationState.selectFeatured();
            selectedSectionIndex =
                    SettingsScreenRenderer.FEATURED_SECTION_INDEX;
            renderWideSelection();
            return;
        }
        String sectionKey = screenModel.getSectionKey(sectionIndex);
        if (sectionKey == null) {
            return;
        }
        navigationState.selectSection(sectionKey);
        selectedSectionIndex = sectionIndex;
        renderWideSelection();
    }

    private void openCompactSection(int sectionIndex) {
        if (sectionLaunchPending ||
                sectionIndex ==
                        SettingsScreenRenderer.FEATURED_SECTION_INDEX) {
            return;
        }
        String sectionId = screenModel.getSectionKey(sectionIndex);
        if (sectionId == null) {
            return;
        }

        sectionLaunchPending = true;
        Intent intent = new Intent(this, StreamSettings.class);
        intent.putExtra(EXTRA_SECTION_ID, sectionId);
        try {
            startActivity(intent);
        }
        catch (RuntimeException exception) {
            sectionLaunchPending = false;
            throw exception;
        }
    }

    private void renderWideSelection() {
        screenRenderer.setContent(
                createScreenState(),
                selectedSectionIndex,
                getCurrentProfileSummary());
        if (!screenRenderer.renderWideSelection()) {
            renderSettings();
            return;
        }
        screenRenderer.restoreScrollY(
                navigationState.getContentScrollY());
        // The wide shell and its section rail are retained in place. Reposting
        // the same rail position after every detail replacement can race the
        // touch-driven scroll state and produce a visible jump near the end of
        // the list.
    }

    private void captureNavigationScroll() {
        if (screenRenderer == null || !screenRenderer.hasContent()) {
            return;
        }
        navigationState.captureContentScroll(
                screenRenderer.captureScrollY());
        navigationState.captureSectionRailScroll(
                screenRenderer.captureSectionListScrollY());
    }

    private void renderSettings() {
        normalizeCompactRootNavigation();
        screenRenderer.setContent(
                createScreenState(),
                selectedSectionIndex,
                getCurrentProfileSummary());
        screenRenderer.render();
        screenRenderer.restoreScrollY(
                navigationState.getContentScrollY());
        screenRenderer.restoreSectionListScrollY(
                navigationState.getSectionRailScrollY());
    }

    private void normalizeCompactRootNavigation() {
        if (sectionActivity ||
                screenRenderer == null ||
                screenRenderer.willUseWideLayout() ||
                !navigationState.hasSelectedSection()) {
            return;
        }
        navigationState.returnToRoot();
        selectedSectionIndex =
                SettingsScreenRenderer.FEATURED_SECTION_INDEX;
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
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
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
                                displayCutoutP,
                                glDeviceSnapshotStore.read()));
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

}
