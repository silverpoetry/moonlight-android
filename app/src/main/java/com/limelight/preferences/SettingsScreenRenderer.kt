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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.R
import com.limelight.ui.compose.components.MoonlightScreen
import com.limelight.ui.compose.theme.MoonlightThemeFromSettings

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
    }

    private data class RenderState(
        val screen: SettingsScreenState,
        val selectedSectionIndex: Int,
        val profileSummary: String,
    )

    private var renderState: RenderState? by mutableStateOf(null)
    private var restoreVersion by mutableIntStateOf(0)
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

    fun render() {
        if (!destroyed) {
            restoreVersion++
        }
    }

    fun renderWideSelection(): Boolean {
        if (destroyed || !willUseWideLayout() || renderState == null) {
            return false
        }
        restoreVersion++
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
        restoreVersion++
    }

    fun restoreSectionListScrollY(scrollY: Int?) {
        pendingSectionRailScroll = scrollY?.coerceAtLeast(0) ?: return
        restoreVersion++
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
                    .padding(horizontal = 20.dp, vertical = 12.dp),
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
                LaunchedEffect(restoreVersion) {
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
                        .verticalScroll(contentState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    if (section == null) {
                        FeaturedSettingsContent(state)
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
        LaunchedEffect(pageKey, restoreVersion) {
            scrollState.scrollTo(pendingContentScroll)
        }
    }

    @Composable
    private fun SettingsRootContent(state: RenderState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .testTag(ROOT_TEST_TAG),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = state.profileSummary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FeaturedSettingsContent(state)
            Text(
                text = stringResource(R.string.settings_more_settings),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            SettingsSectionCards(state)
        }
    }

    @Composable
    private fun FeaturedSettingsContent(state: RenderState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .testTag(FEATURED_TEST_TAG),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_featured_settings),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            SettingsItemGroup(state.screen.featuredRows)
        }
    }

    @Composable
    private fun SettingsSectionCards(state: RenderState) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.screen.sections.forEachIndexed { index, section ->
                SectionCard(
                    section = section,
                    selected = false,
                    onClick = { listener?.onSectionRequested(index) },
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionCard(
                title = stringResource(R.string.settings_featured_settings),
                itemCount = state.screen.featuredRows.size,
                iconRes = R.drawable.ic_quick_actions,
                selected = state.selectedSectionIndex == FEATURED_SECTION_INDEX,
                onClick = { listener?.onSectionRequested(FEATURED_SECTION_INDEX) },
            )
            state.screen.sections.forEachIndexed { index, section ->
                SectionCard(
                    section = section,
                    selected = index == state.selectedSectionIndex,
                    onClick = { listener?.onSectionRequested(index) },
                )
            }
        }
    }

    @Composable
    private fun SettingsSectionContent(section: SettingsScreenState.Section) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .testTag(SECTION_TEST_TAG_PREFIX + section.id),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = pluralStringResource(
                    R.plurals.settings_item_count,
                    section.rows.size,
                    section.rows.size,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            SettingsItemGroup(section.rows)
        }
    }

    @Composable
    private fun SettingsItemGroup(rows: List<SettingsScreenState.Row>) {
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
                SettingsRow(row)
                if (index != rows.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }

    @Composable
    private fun SettingsRow(row: SettingsScreenState.Row) {
        val enabledModifier = if (row.isEnabled) Modifier else Modifier.alpha(0.45f)
        ListItem(
            headlineContent = {
                Text(
                    text = row.title.toString(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = row.summary?.let { summary ->
                {
                    Text(
                        text = summary.toString(),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            leadingContent = {
                if (row.iconRes != 0) {
                    Icon(
                        painter = painterResource(row.iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            trailingContent = {
                if (row.hasSwitchControl()) {
                    Switch(
                        checked = row.isChecked,
                        onCheckedChange = if (row.isEnabled) {
                            { checked -> listener?.onSwitchChanged(row.id, checked) }
                        } else {
                            null
                        },
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.valueText?.let { value ->
                            Text(
                                text = value.toString(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                modifier = Modifier.widthIn(max = 180.dp),
                            )
                        }
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ROW_TEST_TAG_PREFIX + row.id)
                .then(enabledModifier)
                .clickable(enabled = row.isEnabled) {
                    if (row.hasSwitchControl()) {
                        listener?.onSwitchChanged(row.id, !row.isChecked)
                    } else {
                        listener?.onItemRequested(row.id)
                    }
                }
                .padding(horizontal = 4.dp, vertical = 2.dp),
        )
    }

    @Composable
    private fun SectionCard(
        section: SettingsScreenState.Section,
        selected: Boolean,
        onClick: () -> Unit,
    ) = SectionCard(
        title = section.title.toString(),
        itemCount = section.rows.size,
        iconRes = section.iconRes,
        selected = selected,
        testTag = SECTION_CARD_TEST_TAG_PREFIX + section.id,
        onClick = onClick,
    )

    @Composable
    private fun SectionCard(
        title: String,
        itemCount: Int,
        iconRes: Int,
        selected: Boolean,
        testTag: String? = null,
        onClick: () -> Unit,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (testTag == null) Modifier else Modifier.testTag(testTag))
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            ),
        ) {
            ListItem(
                headlineContent = { Text(title) },
                supportingContent = {
                    Text(
                        pluralStringResource(
                            R.plurals.settings_item_count,
                            itemCount,
                            itemCount,
                        ),
                    )
                },
                leadingContent = {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow),
                        contentDescription = null,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
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
