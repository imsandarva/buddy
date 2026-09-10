package com.sandarva.kotlinapps.brain.live

/** Gemini Live (BidiGenerateContent) wire format. Native audio in 16 kHz, out 24 kHz. */
object LiveConfig {
    const val MODEL = "gemini-3.1-flash-live-preview"
    const val WS = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
    const val VOICE = "Orion" // Gemini Live deep male (Aoede was the breezy female)
    const val IN_HZ = 16000
    const val OUT_HZ = 24000
    const val PCM = "audio/pcm"
    /** 20–40 ms is the Live best-practice send size — smaller chunks reach VAD sooner. */
    const val CHUNK_MS = 20
    /** Hold this much PCM before the first write so the track never starts empty. */
    const val PREROLL_MS = 60
    /** Hardware stream buffer — enough to absorb jitter without sitting on the first word. */
    const val TRACK_BUFFER_MS = 160
    /** Mic send backlog. Drop oldest if the socket is busy so we do not send sped-up speech. */
    const val MIC_QUEUE_CHUNKS = 6
    /** Server VAD: how long silence must last before the turn is committed. Larger = later replies. */
    const val VAD_SILENCE_MS = 220
    /** Look-back so the first syllable is not clipped when speech starts. */
    const val VAD_PREFIX_MS = 20
}
