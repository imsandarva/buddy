package com.sandarva.kotlinapps.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.sandarva.kotlinapps.brain.BuddyBrain
import com.sandarva.kotlinapps.debug.BuddyLog
import com.sandarva.kotlinapps.ui.home.LiveBuddyBar
import com.sandarva.kotlinapps.ui.theme.BuddyTheme

/** Compact live pill. WRAP_CONTENT so only the pill eats touches — not a full-width sheet. */
class LiveOverlayWindow(private val context: Context) : OverlayChrome.Layer {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var owner: OverlayComposeOwner? = null
    private var view: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null
    val isShowing: Boolean get() = view != null

    fun show() {
        if (view != null) return
        val nextOwner = OverlayComposeOwner()
        owner = nextOwner
        val layout = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            liveFlags(),
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL }
        nextOwner.start()
        val compose = ComposeView(context).apply {
            hideFromBuddyEyes()
            setViewTreeLifecycleOwner(nextOwner)
            setViewTreeViewModelStoreOwner(nextOwner)
            setViewTreeSavedStateRegistryOwner(nextOwner)
            setContent {
                BuddyTheme {
                    LiveBuddyBar(
                        onStop = { BuddyBrain.cancel() },
                        onTypeInstead = { BuddyBrain.openTypeAsk() }
                    )
                }
            }
        }
        windowManager.addView(compose, layout)
        view = compose
        params = layout
        OverlayChrome.attach(this)
        BuddyLog.d("LiveOverlay", "show")
    }

    fun dismiss() {
        OverlayChrome.detach(this)
        val current = view ?: return
        current.disposeComposition()
        windowManager.removeView(current)
        view = null
        params = null
        owner?.dispose()
        owner = null
        BuddyLog.d("LiveOverlay", "dismiss")
    }

    override fun setPassthrough(on: Boolean) {
        val layout = params ?: return
        val host = view ?: return
        windowManager.applyPassthrough(host, layout, on, liveFlags())
    }

    private fun liveFlags(): Int =
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
}
