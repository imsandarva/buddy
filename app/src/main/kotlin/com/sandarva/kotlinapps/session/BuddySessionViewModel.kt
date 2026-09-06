package com.sandarva.kotlinapps.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.sandarva.kotlinapps.accessibility.AccessibilityController
import com.sandarva.kotlinapps.accessibility.AccessibilitySession
import com.sandarva.kotlinapps.accessibility.BuddyScreenEyes
import com.sandarva.kotlinapps.overlay.BuddyCursorController
import com.sandarva.kotlinapps.overlay.BuddyOverlayController
import com.sandarva.kotlinapps.overlay.OverlaySession

/** UI facade over overlay hands and accessibility eyes. */
class BuddySessionViewModel(app: Application) : AndroidViewModel(app) {
    val isRunning = OverlaySession.active
    val awaitingPermission = OverlaySession.awaitingPermission
    val canSeeScreen = AccessibilitySession.bound
    val awaitingAccess = AccessibilitySession.awaitingGrant

    init { AccessibilityController.refresh(app) }

    fun startBuddy() = BuddyOverlayController.requestStart(getApplication())
    fun stopBuddy() = BuddyOverlayController.stop(getApplication())
    fun watchBuddyMove() = BuddyCursorController.playDemo()
    fun requestScreenAccess() = AccessibilityController.requestAccess(getApplication())
    fun pointAtControl() = BuddyScreenEyes.pointToGuide()
}
