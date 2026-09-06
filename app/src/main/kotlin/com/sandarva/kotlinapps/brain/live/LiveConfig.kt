package com.sandarva.kotlinapps.brain.live

/** Gemini Live (BidiGenerateContent) wire format. Native audio in 16 kHz, out 24 kHz. */
object LiveConfig {
    const val MODEL = "gemini-2.5-flash-native-audio-preview-12-2025"
    const val WS = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
    const val VOICE = "Aoede"
    const val IN_HZ = 16000
    const val OUT_HZ = 24000
    const val PCM = "audio/pcm"
    const val CHUNK_MS = 40
    /** Hold this much PCM before the first write so the track never starts empty. */
    const val PREROLL_MS = 120
    /** Hardware stream buffer — large enough to absorb jitter, not so large it feels late. */
    const val TRACK_BUFFER_MS = 280
    /** Mic send backlog. Drop oldest if the socket is busy so we do not send sped-up speech. */
    const val MIC_QUEUE_CHUNKS = 6
}
