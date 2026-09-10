package com.sandarva.kotlinapps.brain.live

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import com.sandarva.kotlinapps.debug.BuddyLog
import com.sandarva.kotlinapps.overlay.CursorMoodSignals
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

/**
 * 24 kHz mono PCM16 playback at wall-clock speed.
 * Gemini generates faster than realtime — we buffer and never drop, so speech cannot “speed up”.
 */
class LiveSpeaker {
    private val queue = LinkedBlockingQueue<ByteArray>()
    @Volatile private var track: AudioTrack? = null
    @Volatile private var running = false
    @Volatile private var flush = false
    @Volatile private var drain = false
    @Volatile private var onQuiet: (() -> Unit)? = null

    fun start(sessionId: Int = 0) {
        if (running) return
        val min = AudioTrack.getMinBufferSize(LiveConfig.OUT_HZ, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val bytes = maxOf(min * 4, bytesForMs(LiveConfig.TRACK_BUFFER_MS), 4096)
        val next = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(LiveConfig.OUT_HZ).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(bytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .apply { if (sessionId > 0) setSessionId(sessionId) }
            .build()
        track = next
        running = true
        flush = false
        drain = false
        thread(name = "buddy-live-spk", isDaemon = true) { loop() }
        BuddyLog.d("Live.speaker", "start buf=$bytes session=$sessionId")
    }

    fun play(pcm: ByteArray) {
        if (!running || pcm.isEmpty()) return
        queue.offer(pcm)
    }

    /** Barge-in — only the writer thread flushes the track. */
    fun interrupt() {
        onQuiet = null
        queue.clear()
        flush = true
        drain = false
        queue.offer(WAKE)
        CursorMoodSignals.setVoiceAmplitude(0f)
        BuddyLog.d("Live.speaker", "interrupt")
    }

    /** Play whatever is still prerolling when the model finishes a turn. [then] runs once the buffer is empty. */
    fun endUtterance(then: (() -> Unit)? = null) {
        onQuiet = then
        drain = true
        queue.offer(WAKE)
    }

    fun stop() {
        onQuiet = null
        running = false
        queue.clear()
        queue.offer(WAKE)
        CursorMoodSignals.setVoiceAmplitude(0f)
        val current = track ?: return
        track = null
        try { current.pause(); current.flush(); current.stop() } catch (_: Exception) { }
        current.release()
        BuddyLog.d("Live.speaker", "stop")
    }

    private fun loop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        val preroll = ArrayList<ByteArray>(8)
        var prerollBytes = 0
        var primed = false
        val need = bytesForMs(LiveConfig.PREROLL_MS)
        while (running) {
            val chunk = try { queue.take() } catch (_: InterruptedException) { break }
            if (!running) break
            if (flush) {
                flush = false
                drain = false
                primed = false
                preroll.clear()
                prerollBytes = 0
                pauseAndFlush()
                if (chunk.isEmpty()) continue
            }
            if (chunk.isEmpty()) {
                if (drain && preroll.isNotEmpty()) {
                    primed = writeAll(preroll)
                    preroll.clear()
                    prerollBytes = 0
                }
                drain = false
                CursorMoodSignals.setVoiceAmplitude(0f) // utterance drained — buddy's mouth is quiet again
                val quiet = onQuiet
                onQuiet = null
                quiet?.invoke()
                continue
            }
            if (!primed) {
                preroll += chunk
                prerollBytes += chunk.size
                if (prerollBytes >= need || drain) {
                    primed = writeAll(preroll)
                    preroll.clear()
                    prerollBytes = 0
                    drain = false
                }
                continue
            }
            write(chunk)
        }
    }

    private fun writeAll(chunks: List<ByteArray>): Boolean {
        if (chunks.isEmpty()) return false
        ensurePlaying()
        for (chunk in chunks) {
            if (!running || flush) return false
            write(chunk)
        }
        return true
    }

    private fun write(pcm: ByteArray) {
        val t = track ?: return
        CursorMoodSignals.setVoiceAmplitude(LiveSpeech.amplitude(pcm)) // buddycursor visibly "speaks" its reply
        var off = 0
        while (running && !flush && off < pcm.size) {
            val n = t.write(pcm, off, pcm.size - off, AudioTrack.WRITE_BLOCKING)
            if (n <= 0) break
            off += n
        }
    }

    private fun ensurePlaying() {
        val t = track ?: return
        if (t.playState != AudioTrack.PLAYSTATE_PLAYING) runCatching { t.play() }
    }

    private fun pauseAndFlush() {
        val t = track ?: return
        try {
            t.pause()
            t.flush()
        } catch (_: Exception) { }
    }

    companion object {
        private val WAKE = ByteArray(0)
        private fun bytesForMs(ms: Int): Int = (LiveConfig.OUT_HZ * ms / 1000) * 2
    }
}
