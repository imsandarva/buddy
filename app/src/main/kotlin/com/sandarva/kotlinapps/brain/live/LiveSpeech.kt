package com.sandarva.kotlinapps.brain.live

import kotlin.math.abs

/** Client-side speech gate — room noise is not an order. */
class LiveSpeech {
    private var hits = 0

    fun reset() { hits = 0 }

    fun feed(pcm: ByteArray): Boolean {
        if (loud(pcm)) hits++ else hits = 0
        return hits >= NEED
    }

    private fun loud(pcm: ByteArray): Boolean {
        if (pcm.size < 4) return false
        var sum = 0L
        var n = 0
        var i = 0
        while (i + 1 < pcm.size) {
            val sample = (pcm[i].toInt() and 0xff) or (pcm[i + 1].toInt() shl 24 shr 16)
            sum += abs(sample)
            n++
            i += 2
        }
        return n > 0 && sum / n >= FLOOR
    }

    companion object {
        private const val FLOOR = 1400
        private const val NEED = 6
    }
}
