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

    private fun loud(pcm: ByteArray): Boolean = meanAbs(pcm) >= FLOOR

    companion object {
        /** ~1.2% of full scale — AEC-attenuated speech still counts; room hiss usually does not. */
        private const val FLOOR = 400
        private const val NEED = 4
        /** Keep the streak through ~160 ms of AEC holes. */
        private const val HANGOVER = 8
        /** Loud enough that buddycursor's glow should read as "in full voice" (§2 amplitude rings). */
        private const val LOUD_CEILING = 5500f

        private fun meanAbs(pcm: ByteArray): Long {
            if (pcm.size < 4) return 0L
            var sum = 0L
            var n = 0
            var i = 0
            while (i + 1 < pcm.size) {
                val sample = (pcm[i].toInt() and 0xff) or (pcm[i + 1].toInt() shl 24 shr 16)
                sum += abs(sample)
                n++
                i += 2
            }
            return if (n > 0) sum / n else 0L
        }

        /** 0..1 loudness of 16-bit PCM — buddycursor's glow reacts to this, never a canned pulse. */
        fun amplitude(pcm: ByteArray): Float = (meanAbs(pcm) / LOUD_CEILING).coerceIn(0f, 1f)
    }
}
