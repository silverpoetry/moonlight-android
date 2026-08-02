package com.limelight.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.limelight.R;
import com.limelight.utils.UiHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Lifecycle-bound Android renderer for the task-oriented settings screen.
 * It reads presentation state and emits semantic user intents, but never
 * mutates settings storage or applies settings policy.
 */
final class SettingsScreenRenderer {
    interface Listener {
        void onBackRequested();

        void onSectionRequested(int sectionIndex);

        void onItemRequested(String itemId);

        void onSwitchChanged(String itemId, boolean checked);
    }

    static final int FEATURED_SECTION_INDEX = -1;
    private static final int WIDE_LAYOUT_MIN_WIDTH_DP = 720;
    private final Context context;
    private Listener listener;
    private final ArrayList<RenderedSettingsRow> renderedRows =
            new ArrayList<>();
    private final ArrayList<View> wideSectionRows =
            new ArrayList<>();

    private SettingsScreenState state;
    private FrameLayout root;
    private LinearLayout outerContainer;
    private FrameLayout mainContainer;
    private FrameLayout wideItemContainer;
    private TextView titleView;
    private TextView subtitleView;
    private ScrollView activeContentScrollView;
    private ScrollView sectionListScrollView;
    private CharSequence profileSummary = "";
    private int selectedSectionIndex = FEATURED_SECTION_INDEX;
    private boolean wideLayout;
    private boolean updatingControls;
    private boolean destroyed;

    SettingsScreenRenderer(
            Context context,
            Listener listener) {
        this.context = Objects.requireNonNull(context, "context");
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    View createRootView() {
        if (root != null) {
            return root;
        }

        root = new FrameLayout(context);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackgroundResource(R.drawable.bg_gradient_main);

        mainContainer = new FrameLayout(context);
        mainContainer.setId(R.id.settings_content_container);
        root.addView(mainContainer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return root;
    }

    void setContent(
            SettingsScreenState state,
            int selectedSectionIndex,
            CharSequence profileSummary) {
        if (destroyed) {
            return;
        }
        this.state = Objects.requireNonNull(state, "state");
        this.selectedSectionIndex = selectedSectionIndex;
        this.profileSummary = profileSummary == null
                ? ""
                : profileSummary;
    }

    void render() {
        if (destroyed || mainContainer == null || state == null) {
            return;
        }

        wideLayout = willUseWideLayout();
        activeContentScrollView = null;
        sectionListScrollView = null;
        wideItemContainer = null;
        wideSectionRows.clear();
        FrameLayout screenPage = createScreenPage();
        LinearLayout page = createPageContainer();
        FrameLayout contentContainer = new FrameLayout(context);
        LinearLayout.LayoutParams contentParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1);
        contentParams.topMargin = dp(14);
        outerContainer.addView(contentContainer, contentParams);
        contentContainer.addView(page, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        if (wideLayout) {
            renderWide(page);
        }
        else if (selectedSectionIndex >= 0) {
            renderSectionDetail(page, selectedSectionIndex);
        }
        else {
            renderSectionList(page);
        }

        mainContainer.removeAllViews();
        mainContainer.addView(screenPage, matchParentLayoutParams());
        outerContainer.requestApplyInsets();
    }

    boolean renderWideSelection() {
        if (destroyed ||
                !wideLayout ||
                wideItemContainer == null ||
                state == null) {
            return false;
        }
        updateWideSectionSelection();
        renderWideItemContent();
        return true;
    }

    boolean hasContent() {
        return !destroyed && state != null;
    }

    boolean isWideLayout() {
        return wideLayout;
    }

    boolean willUseWideLayout() {
        return getAvailableWidthDp() >= WIDE_LAYOUT_MIN_WIDTH_DP;
    }

    int getSelectedSectionIndex() {
        return selectedSectionIndex;
    }

    Integer captureScrollY() {
        return activeContentScrollView == null
                ? null
                : activeContentScrollView.getScrollY();
    }

    Integer captureSectionListScrollY() {
        return sectionListScrollView == null
                ? null
                : sectionListScrollView.getScrollY();
    }

    void restoreScrollY(Integer scrollY) {
        if (scrollY == null || activeContentScrollView == null) {
            return;
        }
        restoreScrollBeforeFirstDraw(activeContentScrollView, scrollY);
    }

    void restoreSectionListScrollY(Integer scrollY) {
        if (scrollY == null || sectionListScrollView == null) {
            return;
        }
        restoreScrollBeforeFirstDraw(sectionListScrollView, scrollY);
    }

    private void restoreScrollBeforeFirstDraw(
            ScrollView scrollView,
            int scrollY) {
        int targetScrollY = Math.max(0, scrollY);
        if (targetScrollY == 0) {
            scrollView.scrollTo(0, 0);
            return;
        }

        ViewTreeObserver observer = scrollView.getViewTreeObserver();
        observer.addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        ViewTreeObserver currentObserver =
                                scrollView.getViewTreeObserver();
                        if (currentObserver.isAlive()) {
                            currentObserver.removeOnPreDrawListener(this);
                        }
                        scrollView.scrollTo(0, targetScrollY);
                        return true;
                    }
                });
    }

    void updateState(
            SettingsScreenState updatedState,
            CharSequence updatedProfileSummary) {
        if (destroyed || state == null) {
            return;
        }
        state = Objects.requireNonNull(updatedState, "updatedState");
        profileSummary = updatedProfileSummary == null
                ? ""
                : updatedProfileSummary;
        if (subtitleView != null &&
                selectedSectionIndex == FEATURED_SECTION_INDEX) {
            subtitleView.setText(profileSummary);
        }
        updatingControls = true;
        try {
            for (RenderedSettingsRow row : renderedRows) {
                SettingsScreenState.Row updated =
                        state.findRow(row.itemId);
                if (updated != null) {
                    row.refresh(updated);
                }
            }
        }
        finally {
            updatingControls = false;
        }
    }

    void applyWindowPadding() {
        if (outerContainer == null || destroyed) {
            return;
        }

        final int horizontalPadding = dp(18);
        final int topPadding = dp(12);
        final int bottomPadding = dp(12);
        outerContainer.setPadding(
                horizontalPadding,
                topPadding,
                horizontalPadding,
                bottomPadding);
        ViewCompat.setOnApplyWindowInsetsListener(
                outerContainer,
                (view, insets) -> {
                    Insets safeInsets = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars() |
                                    WindowInsetsCompat.Type.displayCutout());
                    view.setPadding(
                            horizontalPadding + safeInsets.left,
                            topPadding + safeInsets.top,
                            horizontalPadding + safeInsets.right,
                            bottomPadding + safeInsets.bottom);
                    return insets;
                });
        ViewCompat.requestApplyInsets(outerContainer);
    }

    void destroy() {
        destroyed = true;
        listener = null;
        renderedRows.clear();
        wideSectionRows.clear();
        activeContentScrollView = null;
        sectionListScrollView = null;
        wideItemContainer = null;
        state = null;
    }

    private LinearLayout createPageContainer() {
        LinearLayout page = new LinearLayout(context);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setAlpha(1f);
        page.setTranslationX(0f);
        page.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return page;
    }

    private FrameLayout createScreenPage() {
        FrameLayout screenPage = new FrameLayout(context);
        screenPage.setBackgroundResource(
                R.drawable.bg_gradient_main);
        screenPage.setAlpha(1f);

        outerContainer = new LinearLayout(context);
        outerContainer.setOrientation(LinearLayout.VERTICAL);
        screenPage.addView(outerContainer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        applyWindowPadding();
        addHeader(outerContainer);
        return screenPage;
    }

    private void addHeader(LinearLayout page) {
        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        page.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        ImageButton backButton = new ImageButton(context);
        backButton.setImageResource(R.drawable.ic_back);
        backButton.setBackgroundResource(
                R.drawable.ic_game_menu_btn_transparent);
        backButton.setPadding(dp(9), dp(9), dp(9), dp(9));
        header.addView(backButton, new LinearLayout.LayoutParams(
                dp(44),
                dp(44)));
        backButton.setOnClickListener(view -> {
            if (listener != null) {
                listener.onBackRequested();
            }
        });

        LinearLayout titleBlock = new LinearLayout(context);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1);
        titleParams.leftMargin = dp(10);
        header.addView(titleBlock, titleParams);

        titleView = new TextView(context);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(24);
        titleView.setTypeface(null, Typeface.BOLD);
        titleBlock.addView(titleView);

        subtitleView = new TextView(context);
        subtitleView.setTextColor(0xCCFFFFFF);
        subtitleView.setTextSize(12);
        subtitleView.setSingleLine(true);
        subtitleView.setEllipsize(TextUtils.TruncateAt.END);
        titleBlock.addView(subtitleView);
    }

    private void renderWide(LinearLayout page) {
        titleView.setText(R.string.settings_title);
        subtitleView.setText(profileSummary);

        LinearLayout columns = new LinearLayout(context);
        columns.setOrientation(LinearLayout.HORIZONTAL);
        page.addView(columns, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        ScrollView sectionScroll = createScrollView();
        sectionListScrollView = sectionScroll;
        LinearLayout sectionList = createVerticalList();
        sectionScroll.addView(sectionList);
        columns.addView(
                sectionScroll,
                new LinearLayout.LayoutParams(
                        dp(300),
                        ViewGroup.LayoutParams.MATCH_PARENT));

        View featuredRow = createFeaturedSectionRow(
                selectedSectionIndex == FEATURED_SECTION_INDEX);
        wideSectionRows.add(featuredRow);
        sectionList.addView(featuredRow);
        List<SettingsScreenState.Section> sections =
                state.getSections();
        for (int index = 0; index < sections.size(); index++) {
            View sectionRow = createSectionRow(
                    sections.get(index),
                    index,
                    index == selectedSectionIndex);
            wideSectionRows.add(sectionRow);
            sectionList.addView(sectionRow);
        }

        wideItemContainer = new FrameLayout(context);
        wideItemContainer.setId(R.id.settings_detail_container);
        LinearLayout.LayoutParams itemParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1);
        itemParams.leftMargin = dp(14);
        columns.addView(wideItemContainer, itemParams);
        renderWideItemContent();
    }

    private void updateWideSectionSelection() {
        for (View row : wideSectionRows) {
            Object tag = row.getTag();
            boolean selected = tag instanceof Integer &&
                    (Integer) tag == selectedSectionIndex;
            row.setSelected(selected);
        }
    }

    private void renderWideItemContent() {
        if (wideItemContainer == null) {
            return;
        }

        LinearLayout page = createPageContainer();
        if (selectedSectionIndex == FEATURED_SECTION_INDEX) {
            renderFeaturedSettings(page, false);
        }
        else {
            renderSectionDetail(page, selectedSectionIndex, false);
        }

        // View mutations made in this call are committed in one traversal.
        // Remove the previous detail before attaching the fully constructed
        // replacement so transparent row backgrounds can never composite
        // two settings sections during a hardware-rendered transition frame.
        wideItemContainer.removeAllViews();
        wideItemContainer.addView(page, matchParentLayoutParams());
    }

    private void renderSectionList(LinearLayout page) {
        renderedRows.clear();
        titleView.setText(R.string.settings_title);
        subtitleView.setText(profileSummary);
        ScrollView scroll = createScrollView();
        activeContentScrollView = scroll;
        LinearLayout list = createVerticalList();
        scroll.addView(list);
        page.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        addRootFeaturedItems(list);
        addListHeader(list, R.string.settings_more_settings);
        List<SettingsScreenState.Section> sections =
                state.getSections();
        for (int index = 0; index < sections.size(); index++) {
            list.addView(createSectionRow(
                    sections.get(index),
                    index,
                    false));
        }
    }

    private void renderFeaturedSettings(LinearLayout page) {
        renderFeaturedSettings(page, true);
    }

    private void renderFeaturedSettings(
            LinearLayout page,
            boolean updateHeader) {
        renderedRows.clear();
        if (updateHeader) {
            titleView.setText(R.string.settings_featured_settings);
            subtitleView.setText(profileSummary);
        }
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
        List<SettingsScreenState.Row> featuredRows =
                state.getFeaturedRows();
        if (featuredRows.isEmpty()) {
            return;
        }
        addListHeader(list, R.string.settings_featured_settings);
        for (SettingsScreenState.Row row : featuredRows) {
            list.addView(createItemRow(row));
        }
    }

    private void addListHeader(LinearLayout list, int titleResId) {
        TextView header = new TextView(context);
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

    private void renderSectionDetail(
            LinearLayout page,
            int sectionIndex) {
        renderSectionDetail(page, sectionIndex, true);
    }

    private void renderSectionDetail(
            LinearLayout page,
            int sectionIndex,
            boolean updateHeader) {
        renderedRows.clear();
        SettingsScreenState.Section section =
                state.getSections().get(sectionIndex);
        List<SettingsScreenState.Row> rows = section.getRows();
        if (updateHeader) {
            titleView.setText(section.getTitle());
            subtitleView.setText(context.getResources().getQuantityString(
                    R.plurals.settings_item_count,
                    rows.size(),
                    rows.size()));
        }

        ScrollView scroll = createScrollView();
        activeContentScrollView = scroll;
        LinearLayout list = createVerticalList();
        scroll.addView(list);
        page.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        addSectionItems(list, rows);
    }

    private ScrollView createScrollView() {
        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setHorizontalScrollBarEnabled(false);
        return scrollView;
    }

    private LinearLayout createVerticalList() {
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, 0, 0, dp(12));
        return list;
    }

    private View createSectionRow(
            SettingsScreenState.Section section,
            int index,
            boolean selected) {
        LinearLayout row = new LinearLayout(context);
        row.setTag(index);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        // Selection is state, not a replacement background. Replacing the
        // drawable after attachment also replaces its padding and can resize
        // the section rail for one frame, which is visible near the end of a
        // scrolled wide-layout list.
        row.setBackgroundResource(
                R.drawable.bg_settings_section_selector);
        row.setSelected(selected);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));

        ImageView icon = new ImageView(context);
        icon.setImageResource(section.getIconRes());
        icon.setAlpha(0.88f);
        row.addView(icon, new LinearLayout.LayoutParams(
                dp(24),
                dp(24)));

        LinearLayout textBlock = new LinearLayout(context);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1);
        textParams.leftMargin = dp(12);
        row.addView(textBlock, textParams);

        TextView title = new TextView(context);
        title.setText(section.getTitle());
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        textBlock.addView(title);

        int itemCount = section.getRows().size();
        TextView summary = new TextView(context);
        summary.setText(context.getResources().getQuantityString(
                R.plurals.settings_item_count,
                itemCount,
                itemCount));
        summary.setTextColor(0xBFFFFFFF);
        summary.setTextSize(12);
        textBlock.addView(summary);

        TextView arrow = new TextView(context);
        arrow.setText(">");
        arrow.setTextColor(0xCCFFFFFF);
        arrow.setTextSize(20);
        row.addView(arrow);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(8);
        row.setLayoutParams(params);
        row.setOnClickListener(view -> selectSection(index));
        return row;
    }

    private View createFeaturedSectionRow(boolean selected) {
        SettingsScreenState.Section section =
                new SettingsScreenState.Section(
                "featured_settings",
                context.getText(R.string.settings_featured_settings),
                R.drawable.ic_quick_actions,
                state.getFeaturedRows());
        return createSectionRow(
                section,
                FEATURED_SECTION_INDEX,
                selected);
    }

    private void selectSection(int sectionIndex) {
        if (listener != null) {
            listener.onSectionRequested(sectionIndex);
        }
    }

    private void addSectionItems(
            LinearLayout list,
            List<SettingsScreenState.Row> rows) {
        if (rows.isEmpty()) {
            TextView empty = new TextView(context);
            empty.setText(R.string.settings_no_items);
            empty.setTextColor(0xCCFFFFFF);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(18), dp(28), dp(18), dp(28));
            list.addView(empty);
            return;
        }

        for (SettingsScreenState.Row row : rows) {
            list.addView(createItemRow(row));
        }
    }

    private View createItemRow(SettingsScreenState.Row item) {
        boolean enabled = item.isEnabled();
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(58));
        row.setPadding(dp(14), dp(9), dp(14), dp(9));
        row.setBackgroundResource(
                R.drawable.ic_game_menu_btn_selector);

        ImageView icon = new ImageView(context);
        icon.setImageResource(item.getIconRes());
        icon.setAlpha(enabled ? 0.86f : 0.35f);
        row.addView(icon, new LinearLayout.LayoutParams(
                dp(22),
                dp(22)));

        LinearLayout textBlock = new LinearLayout(context);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1);
        textParams.leftMargin = dp(12);
        row.addView(textBlock, textParams);

        TextView title = new TextView(context);
        title.setText(item.getTitle());
        title.setTextColor(enabled ? Color.WHITE : 0x80FFFFFF);
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        textBlock.addView(title);

        if (!TextUtils.isEmpty(item.getSummary())) {
            TextView summary = new TextView(context);
            summary.setText(item.getSummary());
            summary.setTextColor(enabled
                    ? 0xBFFFFFFF
                    : 0x66FFFFFF);
            summary.setTextSize(12);
            summary.setMaxLines(2);
            summary.setEllipsize(TextUtils.TruncateAt.END);
            textBlock.addView(summary);
        }

        View control = createControlView(item);
        if (control != null) {
            LinearLayout.LayoutParams controlParams =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            controlParams.leftMargin = dp(10);
            row.addView(control, controlParams);
        }

        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = dp(7);
        row.setLayoutParams(rowParams);
        row.setEnabled(enabled);
        row.setAlpha(enabled ? 1f : 0.55f);
        row.setOnClickListener(view -> {
            if (!item.isEnabled()) {
                return;
            }
            if (item.hasSwitchControl() &&
                    control instanceof CompoundButton) {
                control.performClick();
            }
            else if (listener != null) {
                listener.onItemRequested(item.getId());
            }
        });
        renderedRows.add(new RenderedSettingsRow(
                item.getId(),
                row,
                icon,
                title,
                control));
        return row;
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private View createControlView(SettingsScreenState.Row item) {
        if (item.hasSwitchControl()) {
            Switch switchView = new Switch(context);
            switchView.setShowText(false);
            switchView.setMinimumWidth(dp(52));
            switchView.setChecked(item.isChecked());
            switchView.setEnabled(item.isEnabled());
            tintSwitch(switchView);
            switchView.setOnCheckedChangeListener(
                    (button, checked) -> {
                        if (!updatingControls && listener != null) {
                            listener.onSwitchChanged(
                                    item.getId(),
                                    checked);
                        }
                    });
            return switchView;
        }

        TextView value = new TextView(context);
        value.setTextColor(0xE6FFFFFF);
        value.setTextSize(13);
        value.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        value.setMaxWidth(dp(180));
        value.setSingleLine(true);
        value.setEllipsize(TextUtils.TruncateAt.END);
        value.setText(item.getValueText());
        return value;
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private void tintSwitch(Switch switchView) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        int[][] states = new int[][] {
                new int[] {android.R.attr.state_checked},
                new int[] {-android.R.attr.state_checked},
        };
        switchView.setThumbTintList(new ColorStateList(
                states,
                new int[] {0xFFFFFFFF, 0xFFE8E3F2}));
        switchView.setTrackTintList(new ColorStateList(
                states,
                new int[] {0xFF24C46B, 0xFF37324D}));
    }

    private int getAvailableWidthDp() {
        DisplayMetrics metrics = context.getResources()
                .getDisplayMetrics();
        return (int) (metrics.widthPixels / metrics.density);
    }

    private int dp(float value) {
        return UiHelper.dpToPx(context, value);
    }

    private FrameLayout.LayoutParams matchParentLayoutParams() {
        return new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private final class RenderedSettingsRow {
        private final String itemId;
        private final View row;
        private final ImageView icon;
        private final TextView title;
        private final View control;

        RenderedSettingsRow(
                String itemId,
                View row,
                ImageView icon,
                TextView title,
                View control) {
            this.itemId = itemId;
            this.row = row;
            this.icon = icon;
            this.title = title;
            this.control = control;
        }

        void refresh(SettingsScreenState.Row item) {
            boolean enabled = item.isEnabled();
            row.setEnabled(enabled);
            row.setAlpha(enabled ? 1f : 0.55f);
            icon.setAlpha(enabled ? 0.86f : 0.35f);
            title.setTextColor(enabled
                    ? Color.WHITE
                    : 0x80FFFFFF);

            if (control == null) {
                return;
            }
            control.setEnabled(enabled);
            if (control instanceof Switch) {
                ((Switch) control).setChecked(item.isChecked());
            }
            else if (control instanceof TextView &&
                    !(control instanceof CompoundButton)) {
                ((TextView) control).setText(
                        item.getValueText());
            }
        }
    }
}
