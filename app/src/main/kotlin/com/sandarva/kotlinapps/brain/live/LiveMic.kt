package com.sandarva.kotlinapps.brain.live

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.sandarva.kotlinapps.debug.BuddyLog
import kotlin.concurrent.thread

/** 16 kHz mono PCM16 capture for Live realtimeInput.audio. */
class LiveMic(private val onChunk: (ByteArray) -> Unit) {
    @Volatile private var record: AudioRecord? = null
    @Volatile private var running = false

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (running) return true
        val min = AudioRecord.getMinBufferSize(LiveConfig.IN_HZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) {
            BuddyLog.e("Live.mic", "bad minBuffer=$min")
            return false
        }
        val chunk = (LiveConfig.IN_HZ * LiveConfig.CHUNK_MS / 1000) * 2
        val buffer = maxOf(min, chunk * 2)
        val next = AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, LiveConfig.IN_HZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buffer)
        if (next.state != AudioRecord.STATE_INITIALIZED) {
            BuddyLog.e("Live.mic", "AudioRecord failed state=${next.state}")
            next.release()
            return false
        }
        record = next
        running = true
        next.startRecording()
        thread(name = "buddy-live-mic", isDaemon = true) {
            val buf = ByteArray(chunk)
            while (running) {
                val n = record?.read(buf, 0, buf.size) ?: break
                if (n > 0) onChunk(if (n == buf.size) buf.copyOf() else buf.copyOf(n))
            }
        }
        BuddyLog.d("Live.mic", "start chunk=$chunk")
        return true
    }

    fun stop() {
        running = false
        val current = record ?: return
        record = null
        try { current.stop() } catch (_: Exception) { }
        current.release()
        BuddyLog.d("Live.mic", "stop")
    }
}
