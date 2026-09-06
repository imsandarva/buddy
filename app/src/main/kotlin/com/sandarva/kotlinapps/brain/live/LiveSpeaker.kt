package com.sandarva.kotlinapps.brain.live

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.sandarva.kotlinapps.debug.BuddyLog
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

/** 24 kHz mono PCM16 playback. Flush on barge-in. */
class LiveSpeaker {
    private val queue = LinkedBlockingQueue<ByteArray>(32)
    @Volatile private var track: AudioTrack? = null
    @Volatile private var running = false

    fun start() {
        if (running) return
        val min = AudioTrack.getMinBufferSize(LiveConfig.OUT_HZ, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val next = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(LiveConfig.OUT_HZ).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(min.coerceAtLeast(4096))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track = next
        running = true
        next.play()
        thread(name = "buddy-live-spk", isDaemon = true) {
            while (running) {
                val chunk = try { queue.take() } catch (_: InterruptedException) { break }
                if (!running) break
                if (chunk.isEmpty()) continue
                track?.write(chunk, 0, chunk.size)
            }
        }
        BuddyLog.d("Live.speaker", "start")
    }

    fun play(pcm: ByteArray) {
        if (!running || pcm.isEmpty()) return
        if (!queue.offer(pcm)) {
            queue.poll()
            queue.offer(pcm)
        }
    }

    fun interrupt() {
        queue.clear()
        track?.pause()
        track?.flush()
        track?.play()
        BuddyLog.d("Live.speaker", "interrupt")
    }

    fun stop() {
        running = false
        queue.clear()
        queue.offer(ByteArray(0))
        val current = track ?: return
        track = null
        try { current.pause(); current.flush(); current.stop() } catch (_: Exception) { }
        current.release()
        BuddyLog.d("Live.speaker", "stop")
    }
}
