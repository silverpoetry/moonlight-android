package com.limelight

import android.widget.ImageView
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.limelight.ui.compose.components.MoonlightActionRow
import com.limelight.ui.compose.components.MoonlightGroup
import com.limelight.ui.compose.components.MoonlightScreen
import com.limelight.ui.compose.theme.MoonlightThemeFromSettings
import com.limelight.utils.HelpLauncher

/** Displays product identity and durable project resources. */
class AboutActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MoonlightThemeFromSettings {
                AboutScreen(
                    versionName = BuildConfig.VERSION_NAME,
                    onBack = ::finish,
                    onProject = { HelpLauncher.launchUrl(this, PROJECT_URL) },
                    onDocumentation = {
                        HelpLauncher.launchUrl(this, DOCUMENTATION_URL)
                    },
                    onLicense = { HelpLauncher.launchUrl(this, LICENSE_URL) },
                )
            }
        }
    }

    private companion object {
        const val PROJECT_URL =
            "https://github.com/silverpoetry/moonlight-android"
        const val DOCUMENTATION_URL =
            "https://github.com/moonlight-stream/moonlight-docs/wiki/Setup-Guide"
        const val LICENSE_URL = "$PROJECT_URL/blob/master/LICENSE.txt"
    }
}

@Composable
private fun AboutScreen(
    versionName: String,
    onBack: () -> Unit,
    onProject: () -> Unit,
    onDocumentation: () -> Unit,
    onLicense: () -> Unit,
) {
    MoonlightScreen(
        title = stringResource(R.string.category_about_settings),
        onBack = onBack,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            AndroidView(
                factory = { viewContext ->
                    ImageView(viewContext).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        setImageDrawable(
                            viewContext.applicationInfo.loadIcon(
                                viewContext.packageManager,
                            ),
                        )
                        importantForAccessibility =
                            android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }
                },
                modifier = Modifier.size(88.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.app_label),
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = stringResource(R.string.about_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(
                    R.string.about_version_format,
                    versionName,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(28.dp))
            MoonlightGroup(modifier = Modifier.fillMaxWidth()) {
                MoonlightActionRow(
                    title = stringResource(R.string.about_project),
                    iconRes = R.drawable.ic_github,
                    onClick = onProject,
                )
                MoonlightActionRow(
                    title = stringResource(R.string.about_documentation),
                    iconRes = R.drawable.ic_app_about,
                    onClick = onDocumentation,
                )
                MoonlightActionRow(
                    title = stringResource(R.string.about_license),
                    iconRes = R.drawable.ic_lock_screen,
                    onClick = onLicense,
                    showDivider = false,
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
