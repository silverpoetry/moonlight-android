package com.limelight.ui.gamemenu

import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.limelight.R
import com.limelight.ui.compose.theme.MoonlightThemeFromSettings

/** Window/lifecycle shell for game-menu pages whose entire content is Compose. */
abstract class ComposeGameMenuDialogFragment : DialogFragment() {
    private var requestedWidthPx: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.BottomDialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        ComponentDialog(requireContext(), theme)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            MoonlightThemeFromSettings {
                DialogContent()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val window = dialog?.window ?: return
        window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        window.setDimAmount(0.4f)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat
                .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        val landscape = resources.configuration.orientation ==
            Configuration.ORIENTATION_LANDSCAPE
        window.attributes = window.attributes.apply {
            dimAmount = 0.4f
            gravity = if (landscape) Gravity.END else Gravity.BOTTOM
            width = if (landscape && requestedWidthPx > 0) {
                requestedWidthPx
            } else {
                WindowManager.LayoutParams.MATCH_PARENT
            }
            height = if (landscape) {
                WindowManager.LayoutParams.MATCH_PARENT
            } else {
                // BaseGameMenuFragmentDialog treats the requested side-panel
                // width as the corresponding bottom-sheet height in portrait.
                // Preserve that contract so navigating to a Compose subpage
                // doesn't make the menu jump from 364 dp to its 680 dp cap.
                if (requestedWidthPx > 0) {
                    requestedWidthPx
                } else {
                    WindowManager.LayoutParams.WRAP_CONTENT
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        (activity as? GameMenuHostProvider)
            ?.gameMenuHost
            ?.cancelPendingStreamBackExit()
        super.onDismiss(dialog)
    }

    fun setWidth(widthPx: Int) {
        requestedWidthPx = widthPx
    }

    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, javaClass.name)
    }

    @Composable
    protected abstract fun DialogContent()
}
