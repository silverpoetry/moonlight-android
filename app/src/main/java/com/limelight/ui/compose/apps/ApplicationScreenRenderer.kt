package com.limelight.ui.compose.apps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.limelight.AppView
import com.limelight.R
import com.limelight.grid.AppGridAdapter
import com.limelight.ui.compose.components.MoonlightScreen
import com.limelight.ui.compose.theme.MoonlightThemeFromSettings

/** Material 3 app browser backed by the existing app-list and artwork pipeline. */
class ApplicationScreenRenderer(
    private val context: Context,
    private var listener: Listener?,
) {
    interface Listener {
        fun onBackRequested()
        fun onAppRequested(app: AppView.AppObject)
        fun onAppMenuRequested(app: AppView.AppObject, artwork: Bitmap?)
    }

    private data class State(
        val title: String,
        val apps: List<AppView.AppObject>,
        val adapter: AppGridAdapter?,
        val loading: Boolean,
        val revision: Int,
    )

    private var state: State by mutableStateOf(
        State("", emptyList(), null, true, 0),
    )
    private var composeView: ComposeView? = null

    fun createRootView(): View {
        composeView?.let { return it }
        return ComposeView(context).also { view ->
            composeView = view
            view.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
            )
            view.setContent {
                MoonlightThemeFromSettings {
                    ApplicationScreen(state)
                }
            }
        }
    }

    fun updateTitle(title: CharSequence?) {
        state = state.copy(title = title?.toString().orEmpty())
    }

    fun updateApps(adapter: AppGridAdapter?, apps: List<AppView.AppObject>) {
        state = state.copy(
            adapter = adapter,
            apps = apps.toList(),
            loading = false,
            revision = state.revision + 1,
        )
    }

    fun showLoading() {
        state = state.copy(loading = true)
    }

    fun destroy() {
        listener = null
        composeView?.disposeComposition()
        composeView = null
    }

    @Composable
    private fun ApplicationScreen(state: State) {
        MoonlightScreen(
            title = state.title,
            onBack = { listener?.onBackRequested() },
        ) { padding ->
            when {
                state.loading -> Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
                state.apps.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.applist_refresh_msg),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(164.dp),
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = state.apps,
                        key = { it.app.appId },
                    ) { app ->
                        ApplicationCard(app, state.adapter)
                    }
                }
            }
        }
    }

    @Composable
    private fun ApplicationCard(
        app: AppView.AppObject,
        adapter: AppGridAdapter?,
    ) {
        var artworkView: ImageView? by remember(app.app.appId) { mutableStateOf(null) }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (app.isHidden) 0.5f else 1f)
                .clickable { listener?.onAppRequested(app) },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    contentAlignment = Alignment.Center,
                ) {
                    AndroidView(
                        factory = { viewContext ->
                            FrameLayout(viewContext).apply {
                                val image = ImageView(viewContext).apply {
                                    scaleType = ImageView.ScaleType.CENTER_CROP
                                    setImageResource(R.drawable.no_app_image)
                                }
                                val fallback = TextView(viewContext).apply {
                                    gravity = Gravity.CENTER
                                    text = app.app.appName
                                }
                                addView(
                                    image,
                                    FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                    ),
                                )
                                addView(
                                    fallback,
                                    FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                    ),
                                )
                                artworkView = image
                                adapter?.populateArtwork(image, fallback, app)
                            }
                        },
                        update = { frame ->
                            val image = frame.getChildAt(0) as ImageView
                            val fallback = frame.getChildAt(1) as TextView
                            artworkView = image
                            adapter?.populateArtwork(image, fallback, app)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (app.isRunning) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_m3_play_arrow),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = stringResource(R.string.applist_menu_status_running),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.app.appName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (app.isRunning || app.isHidden) {
                            Text(
                                text = stringResource(
                                    if (app.isRunning) {
                                        R.string.applist_menu_status_running
                                    } else {
                                        R.string.applist_menu_hide_app
                                    },
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            val artwork = (artworkView?.drawable as? BitmapDrawable)?.bitmap
                            listener?.onAppMenuRequested(app, artwork)
                        },
                    ) {
                        Text("⋮", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
    }
}
