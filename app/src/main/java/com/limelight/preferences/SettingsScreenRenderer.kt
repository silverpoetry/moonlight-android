package com.limelight.preferences

import android.content.Context
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.limelight.R
import com.limelight.ui.compose.components.MoonlightScreen
import com.limelight.ui.compose.components.MoonlightStepSlider
import com.limelight.ui.compose.components.MoonlightSwitch
import com.limelight.ui.compose.components.performToggleHapticFeedback
import com.limelight.ui.compose.theme.MoonlightThemeFromSettings
import kotlin.math.roundToInt

/**
 * Lifecycle-bound Compose renderer for the settings surface.
 *
 * Settings policy, persistence, dependency evaluation, and mutations remain
 * owned by the existing settings domain. This class only renders immutable
 * [SettingsScreenState] snapshots and reports semantic user intents.
 */
class SettingsScreenRenderer(
    private val context: Context,
    private var listener: Listener?,
) {
    interface Listener {
        fun onBackRequested()
        fun onSectionRequested(sectionIndex: Int)
        fun onItemRequested(itemId: String)
        fun onSwitchChanged(itemId: String, checked: Boolean)
        fun onInlineChoiceChanged(itemId: String, value: String)
        fun onInlineSliderChanged(itemId: String, value: Int)
    }

    private data class RenderState(
        val screen: SettingsScreenState,
        val selectedSectionIndex: Int,
        val profileSummary: String,
    )

    private data class SearchResult(
        val sectionTitle: String,
        val row: SettingsScreenState.Row,
    )

    private var renderState: RenderState? by mutableStateOf(null)
    private var searchQuery by mutableStateOf("")
    private var contentRestoreVersion by mutableIntStateOf(0)
    private var sectionRailRestoreVersion by mutableIntStateOf(0)
    private var pendingContentScroll = 0
    private var pendingSectionRailScroll = 0
    private var activeContentScroll: androidx.compose.foundation.ScrollState? = null
    private var sectionRailScroll: androidx.compose.foundation.ScrollState? = null
    private var composeView: ComposeView? = null
    private var destroyed = false

    fun createRootView(): View {
        composeView?.let { return it }
        return ComposeView(context).also { view ->
            composeView = view
            view.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
            )
            view.setContent {
                MoonlightThemeFromSettings {
                    renderState?.let { state ->
                        SettingsSurface(state)
                    }
                }
            }
        }
    }

    fun setContent(
        state: SettingsScreenState,
        selectedSectionIndex: Int,
        profileSummary: CharSequence?,
    ) {
        if (destroyed) return
        renderState = RenderState(
            screen = state,
            selectedSectionIndex = selectedSectionIndex,
            profileSummary = profileSummary?.toString().orEmpty(),
        )
    }

    fun renderWideSelection(): Boolean {
        if (destroyed || !willUseWideLayout() || renderState == null) {
            return false
        }
        return true
    }

    fun hasContent(): Boolean = !destroyed && renderState != null

    fun isWideLayout(): Boolean = willUseWideLayout()

    fun willUseWideLayout(): Boolean =
        context.resources.configuration.screenWidthDp >= WIDE_LAYOUT_MIN_WIDTH_DP

    fun getSelectedSectionIndex(): Int =
        renderState?.selectedSectionIndex ?: FEATURED_SECTION_INDEX

    fun captureScrollY(): Int? = activeContentScroll?.value

    fun captureSectionListScrollY(): Int? = sectionRailScroll?.value

    fun restoreScrollY(scrollY: Int?) {
        pendingContentScroll = scrollY?.coerceAtLeast(0) ?: return
        contentRestoreVersion++
    }

    /** Selects the next page's scroll before Compose observes its page key. */
    fun prepareContentScroll(scrollY: Int) {
        pendingContentScroll = scrollY.coerceAtLeast(0)
    }

    fun restoreSectionListScrollY(scrollY: Int?) {
        pendingSectionRailScroll = scrollY?.coerceAtLeast(0) ?: return
        sectionRailRestoreVersion++
    }

    fun prepareSectionListScroll(scrollY: Int) {
        pendingSectionRailScroll = scrollY.coerceAtLeast(0)
    }

    fun updateState(
        updatedState: SettingsScreenState,
        updatedProfileSummary: CharSequence?,
    ) {
        val current = renderState ?: return
        if (destroyed) return
        renderState = current.copy(
            screen = updatedState,
            profileSummary = updatedProfileSummary?.toString().orEmpty(),
        )
    }

    /** Insets are owned by Material 3 Scaffold and BaseActivity edge-to-edge. */
    fun applyWindowPadding() = Unit

    fun destroy() {
        destroyed = true
        listener = null
        renderState = null
        activeContentScroll = null
        sectionRailScroll = null
        composeView?.disposeComposition()
        composeView = null
    }

    @Composable
    private fun SettingsSurface(state: RenderState) {
        if (willUseWideLayout()) {
            WideSettingsSurface(state)
        } else {
            CompactSettingsSurface(state)
        }
    }

    @Composable
    private fun CompactSettingsSurface(state: RenderState) {
        val section = state.screen.sections.getOrNull(state.selectedSectionIndex)
        val title = section?.title?.toString()
            ?: stringResource(R.string.settings_title)
        MoonlightScreen(
            title = title,
            onBack = { listener?.onBackRequested() },
            collapsible = true,
        ) { padding ->
            val pageKey = section?.id ?: ROOT_PAGE_KEY
            val scrollState = remember(pageKey) {
                androidx.compose.foundation.ScrollState(pendingContentScroll)
            }
            BindContentScroll(pageKey, scrollState)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(
                        start = 20.dp,
                        top = 12.dp,
                        end = 20.dp,
                        bottom = 32.dp,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                if (section == null) {
                    SettingsRootContent(state)
                } else {
                    SettingsSectionContent(section)
                }
            }
        }
    }

    @Composable
    private fun WideSettingsSurface(state: RenderState) {
        MoonlightScreen(
            title = stringResource(R.string.settings_title),
            onBack = { listener?.onBackRequested() },
        ) { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                val railState = rememberScrollState(pendingSectionRailScroll)
                SideEffect { sectionRailScroll = railState }
                LaunchedEffect(sectionRailRestoreVersion) {
                    railState.scrollTo(pendingSectionRailScroll)
                }
                SettingsSectionRail(
                    state = state,
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                        .verticalScroll(railState),
                )

                val section = state.screen.sections
                    .getOrNull(state.selectedSectionIndex)
                val pageKey = section?.id ?: ROOT_PAGE_KEY
                val contentState = remember(pageKey) {
                    androidx.compose.foundation.ScrollState(pendingContentScroll)
                }
                BindContentScroll(pageKey, contentState)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(contentState)
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    if (section == null) {
                        SettingsWideRootContent(state)
                    } else {
                        SettingsSectionContent(section)
                    }
                }
            }
        }
    }

    @Composable
    private fun BindContentScroll(
        pageKey: String,
        scrollState: androidx.compose.foundation.ScrollState,
    ) {
        SideEffect { activeContentScroll = scrollState }
        LaunchedEffect(pageKey, contentRestoreVersion) {
            scrollState.scrollTo(pendingContentScroll)
        }
    }

    @Composable
    private fun SettingsRootContent(state: RenderState) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .testTag(ROOT_TEST_TAG),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ProfileSummary(state)
            SettingsSearchField()
            if (searchQuery.isNotBlank()) {
                SettingsSearchResults(state)
                return@Column
            }
            FeaturedSettingsContent(
                state = state,
                showProfileSummary = false,
            )
            SettingsSectionCards(state)
        }
    }

    @Composable
    private fun SettingsWideRootContent(state: RenderState) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ProfileSummary(state)
            SettingsSearchField()
            if (searchQuery.isNotBlank()) {
                SettingsSearchResults(state)
            } else {
                FeaturedSettingsContent(state)
            }
        }
    }

    @Composable
    private fun ProfileSummary(state: RenderState) {
        if (state.profileSummary.isNotEmpty()) {
            Text(
                text = state.profileSummary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }

    @Composable
    private fun SettingsSearchField() {
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.settings_search_hint)) },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_m3_search),
                    contentDescription = null,
                )
            },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_m3_close),
                            contentDescription = stringResource(R.string.settings_search_clear),
                        )
                    }
                }
            } else {
                null
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
        )
    }

    @Composable
    private fun SettingsSearchResults(state: RenderState) {
        val query = searchQuery.trim()
        val results = remember(state.screen, query) {
            state.screen.sections.flatMap { section ->
                section.rows.map { row -> SearchResult(section.title.toString(), row) }
            }.distinctBy { result -> result.row.id }
                .filter { result ->
                    result.sectionTitle.contains(query, ignoreCase = true) ||
                        result.row.title.toString().contains(query, ignoreCase = true) ||
                        result.row.summary?.toString()?.contains(query, ignoreCase = true) == true ||
                        result.row.valueText?.toString()?.contains(query, ignoreCase = true) == true
                }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.settings_search_results),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            if (results.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.settings_search_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ) {
                    results.forEachIndexed { index, result ->
                        SettingsRow(
                            row = result.row,
                            compact = false,
                            showIcon = true,
                            contextLabel = result.sectionTitle,
                        )
                        if (index != results.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 64.dp, end = 20.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun FeaturedSettingsContent(
        state: RenderState,
        showProfileSummary: Boolean = false,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .testTag(FEATURED_TEST_TAG),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (showProfileSummary && state.profileSummary.isNotEmpty()) {
                Text(
                    text = state.profileSummary,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
            SettingsGroups(
                groups = state.screen.featuredGroups,
                compact = true,
            )
        }
    }

    @Composable
    private fun SettingsSectionCards(state: RenderState) {
        val indexedSections = state.screen.sections.withIndex().toList()
        val clusters = indexedSections.groupBy { indexed ->
            SettingsPresentationCatalog.forSection(indexed.value.id).id
        }
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            clusters.values.forEach { entries ->
                val group = SettingsPresentationCatalog.forSection(
                    entries.first().value.id,
                )
                SectionNavigationGroup(
                    titleRes = group.titleRes,
                    entries = entries,
                    selectedSectionIndex = FEATURED_SECTION_INDEX,
                )
            }
        }
    }

    @Composable
    private fun SettingsSectionRail(
        state: RenderState,
        modifier: Modifier,
    ) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                SectionNavigationRow(
                    title = stringResource(R.string.settings_featured_settings),
                    supportingText = state.profileSummary,
                    iconRes = R.drawable.ic_m3_tune,
                    selected = state.selectedSectionIndex == FEATURED_SECTION_INDEX,
                    onClick = { listener?.onSectionRequested(FEATURED_SECTION_INDEX) },
                )
            }
            val indexedSections = state.screen.sections.withIndex().toList()
            val clusters = indexedSections.groupBy { indexed ->
                SettingsPresentationCatalog.forSection(indexed.value.id).id
            }
            clusters.values.forEach { entries ->
                val group = SettingsPresentationCatalog.forSection(
                    entries.first().value.id,
                )
                SectionNavigationGroup(
                    titleRes = group.titleRes,
                    entries = entries,
                    selectedSectionIndex = state.selectedSectionIndex,
                )
            }
        }
    }

    @Composable
    private fun SettingsSectionContent(section: SettingsScreenState.Section) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .testTag(SECTION_TEST_TAG_PREFIX + section.id),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SettingsGroups(
                groups = section.groups,
                showIcons = false,
            )
        }
    }

    @Composable
    private fun SettingsGroups(
        groups: List<SettingsScreenState.Group>,
        compact: Boolean = false,
        showIcons: Boolean = true,
    ) {
        groups.forEach { group ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(group.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                SettingsItemGroup(group.rows, compact, showIcons)
            }
        }
    }

    @Composable
    private fun SettingsItemGroup(
        rows: List<SettingsScreenState.Row>,
        compact: Boolean,
        showIcons: Boolean,
    ) {
        if (rows.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_no_items),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
            return
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            rows.forEachIndexed { index, row ->
                SettingsRow(row, compact, showIcons)
                if (index != rows.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(
                            start = if (showIcons) 64.dp else 20.dp,
                            end = 20.dp,
                        ),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }

    @Composable
    private fun SettingsRow(
        row: SettingsScreenState.Row,
        compact: Boolean,
        showIcon: Boolean,
        contextLabel: String? = null,
    ) {
        val haptics = LocalHapticFeedback.current
        val enabledModifier = if (row.isEnabled) Modifier else Modifier.alpha(0.45f)
        val hasInlineEditor = row.hasInlineChoices() ||
            (row.hasNumericSlider() && !compact)
        if (hasInlineEditor) {
            InlineSettingsRow(
                row = row,
                compact = compact,
                showIcon = showIcon,
                modifier = enabledModifier,
            )
            return
        }
        val supportingText = when {
            compact && row.hasSwitchControl() -> stringResource(
                if (row.isChecked) {
                    R.string.settings_value_enabled
                } else {
                    R.string.settings_value_disabled
                },
            )
            compact -> row.valueText?.toString()
            row.hasSwitchControl() -> row.summary?.toString()
            row.hasValueControl() -> row.valueText?.toString()
                ?: row.summary?.toString()
            else -> row.summary?.toString()
        }
        val displayedSupportingText = listOfNotNull(contextLabel, supportingText)
            .joinToString(" · ")
            .takeIf { it.isNotEmpty() }
        Column(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = {
                Text(
                    text = row.title.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = displayedSupportingText?.let { supporting ->
                {
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (compact) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            leadingContent = if (showIcon && row.iconRes != 0) {
                {
                    Icon(
                        painter = painterResource(row.iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            } else {
                null
            },
            trailingContent = {
                if (row.hasSwitchControl()) {
                    MoonlightSwitch(
                        checked = row.isChecked,
                        onCheckedChange = { checked ->
                            listener?.onSwitchChanged(row.id, checked)
                        },
                        enabled = row.isEnabled,
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_m3_chevron_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ROW_TEST_TAG_PREFIX + row.id)
                .then(enabledModifier)
                .clickable(
                    enabled = row.isEnabled,
                ) {
                    if (row.hasSwitchControl()) {
                        val checked = !row.isChecked
                        haptics.performToggleHapticFeedback(checked)
                        listener?.onSwitchChanged(row.id, checked)
                    } else {
                        listener?.onItemRequested(row.id)
                    }
                }
                .padding(horizontal = 4.dp, vertical = 2.dp),
        )
        }
    }

    @Composable
    private fun InlineSettingsRow(
        row: SettingsScreenState.Row,
        compact: Boolean,
        showIcon: Boolean,
        modifier: Modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ROW_TEST_TAG_PREFIX + row.id)
                .then(modifier)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showIcon && row.iconRes != 0) {
                    Icon(
                        painter = painterResource(row.iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    text = row.title.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = if (showIcon && row.iconRes != 0) 20.dp else 0.dp),
                )
                if (row.hasNumericSlider() && !compact) {
                    row.valueText?.let { value ->
                        Text(
                            text = value.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.padding(
                    start = if (showIcon && row.iconRes != 0) 44.dp else 0.dp,
                ),
            ) {
                when {
                    row.hasDiscreteSlider() ->
                        InlineDiscreteSlider(row)
                    row.hasInlineChoices() -> InlineChoiceSegments(row)
                    row.hasNumericSlider() && !compact ->
                        InlineNumericSlider(row)
                }
            }
        }
    }

    @Composable
    private fun InlineChoiceSegments(row: SettingsScreenState.Row) {
        val haptics = LocalHapticFeedback.current
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
        ) {
            row.inlineChoices.forEachIndexed { index, choice ->
                SegmentedButton(
                    selected = choice.value == row.selectedChoiceValue,
                    onClick = {
                        if (choice.value != row.selectedChoiceValue) {
                            haptics.performHapticFeedback(
                                HapticFeedbackType.SegmentTick,
                            )
                            listener?.onInlineChoiceChanged(row.id, choice.value)
                        }
                    },
                    enabled = row.isEnabled,
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = row.inlineChoices.size,
                    ),
                    label = {
                        Text(
                            text = choice.label.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }
    }

    @Composable
    private fun InlineDiscreteSlider(row: SettingsScreenState.Row) {
        val selectedIndex = row.inlineChoices.indexOfFirst {
            it.value == row.selectedChoiceValue
        }.coerceAtLeast(0)
        var sliderPosition by remember(row.id, row.selectedChoiceValue) {
            mutableFloatStateOf(selectedIndex.toFloat())
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            MoonlightStepSlider(
                value = sliderPosition,
                onValueChange = {
                    it.roundToInt().coerceIn(0, row.inlineChoices.lastIndex).toFloat()
                },
                valueRange = 0f..row.inlineChoices.lastIndex.toFloat(),
                steps = (row.inlineChoices.size - 2).coerceAtLeast(0),
                modifier = Modifier.fillMaxWidth().height(40.dp),
                onEffectiveValueChanged = { effective ->
                    val index = effective.roundToInt()
                        .coerceIn(0, row.inlineChoices.lastIndex)
                    sliderPosition = index.toFloat()
                    listener?.onInlineChoiceChanged(
                        row.id,
                        row.inlineChoices[index].value,
                    )
                },
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                row.inlineChoices.forEach { choice ->
                    Text(
                        text = choice.label.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (choice.value == row.selectedChoiceValue) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    @Composable
    private fun InlineNumericSlider(row: SettingsScreenState.Row) {
        var sliderValue by remember(row.id, row.sliderValue) {
            mutableFloatStateOf(row.sliderValue.toFloat())
        }
        MoonlightStepSlider(
            value = sliderValue,
            onValueChange = { raw ->
                val stepped = ((raw.roundToInt() - row.sliderMinimum) /
                    row.sliderStep) * row.sliderStep + row.sliderMinimum
                stepped.coerceIn(row.sliderMinimum, row.sliderMaximum).toFloat()
            },
            valueRange = row.sliderMinimum.toFloat()..row.sliderMaximum.toFloat(),
            onEffectiveValueChanged = { effective ->
                val value = effective.roundToInt()
                    .coerceIn(row.sliderMinimum, row.sliderMaximum)
                sliderValue = value.toFloat()
                listener?.onInlineSliderChanged(row.id, value)
            },
            modifier = Modifier.fillMaxWidth().height(40.dp),
        )
    }

    @Composable
    private fun SectionNavigationGroup(
        titleRes: Int,
        entries: List<IndexedValue<SettingsScreenState.Section>>,
        selectedSectionIndex: Int,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                entries.forEachIndexed { position, indexed ->
                    SectionNavigationRow(
                        title = indexed.value.title.toString(),
                        supportingText = stringResource(
                            SettingsPresentationCatalog.summaryForSection(
                                indexed.value.id,
                            ),
                        ),
                        iconRes = indexed.value.iconRes,
                        selected = indexed.index == selectedSectionIndex,
                        testTag = SECTION_CARD_TEST_TAG_PREFIX + indexed.value.id,
                        onClick = { listener?.onSectionRequested(indexed.index) },
                    )
                    if (position != entries.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 64.dp, end = 20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun SectionNavigationRow(
        title: String,
        supportingText: String,
        iconRes: Int,
        selected: Boolean,
        testTag: String? = null,
        onClick: () -> Unit,
    ) {
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (testTag == null) Modifier else Modifier.testTag(testTag))
                .clickable(onClick = onClick),
            headlineContent = { Text(title) },
            supportingContent = {
                Text(
                    text = supportingText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingContent = {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            },
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_m3_chevron_right),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    Color.Transparent
                },
            ),
        )
    }

    companion object {
        const val FEATURED_SECTION_INDEX = -1
        private const val WIDE_LAYOUT_MIN_WIDTH_DP = 720
        private const val ROOT_PAGE_KEY = "featured"
        const val ROOT_TEST_TAG = "settings:root"
        const val FEATURED_TEST_TAG = "settings:featured"
        const val SECTION_TEST_TAG_PREFIX = "settings:page:"
        const val SECTION_CARD_TEST_TAG_PREFIX = "settings:section:"
        const val ROW_TEST_TAG_PREFIX = "settings:row:"
    }
}
