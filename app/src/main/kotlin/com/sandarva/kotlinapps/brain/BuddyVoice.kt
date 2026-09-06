package com.sandarva.kotlinapps.brain

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** On-device speak and listen. Gemini never hears raw audio in this path. */
class BuddyVoice(private val context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var ready = false
    private var recognizer: SpeechRecognizer? = null
    private var onHeard: ((String) -> Unit)? = null
    private var onListenFailed: ((String) -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) tts?.language = Locale.getDefault()
        }
    }

    fun speak(text: String, then: (() -> Unit)? = null) {
        val engine = tts
        if (!ready || engine == null) { then?.invoke(); return }
        if (then == null) {
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "buddy-say")
            return
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = Unit
            override fun onDone(id: String?) { main.post(then) }
            @Deprecated("Deprecated in Java") override fun onError(id: String?) { main.post(then) }
        })
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "buddy-say")
    }

    fun listen(onText: (String) -> Unit, onFailed: (String) -> Unit) {
        onHeard = onText
        onListenFailed = onFailed
        main.post {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                onFailed("I couldn’t hear you on this phone. Type it instead.")
                return@post
            }
            val next = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
            next.setRecognitionListener(listener)
            next.startListening(listenIntent())
        }
    }

    fun stopListening() {
        main.post { recognizer?.stopListening() }
    }

    fun release() {
        main.post {
            recognizer?.destroy()
            recognizer = null
            tts?.stop()
            tts?.shutdown()
            tts = null
        }
    }

    private fun listenIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onError(error: Int) {
            onListenFailed?.invoke("I missed that. Try again, or type it.")
        }
        override fun onResults(results: Bundle?) {
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty()
            if (text.isBlank()) onListenFailed?.invoke("I missed that. Try again, or type it.")
            else onHeard?.invoke(text)
        }
        override fun onPartialResults(partialResults: Bundle?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }
}
