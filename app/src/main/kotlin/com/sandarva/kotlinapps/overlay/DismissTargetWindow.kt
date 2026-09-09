package com.sandarva.kotlinapps.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.sandarva.kotlinapps.R
import com.sandarva.kotlinapps.ui.cursor.DismissTarget
import com.sandarva.kotlinapps.ui.theme.BuddyTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fixed bottom overlay for the dismiss X. Not-touchable on purpose: the cursor keeps the drag,
 * and we arm by geometry ([DismissZone]), never by the finger landing on this window.
 * Attached under the cursor window so Buddy stays visually on top while approaching the X.
 */
class DismissTargetWindow(
    private val context: Context,
    private val windowType: Int
) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val _visible = MutableStateFlow(false)
    private val visible = _visible.asStateFlow()
    private var owner: OverlayComposeOwner? = null
    private var view: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null

    fun attach() {
        if (view != null) return
        val nextOwner = OverlayComposeOwner()
        owner = nextOwner
        val layout = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            flags(),
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = liftPx()
        }
        val compose = ComposeView(context).apply {
            hideFromBuddyEyes()
            setViewTreeLifecycleOwner(nextOwner)
            setViewTreeViewModelStoreOwner(nextOwner)
            setViewTreeSavedStateRegistryOwner(nextOwner)
            setContent {
                val shown by visible.collectAsStateWithLifecycle()
                val armed by CursorMoodSignals.armedToDismiss.collectAsStateWithLifecycle()
                BuddyTheme {
                    DismissTarget(visible = shown, armed = armed, contentDescription = context.getString(R.string.buddy_dismiss_label))
                }
            }
        }
        nextOwner.start()
        try {
            windowManager.addView(compose, layout)
        } catch (error: Throwable) {
            nextOwner.dispose()
            if (owner === nextOwner) owner = null
            throw error
        }
        view = compose
        params = layout
    }

    fun reveal() {
        attach()
        syncLift()
        _visible.value = true
    }

    fun conceal() { _visible.value = false }

    fun dismiss() {
        _visible.value = false
        val current = view ?: return
        current.disposeComposition()
        runCatching { windowManager.removeView(current) }
        view = null
        params = null
        owner?.dispose()
        owner = null
    }

    /** Screen-space center of the drawn circle — used for hit-testing the dragging cursor. */
    fun centerOnScreen(): Pair<Float, Float>? {
        val host = view ?: return null
        if (host.width <= 0 || host.height <= 0) return null
        val loc = IntArray(2)
        host.getLocationOnScreen(loc)
        return loc[0] + host.width / 2f to loc[1] + host.height / 2f
    }

    private fun syncLift() {
        val layout = params ?: return
        val host = view ?: return
        layout.y = liftPx()
        runCatching { windowManager.updateViewLayout(host, layout) }
    }

    private fun liftPx(): Int {
        val density = context.resources.displayMetrics.density
        return navBottomPx() + (DismissZone.BOTTOM_GAP_DP * density).toInt()
    }

    private fun navBottomPx(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return windowManager.currentWindowMetrics.windowInsets.getInsets(WindowInsets.Type.navigationBars()).bottom
        }
        return 0
    }

    private fun flags(): Int =
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
}
