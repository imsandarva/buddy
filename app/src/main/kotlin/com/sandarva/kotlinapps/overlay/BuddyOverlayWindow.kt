package com.sandarva.kotlinapps.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.sandarva.kotlinapps.R
import com.sandarva.kotlinapps.debug.BuddyLog
import com.sandarva.kotlinapps.ui.cursor.BuddyCursorHandle
import com.sandarva.kotlinapps.ui.cursor.Cursor
import com.sandarva.kotlinapps.ui.theme.BuddyTheme

/**
 * Small, fixed-size WRAP_CONTENT window. Drag or animateTo moves the same LayoutParams; the
 * window itself never resizes as buddy morphs — see [Cursor.touchWidth]. Also reports the raw
 * motion/drag facts a mood needs (traveling, velocity, user-drag, dismiss-zone) into
 * [CursorMoodSignals] — this class owns the *physical* truths, never the drawn mood itself.
 */
class BuddyOverlayWindow(
    private val context: Context,
    private val windowType: Int = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
    private val onAsk: () -> Unit = {},
    private val onInterrupt: () -> Unit = {},
    private val onDismiss: () -> Unit = {}
) : BuddyCursorMover, OverlayChrome.Layer {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val owner = OverlayComposeOwner()
    private val dismissTarget = DismissTargetWindow(context, windowType)
    private var view: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null
    private var flight: CursorFlightAnimator? = null
    private var pathGen = 0
    private var lastWindowX = 0f
    private var lastWindowY = 0f
    val isShowing: Boolean get() = view != null

    fun show() {
        if (view != null) return
        dismissTarget.attach()
        try {
            val screen = screenSize()
            val start = OverlaySession.placement
            val layout = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                windowType,
                overlayFlags(),
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                val (tipOx, tipOy) = tipOffsetPx()
                x = ((screen.first * start.xFraction) - tipOx).toInt()
                y = ((screen.second * start.yFraction) - tipOy).toInt()
            }
            lastWindowX = layout.x.toFloat()
            lastWindowY = layout.y.toFloat()
            val compose = ComposeView(context).apply {
                hideFromBuddyEyes()
                setViewTreeLifecycleOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setContent {
                    val mood by CursorMoodResolver.mood.collectAsStateWithLifecycle()
                    BuddyTheme {
                        BuddyCursorHandle(
                            mood = mood,
                            onGrab = { grab() },
                            onDrag = { dx, dy -> moveBy(dx, dy) },
                            onRelease = { release() },
                            onDoubleTap = onAsk,
                            onInterrupt = onInterrupt,
                            label = context.getString(R.string.buddy_cursor_label)
                        )
                    }
                }
            }
            owner.start()
            windowManager.addView(compose, layout)
            view = compose
            params = layout
            flight = CursorFlightAnimator(compose, ::currentTipXY, ::applyPixels)
            OverlayChrome.attach(this)
        } catch (error: Throwable) {
            dismissTarget.dismiss()
            throw error
        }
    }

    fun raise() {
        val current = view ?: return
        val layout = params ?: return
        windowManager.removeView(current)
        windowManager.addView(current, layout)
    }

    fun dismiss() {
        OverlayChrome.detach(this)
        cancelFlight()
        dismissTarget.dismiss()
        CursorMoodSignals.reset()
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
        CursorMoodSignals.setTraveling(true)
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
        CursorMoodSignals.setTraveling(true)
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

    override fun tipPixels(): Pair<Float, Float> = currentTipXY()
    override fun screenPixels(): Pair<Int, Int> = screenSize()

    override fun setPassthrough(on: Boolean) {
        val layout = params ?: return
        val host = view ?: return
        windowManager.applyPassthrough(host, layout, on, overlayFlags())
    }

    override fun animateToNormalized(x: Float, y: Float) {
        val (px, py) = normalizedToPixels(x, y)
        animateToPixels(px, py)
    }

    override fun animateToGrid999(x: Int, y: Int) = animateToNormalized(x / 999f, y / 999f)

    override fun nudgeNormalized(dx: Float, dy: Float) {
        val screen = screenSize()
        if (screen.first <= 0 || screen.second <= 0) return
        val now = currentTipXY()
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
        CursorMoodSignals.setTraveling(false)
        CursorMoodSignals.setVelocity(0f, 0f)
    }

    private fun flyStep(index: Int, points: List<Pair<Float, Float>>, gen: Int) {
        if (gen != pathGen) return
        if (index >= points.size) { persist(); return }
        val (px, py) = normalizedToPixels(points[index].first, points[index].second)
        flight?.flyTo(px, py) { flyStep(index + 1, points, gen) }
    }

    private fun grab() {
        cancelFlight()
        CursorMoodSignals.setUserDragging(true)
        dismissTarget.reveal()
    }

    private fun moveBy(dx: Float, dy: Float) {
        val now = currentTipXY()
        var x = now.first + dx
        var y = now.second + dy
        val center = dismissTarget.centerOnScreen()
        if (center != null) {
            val tip = tipOnScreen()
            val density = context.resources.displayMetrics.density
            val pull = DismissZone.magnetDelta(tip.first + dx, tip.second + dy, center.first, center.second, density)
            x += pull.first
            y += pull.second
        }
        applyPixels(x, y)
        updateDismissArm()
    }

    /** Releasing over the X stops Buddy entirely — Messenger chat-head / Android Bubble pattern. */
    private fun updateDismissArm() {
        val center = dismissTarget.centerOnScreen() ?: return
        val tip = tipOnScreen()
        val density = context.resources.displayMetrics.density
        CursorMoodSignals.setArmedToDismiss(DismissZone.hits(tip.first, tip.second, center.first, center.second, density))
    }

    private fun release() {
        val dismissing = CursorMoodSignals.armedToDismiss.value
        CursorMoodSignals.setUserDragging(false)
        CursorMoodSignals.setVelocity(0f, 0f)
        if (dismissing) onDismiss() else { dismissTarget.conceal(); persist() }
    }

    private fun tipOnScreen(): Pair<Float, Float> {
        val host = view
        if (host != null && host.width > 0 && host.height > 0) {
            val loc = IntArray(2)
            host.getLocationOnScreen(loc)
            return loc[0] + host.width / 2f to loc[1] + host.height / 2f
        }
        return currentTipXY()
    }

    /** [x],[y] are tip pixels on screen; window origin is offset by [Cursor]. */
    private fun applyPixels(tipX: Float, tipY: Float) {
        val (ox, oy) = tipOffsetPx()
        applyWindowPixels(tipX - ox, tipY - oy)
    }

    private fun applyWindowPixels(x: Float, y: Float) {
        val host = view
        if (host != null && Looper.myLooper() != Looper.getMainLooper()) {
            host.post { applyWindowPixels(x, y) }
            return
        }
        val layout = params ?: return
        val screen = screenSize()
        val density = context.resources.displayMetrics.density
        val w = host?.width?.takeIf { it > 0 } ?: (Cursor.touchWidth.value * density).toInt()
        val h = host?.height?.takeIf { it > 0 } ?: (Cursor.touchHeight.value * density).toInt()
        layout.x = x.toInt().coerceIn(0, (screen.first - w).coerceAtLeast(0))
        layout.y = y.toInt().coerceIn(0, (screen.second - h).coerceAtLeast(0))
        CursorMoodSignals.setVelocity(layout.x - lastWindowX, layout.y - lastWindowY)
        lastWindowX = layout.x.toFloat()
        lastWindowY = layout.y.toFloat()
        host?.let { windowManager.updateViewLayout(it, layout) }
    }

    private fun tipOffsetPx(): Pair<Float, Float> =
        Cursor.tipOffsetPx(context.resources.displayMetrics.density)

    private fun currentTipXY(): Pair<Float, Float> {
        val layout = params ?: return 0f to 0f
        val (ox, oy) = tipOffsetPx()
        return layout.x + ox to layout.y + oy
    }

    private fun normalizedToPixels(nx: Float, ny: Float): Pair<Float, Float> {
        val screen = screenSize()
        return screen.first * nx.coerceIn(0f, 1f) to screen.second * ny.coerceIn(0f, 1f)
    }

    private fun persist() {
        CursorMoodSignals.setTraveling(false)
        CursorMoodSignals.setVelocity(0f, 0f)
        val (tx, ty) = currentTipXY()
        val screen = screenSize()
        if (screen.first > 0 && screen.second > 0) {
            OverlaySession.savePlacement(tx / screen.first.toFloat(), ty / screen.second.toFloat())
        }
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
}
