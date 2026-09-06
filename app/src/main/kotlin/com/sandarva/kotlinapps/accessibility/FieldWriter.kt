package com.sandarva.kotlinapps.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.EditorInfo
import androidx.annotation.RequiresApi
import com.sandarva.kotlinapps.debug.BuddyLog
import com.sandarva.kotlinapps.overlay.BuddyCursorController
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.hypot

/**
 * Writes into a live editable node.
 * Primary: [AccessibilityNodeInfo.ACTION_SET_TEXT] (Voice Access / UI Automator).
 * Android 13+: accessibility IME [commitText] if set-text fails.
 * Last: clipboard paste. Submit uses IME enter, not a typed newline.
 */
class FieldWriter(private val service: AccessibilityService) {
    private val picker = WindowRootPicker(service)
    private val main = Handler(Looper.getMainLooper())
    private val box = Rect()

    suspend fun write(text: String, target: FieldTarget, submit: Boolean): Boolean {
        if (target.bounds != null && BuddyHands.isReady()) {
            if (!BuddyHands.tapAt(target.bounds.centerX, target.bounds.centerY)) return false
            delay(FOCUS_MS)
        }
        val ok = onMain { putText(text, target) } == true
        BuddyLog.d("Type.put", "ok=$ok submit=$submit")
        if (!ok) return false
        if (!submit) return true
        delay(SUBMIT_GAP_MS)
        return onMain { pressIme(target) } == true
    }

    suspend fun submit(target: FieldTarget): Boolean = onMain { pressIme(target) } == true

    private fun putText(text: String, target: FieldTarget): Boolean {
        val node = find(target) ?: return false
        try {
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            if (setText(node, text)) return true
            if (imeCommit(text)) return true
            return paste(node, text)
        } finally {
            AccessibilityNodes.recycle(node)
        }
    }

    private fun pressIme(target: FieldTarget): Boolean {
        val node = find(target)
        try {
            if (node != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (node.performAction(android.R.id.accessibilityActionImeEnter)) return true
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && imeAction()) return true
            return node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
        } finally {
            if (node != null) AccessibilityNodes.recycle(node)
        }
    }

    private fun setText(node: AccessibilityNodeInfo, text: String): Boolean {
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun imeAction(): Boolean {
        val ic = service.inputMethod?.currentInputConnection ?: return false
        ic.performEditorAction(EditorInfo.IME_ACTION_GO)
        return true
    }

    private fun imeCommit(text: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        return imeReplace(text)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun imeReplace(text: String): Boolean {
        val ic = service.inputMethod?.currentInputConnection ?: return false
        ic.deleteSurroundingText(CLEAR_SPAN, CLEAR_SPAN)
        ic.commitText(text, 1, null)
        return true
    }

    private fun paste(node: AccessibilityNodeInfo, text: String): Boolean {
        val clip = service.getSystemService(ClipboardManager::class.java) ?: return false
        val prior = clip.primaryClip
        clip.setPrimaryClip(ClipData.newPlainText("buddy", text))
        val ok = node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        if (prior != null) clip.setPrimaryClip(prior) else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) clip.clearPrimaryClip()
        return ok
    }

    private fun find(target: FieldTarget): AccessibilityNodeInfo? {
        val roots = picker.roots()
        if (roots.isEmpty()) return null
        val fields = ArrayList<LiveField>(12)
        for (root in roots) collect(root, fields)
        val tip = BuddyCursorController.tipPixels()
        val hit = pick(fields, target, tip)
        val keep = hit?.let { AccessibilityNodes.copy(it.node) }
        for (field in fields) AccessibilityNodes.recycle(field.node)
        for (root in roots) AccessibilityNodes.recycle(root)
        return keep
    }

    private fun collect(node: AccessibilityNodeInfo, into: ArrayList<LiveField>) {
        if (canType(node)) {
            node.getBoundsInScreen(box)
            if (box.width() >= 8 && box.height() >= 8) {
                val viewId = ScreenTreeWalker.shortViewId(node.viewIdResourceName)
                into += LiveField(
                    node = AccessibilityNodes.copy(node),
                    id = viewId?.takeIf { it.isNotBlank() } ?: ScreenTreeWalker.slug(labelOf(node, viewId)),
                    viewId = viewId,
                    bounds = ScreenBounds(box.left, box.top, box.right, box.bottom),
                    focused = node.isFocused
                )
            }
        }
        val count = node.childCount
        for (i in 0 until count) {
            val child = AccessibilityNodes.child(node, i) ?: continue
            collect(child, into)
            AccessibilityNodes.recycle(child)
        }
    }

    private fun pick(fields: List<LiveField>, target: FieldTarget, tip: Pair<Float, Float>?): LiveField? {
        if (fields.isEmpty()) return null
        target.viewId?.let { id -> fields.find { it.viewId == id } }?.let { return it }
        target.bounds?.let { want ->
            fields.minByOrNull { dist(it.bounds, want.centerX, want.centerY) }
                ?.takeIf { dist(it.bounds, want.centerX, want.centerY) < MATCH_PX }
                ?.let { return it }
        }
        target.id?.let { id ->
            fields.find { it.id == id || it.viewId == id }?.let { return it }
        }
        fields.find { it.focused }?.let { return it }
        if (tip != null) return fields.minByOrNull { dist(it.bounds, tip.first, tip.second) }
        return fields.first()
    }

    private fun canType(node: AccessibilityNodeInfo): Boolean {
        if (node.isEditable) return true
        return node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_SET_TEXT }
    }

    private fun labelOf(node: AccessibilityNodeInfo, viewId: String?): String {
        val raw = sequenceOf(node.text, node.contentDescription, hintOf(node))
            .map { it?.toString()?.trim() }
            .firstOrNull { !it.isNullOrBlank() }
        if (!raw.isNullOrBlank()) return raw.take(80)
        if (!viewId.isNullOrBlank()) return viewId.replace('_', ' ')
        return "text field"
    }

    private fun hintOf(node: AccessibilityNodeInfo): CharSequence? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) node.hintText else null

    private fun dist(bounds: ScreenBounds, x: Float, y: Float): Float =
        hypot(bounds.centerX - x, bounds.centerY - y)

    private suspend fun <T> onMain(block: () -> T): T? = suspendCancellableCoroutine { cont ->
        val run = Runnable { if (cont.isActive) cont.resume(block()) }
        if (Looper.myLooper() == Looper.getMainLooper()) run.run() else main.post(run)
        cont.invokeOnCancellation { main.removeCallbacks(run) }
    }

    private data class LiveField(
        val node: AccessibilityNodeInfo,
        val id: String,
        val viewId: String?,
        val bounds: ScreenBounds,
        val focused: Boolean
    )

    companion object {
        private const val FOCUS_MS = 220L
        private const val SUBMIT_GAP_MS = 80L
        private const val MATCH_PX = 96f
        private const val CLEAR_SPAN = 10_000
    }
}
