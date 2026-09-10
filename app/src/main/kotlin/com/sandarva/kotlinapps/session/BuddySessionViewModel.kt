package com.sandarva.kotlinapps.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.sandarva.kotlinapps.accessibility.AccessibilityController
import com.sandarva.kotlinapps.accessibility.AccessibilitySession
import com.sandarva.kotlinapps.accessibility.BuddyScreenEyes
import com.sandarva.kotlinapps.brain.BrainSession
import com.sandarva.kotlinapps.brain.BuddyBrain
import com.sandarva.kotlinapps.data.ActivityLog
import com.sandarva.kotlinapps.data.ApiKeyStore
import com.sandarva.kotlinapps.data.LanguagePrefs
import com.sandarva.kotlinapps.data.OnboardingPrefs
import com.sandarva.kotlinapps.debug.BuddyLog
import com.sandarva.kotlinapps.overlay.BuddyCursorController
import com.sandarva.kotlinapps.overlay.BuddyOverlayController
import com.sandarva.kotlinapps.overlay.OverlaySession

/** UI facade over overlay hands, accessibility eyes, and the Gemini brain. */
class BuddySessionViewModel(app: Application) : AndroidViewModel(app) {
    val isRunning = OverlaySession.active
    val awaitingPermission = OverlaySession.awaitingPermission
    val canSeeScreen = AccessibilitySession.bound
    val awaitingAccess = AccessibilitySession.awaitingGrant
    val brainPhase = BrainSession.phase
    val brainNote = BrainSession.note
    val askOpen = BrainSession.askOpen
    val hasKey = ApiKeyStore.hasKey
    val keyInvalid = ApiKeyStore.invalid
    val hasLanguage = LanguagePrefs.hasSelected
    val languageCode = LanguagePrefs.selectedCode
    val introSeen = OnboardingPrefs.introSeen
    val activationDone = OnboardingPrefs.activationDone
    val sessionOpens = OnboardingPrefs.sessionOpens
    val recentActivity = ActivityLog.entries

    init {
        ApiKeyStore.ensure(app)
        LanguagePrefs.ensure(app)
        OnboardingPrefs.ensure(app)
        ActivityLog.ensure(app)
        AccessibilityController.refresh(app)
        BuddyBrain.ensure(app)
    }

    fun startBuddy() {
        BuddyLog.d("VM", "startBuddy")
        BuddyOverlayController.requestStart(getApplication())
    }
    fun stopBuddy() {
        BuddyLog.d("VM", "stopBuddy running=${isRunning.value}")
        BuddyBrain.ensure(getApplication()).cancel()
        BuddyOverlayController.stop(getApplication())
    }
    fun watchBuddyMove() {
        BuddyLog.d("VM", "watchBuddyMove")
        BuddyCursorController.playDemo()
    }
    fun requestScreenAccess() {
        BuddyLog.d("VM", "requestScreenAccess")
        AccessibilityController.requestAccess(getApplication())
    }
    fun pointAtControl() {
        BuddyLog.d("VM", "pointAtControl")
        BuddyScreenEyes.pointToGuide()
    }
    fun listenToAsk() {
        BuddyLog.d("VM", "listenToAsk")
        BuddyBrain.ensure(getApplication()).listen()
    }
    fun askWithText(text: String) {
        BuddyLog.d("VM", "askWithText")
        BuddyBrain.ensure(getApplication()).ask(text)
    }
    fun cancelAsk() {
        BuddyLog.d("VM", "cancelAsk")
        BuddyBrain.ensure(getApplication()).cancel()
    }

    fun saveApiKey(key: String) {
        BuddyLog.d("VM", "saveApiKey")
        ApiKeyStore.save(key)
    }
    fun clearApiKey() {
        BuddyLog.d("VM", "clearApiKey")
        ApiKeyStore.clear()
    }
    fun clearApiKeyInvalid() = ApiKeyStore.clearInvalid()
    fun saveLanguage(countryCode: String) {
        BuddyLog.d("VM", "saveLanguage code=$countryCode")
        LanguagePrefs.save(countryCode)
    }
    fun markIntroSeen() = OnboardingPrefs.markIntroSeen()
    fun markActivationDone() = OnboardingPrefs.markActivationDone()
    fun noteHomeOpened() = OnboardingPrefs.noteHomeOpened()
    fun resetBuddy() {
        BuddyLog.d("VM", "resetBuddy")
        stopBuddy()
        ApiKeyStore.clear()
        LanguagePrefs.clear()
        OnboardingPrefs.clear()
        ActivityLog.clear()
    }
}
