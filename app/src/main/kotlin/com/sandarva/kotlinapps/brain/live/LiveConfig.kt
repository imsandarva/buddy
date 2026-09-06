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
}
