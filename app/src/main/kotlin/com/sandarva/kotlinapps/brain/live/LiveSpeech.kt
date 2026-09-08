package com.sandarva.kotlinapps.brain.live

import kotlin.math.abs

/**
 * Hangover energy gate on 16-bit PCM. AEC on `VOICE_COMMUNICATION` dips below a hard floor
 * between syllables — resetting the streak on every quiet chunk never reached “heard”.
 */
class LiveSpeech {
    private var loudHits = 0
    private var quietHits = 0

    fun reset() { loudHits = 0; quietHits = 0 }

    fun feed(pcm: ByteArray): Boolean {
        if (loud(pcm)) {
            loudHits += 1
            quietHits = 0
        } else if (loudHits > 0) {
            quietHits += 1
            if (quietHits > HANGOVER) { loudHits = 0; quietHits = 0 }
        }
        return loudHits >= NEED
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
        /** ~1.2% of full scale — AEC-attenuated speech still counts; room hiss usually does not. */
        private const val FLOOR = 400
        private const val NEED = 4
        /** Keep the streak through ~160 ms of AEC holes. */
        private const val HANGOVER = 8
    }
}
