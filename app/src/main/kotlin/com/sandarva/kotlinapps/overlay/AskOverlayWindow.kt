package com.sandarva.kotlinapps.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.fillMaxSize
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
import com.sandarva.kotlinapps.ui.home.AskBuddySheet
import com.sandarva.kotlinapps.ui.theme.BuddyTheme

/** Full-screen overlay ask panel. Lives in the service so it works over the launcher and other apps. */
class AskOverlayWindow(private val context: Context) {
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
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            askFlags(),
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        nextOwner.start()
        val compose = ComposeView(context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setViewTreeLifecycleOwner(nextOwner)
            setViewTreeViewModelStoreOwner(nextOwner)
            setViewTreeSavedStateRegistryOwner(nextOwner)
            setContent {
                val phase by BrainSession.phase.collectAsStateWithLifecycle()
                val note by BrainSession.note.collectAsStateWithLifecycle()
                BuddyTheme {
                    AskBuddySheet(
                        phase = phase,
                        note = note,
                        onDismiss = { BuddyBrain.cancel() },
                        onAskText = { BuddyBrain.ask(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        windowManager.addView(compose, layout)
        view = compose
        BuddyLog.d("AskOverlay", "show")
    }

    fun dismiss() {
        val current = view ?: return
        hideKeyboard(current)
        current.disposeComposition()
        windowManager.removeView(current)
        view = null
        owner?.dispose()
        owner = null
        BuddyLog.d("AskOverlay", "dismiss")
    }

    private fun hideKeyboard(host: ComposeView) {
        val imm = context.getSystemService(InputMethodManager::class.java)
        imm.hideSoftInputFromWindow(host.windowToken, 0)
    }

    private fun askFlags(): Int =
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
}
