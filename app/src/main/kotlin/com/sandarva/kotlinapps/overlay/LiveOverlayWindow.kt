package com.sandarva.kotlinapps.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.sandarva.kotlinapps.brain.BrainSession
import com.sandarva.kotlinapps.brain.BuddyBrain
import com.sandarva.kotlinapps.debug.BuddyLog
import com.sandarva.kotlinapps.ui.home.LiveBuddyBar
import com.sandarva.kotlinapps.ui.theme.BuddyTheme

/** Small bottom bar while Gemini Live is on. The rest of the screen stays tappable. */
class LiveOverlayWindow(private val context: Context) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var owner: OverlayComposeOwner? = null
    private var view: ComposeView? = null
    val isShowing: Boolean get() = view != null

    fun show() {
        if (view != null) return
        val nextOwner = OverlayComposeOwner()
        owner = nextOwner
        val layout = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            liveFlags(),
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL }
        nextOwner.start()
        val compose = ComposeView(context).apply {
            setViewTreeLifecycleOwner(nextOwner)
            setViewTreeViewModelStoreOwner(nextOwner)
            setViewTreeSavedStateRegistryOwner(nextOwner)
            setContent {
                val note by BrainSession.note.collectAsStateWithLifecycle()
                BuddyTheme {
                    LiveBuddyBar(
                        note = note,
                        onStop = { BuddyBrain.cancel() },
                        onTypeInstead = { BuddyBrain.openTypeAsk() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        windowManager.addView(compose, layout)
        view = compose
        BuddyLog.d("LiveOverlay", "show")
    }

    fun dismiss() {
        val current = view ?: return
        current.disposeComposition()
        windowManager.removeView(current)
        view = null
        owner?.dispose()
        owner = null
        BuddyLog.d("LiveOverlay", "dismiss")
    }

    private fun liveFlags(): Int =
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
}
