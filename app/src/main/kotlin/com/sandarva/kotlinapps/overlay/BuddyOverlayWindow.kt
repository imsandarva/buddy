package com.sandarva.kotlinapps.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.sandarva.kotlinapps.R
import com.sandarva.kotlinapps.debug.BuddyLog
import com.sandarva.kotlinapps.ui.cursor.BuddyCursorHandle
import com.sandarva.kotlinapps.ui.theme.BuddyTheme

/** Small WRAP_CONTENT window. Drag or animateTo moves the same LayoutParams. */
class BuddyOverlayWindow(
    private val context: Context,
    private val onAsk: () -> Unit = {}
) : BuddyCursorMover {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val owner = OverlayComposeOwner()
    private var view: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null
    private var flight: CursorFlightAnimator? = null
    private var pathGen = 0
    val isShowing: Boolean get() = view != null

    fun show() {
        if (view != null) return
        val screen = screenSize()
        val start = OverlaySession.placement
        val layout = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            overlayFlags(),
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (screen.first * start.xFraction).toInt()
            y = (screen.second * start.yFraction).toInt()
        }
        val compose = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                BuddyTheme {
                    BuddyCursorHandle(
                        onGrab = { cancelFlight() },
                        onDrag = { dx, dy -> moveBy(dx, dy) },
                        onRelease = { persist() },
                        onDoubleTap = onAsk,
                        label = context.getString(R.string.buddy_cursor_label)
                    )
                }
            }
        }
        owner.start()
        windowManager.addView(compose, layout)
        view = compose
        params = layout
        flight = CursorFlightAnimator(compose, ::currentXY, ::applyPixels)
    }

    fun raise() {
        val current = view ?: return
        val layout = params ?: return
        windowManager.removeView(current)
        windowManager.addView(current, layout)
    }

    fun dismiss() {
        cancelFlight()
        OverlaySession.setPressing(false)
        flight = null
        val current = view ?: return
        current.disposeComposition()
        windowManager.removeView(current)
        view = null
        params = null
        owner.dispose()
    }

    override fun animateToPixels(xPx: Float, yPx: Float, onLanded: (() -> Unit)?) {
        BuddyLog.d("Overlay.flyTo", "x=$xPx y=$yPx")
        pathGen += 1
        val gen = pathGen
        val anim = flight
        if (anim == null) {
            applyPixels(xPx, yPx)
            persist()
            onLanded?.invoke()
            return
        }
        anim.flyTo(xPx, yPx) {
            if (gen != pathGen) return@flyTo
            persist()
            onLanded?.invoke()
        }
    }

    override fun slideToPixels(xPx: Float, yPx: Float, durationMs: Long, onLanded: (() -> Unit)?) {
        pathGen += 1
        val gen = pathGen
        val anim = flight
        if (anim == null) {
            applyPixels(xPx, yPx)
            persist()
            onLanded?.invoke()
            return
        }
        anim.slideTo(xPx, yPx, durationMs) {
            if (gen != pathGen) return@slideTo
            persist()
            onLanded?.invoke()
        }
    }

    override fun tipPixels(): Pair<Float, Float> = currentXY()
    override fun screenPixels(): Pair<Int, Int> = screenSize()

    override fun setPassthrough(on: Boolean) {
        val layout = params ?: return
        val host = view ?: return
        // Android 12+ drops touches through an opaque SAW; fade just enough to stay trusted.
        layout.flags = if (on) overlayFlags() or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else overlayFlags()
        layout.alpha = if (on) PASS_ALPHA else 1f
        host.post { if (params === layout && view === host) windowManager.updateViewLayout(host, layout) }
    }

    override fun animateToNormalized(x: Float, y: Float) {
        val (px, py) = normalizedToPixels(x, y)
        animateToPixels(px, py)
    }

    override fun animateToGrid999(x: Int, y: Int) = animateToNormalized(x / 999f, y / 999f)

    override fun nudgeNormalized(dx: Float, dy: Float) {
        val screen = screenSize()
        if (screen.first <= 0 || screen.second <= 0) return
        val now = currentXY()
        val nx = (now.first / screen.first + dx).coerceIn(0.04f, 0.96f)
        val ny = (now.second / screen.second + dy).coerceIn(0.04f, 0.92f)
        BuddyLog.d("Overlay.nudge", "dx=$dx dy=$dy to=$nx,$ny")
        animateToNormalized(nx, ny)
    }

    override fun playPath(normalized: List<Pair<Float, Float>>) {
        val gen = ++pathGen
        flyStep(0, normalized, gen)
    }

    override fun cancelFlight() {
        pathGen += 1
        flight?.cancel()
    }

    private fun flyStep(index: Int, points: List<Pair<Float, Float>>, gen: Int) {
        if (gen != pathGen) return
        if (index >= points.size) { persist(); return }
        val (px, py) = normalizedToPixels(points[index].first, points[index].second)
        flight?.flyTo(px, py) { flyStep(index + 1, points, gen) }
    }

    private fun moveBy(dx: Float, dy: Float) {
        val now = currentXY()
        applyPixels(now.first + dx, now.second + dy)
    }

    private fun applyPixels(x: Float, y: Float) {
        val host = view
        if (host != null && Looper.myLooper() != Looper.getMainLooper()) {
            host.post { applyPixels(x, y) }
            return
        }
        val layout = params ?: return
        val screen = screenSize()
        val w = host?.width?.takeIf { it > 0 } ?: 80
        val h = host?.height?.takeIf { it > 0 } ?: 80
        layout.x = x.toInt().coerceIn(0, (screen.first - w).coerceAtLeast(0))
        layout.y = y.toInt().coerceIn(0, (screen.second - h).coerceAtLeast(0))
        host?.let { windowManager.updateViewLayout(it, layout) }
    }

    private fun currentXY(): Pair<Float, Float> {
        val layout = params ?: return 0f to 0f
        return layout.x.toFloat() to layout.y.toFloat()
    }

    private fun normalizedToPixels(nx: Float, ny: Float): Pair<Float, Float> {
        val screen = screenSize()
        return screen.first * nx.coerceIn(0f, 1f) to screen.second * ny.coerceIn(0f, 1f)
    }

    private fun persist() {
        val layout = params ?: return
        val screen = screenSize()
        if (screen.first > 0 && screen.second > 0) OverlaySession.savePlacement(layout.x / screen.first.toFloat(), layout.y / screen.second.toFloat())
    }

    private fun screenSize(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val metrics = context.resources.displayMetrics
            metrics.widthPixels to metrics.heightPixels
        }
    }

    private fun overlayFlags(): Int {
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        return flags
    }

    companion object {
        /** At or below [WindowManager.LayoutParams] maximum obscuring opacity so pass-through taps reach the app. */
        private const val PASS_ALPHA = 0.79f
    }
}
