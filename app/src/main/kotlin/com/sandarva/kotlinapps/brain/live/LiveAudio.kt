package com.sandarva.kotlinapps.brain.live

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioRecord
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AudioEffect
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import com.sandarva.kotlinapps.debug.BuddyLog

/**
 * One audio session for mic + speaker, routed like a speakerphone call.
 * That is the path Android’s AEC is built for — same idea as the official Gemini app.
 */
class LiveAudio(context: Context) {
    private val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val sessionId: Int = am.generateAudioSessionId()
    private val effects = ArrayList<AudioEffect>(3)
    private var previousMode = AudioManager.MODE_NORMAL
    @Suppress("DEPRECATION")
    private var previousSpeaker = false

    fun enterVoiceRoute() {
        previousMode = am.mode
        @Suppress("DEPRECATION")
        previousSpeaker = am.isSpeakerphoneOn
        am.mode = AudioManager.MODE_IN_COMMUNICATION
        routeToSpeaker()
        BuddyLog.d("Live.audio", "voice route session=$sessionId")
    }

    fun attachRecord(record: AudioRecord) {
        releaseEffects()
        val id = record.audioSessionId
        add(AcousticEchoCanceler.isAvailable()) { AcousticEchoCanceler.create(id) }
        add(NoiseSuppressor.isAvailable()) { NoiseSuppressor.create(id) }
        add(AutomaticGainControl.isAvailable()) { AutomaticGainControl.create(id) }
        BuddyLog.d("Live.audio", "session=$sessionId record=$id effects=${effects.size}")
    }

    fun release() {
        releaseEffects()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.clearCommunicationDevice()
        @Suppress("DEPRECATION")
        am.isSpeakerphoneOn = previousSpeaker
        am.mode = previousMode
    }

    private fun routeToSpeaker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val speaker = am.availableCommunicationDevices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            if (speaker != null && am.setCommunicationDevice(speaker)) return
        }
        @Suppress("DEPRECATION")
        am.isSpeakerphoneOn = true
    }

    private fun add(available: Boolean, create: () -> AudioEffect?) {
        if (!available) return
        val fx = runCatching { create() }.getOrNull() ?: return
        runCatching { fx.enabled = true }
        effects += fx
    }

    private fun releaseEffects() {
        effects.forEach { runCatching { it.release() } }
        effects.clear()
    }

    companion object {
        fun canBindRecordSession(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    }
}
