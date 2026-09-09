package com.sandarva.kotlinapps.brain.live

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import com.sandarva.kotlinapps.debug.BuddyLog
import com.sandarva.kotlinapps.overlay.CursorMoodSignals
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

/** 16 kHz mono PCM16 capture. A sender thread keeps the record loop from blocking on the socket. */
class LiveMic(private val onChunk: (ByteArray) -> Unit) {
    private val outgoing = LinkedBlockingQueue<ByteArray>(LiveConfig.MIC_QUEUE_CHUNKS)
    @Volatile private var record: AudioRecord? = null
    @Volatile private var running = false

    @SuppressLint("MissingPermission")
    fun start(audio: LiveAudio? = null): Boolean {
        if (running) return true
        val min = AudioRecord.getMinBufferSize(LiveConfig.IN_HZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) {
            BuddyLog.e("Live.mic", "bad minBuffer=$min")
            return false
        }
        val chunk = (LiveConfig.IN_HZ * LiveConfig.CHUNK_MS / 1000) * 2
        val buffer = maxOf(min, chunk * 4)
        val next = AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, LiveConfig.IN_HZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buffer)
        if (next.state != AudioRecord.STATE_INITIALIZED) {
            BuddyLog.e("Live.mic", "AudioRecord failed state=${next.state}")
            next.release()
            return false
        }
        record = next
        audio?.attachRecord(next)
        running = true
        next.startRecording()
        thread(name = "buddy-live-mic", isDaemon = true) { capture(chunk) }
        thread(name = "buddy-live-mic-send", isDaemon = true) { send() }
        BuddyLog.d("Live.mic", "start chunk=$chunk")
        return true
    }

    fun stop() {
        running = false
        outgoing.clear()
        outgoing.offer(WAKE)
        CursorMoodSignals.setVoiceAmplitude(0f)
        val current = record ?: return
        record = null
        try { current.stop() } catch (_: Exception) { }
        current.release()
        BuddyLog.d("Live.mic", "stop")
    }

    private fun capture(chunk: Int) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        val buf = ByteArray(chunk)
        while (running) {
            val n = record?.read(buf, 0, buf.size) ?: break
            if (n <= 0) continue
            val pcm = if (n == buf.size) buf.copyOf() else buf.copyOf(n)
            CursorMoodSignals.setVoiceAmplitude(LiveSpeech.amplitude(pcm)) // buddycursor visibly "hears" the person
            if (!outgoing.offer(pcm)) {
                outgoing.poll()
                outgoing.offer(pcm)
            }
        }
    }

    private fun send() {
        while (running) {
            val pcm = try { outgoing.take() } catch (_: InterruptedException) { break }
            if (!running || pcm.isEmpty()) continue
            onChunk(pcm)
        }
    }

    companion object { private val WAKE = ByteArray(0) }
}
