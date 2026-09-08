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
import com.sandarva.kotlinapps.debug.BuddyLog
import java.util.Locale

/** On-device speak and listen. Gemini never hears raw audio in this path. */
class BuddyVoice(private val context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var ready = false
    private var recognizer: SpeechRecognizer? = null
    private var onHeard: ((String) -> Unit)? = null
    private var onListenFailed: ((String) -> Unit)? = null
    @Volatile private var accepting = false
    private var speakGen = 0

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            BuddyLog.d("Voice.ttsInit", "ready=$ready status=$status")
            if (ready) tts?.language = Locale.getDefault()
        }
    }

    fun isSpeaking(): Boolean = ready && tts?.isSpeaking == true

    fun speak(text: String, then: (() -> Unit)? = null) {
        val engine = tts
        BuddyLog.d("Voice.speak", "ready=$ready text=\"${text.take(80)}\" then=${then != null}")
        if (!ready || engine == null) { then?.invoke(); return }
        val gen = ++speakGen
        if (then == null) {
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "buddy-say")
            return
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = Unit
            override fun onDone(id: String?) {
                main.post {
                    if (gen != speakGen) BuddyLog.d("Voice.speakDone", "ignored stale gen=$gen current=$speakGen")
                    else then()
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                main.post { if (gen == speakGen) then() }
            }
        })
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "buddy-say")
    }

    fun listen(onText: (String) -> Unit, onFailed: (String) -> Unit) {
        accepting = true
        onHeard = onText
        onListenFailed = onFailed
        BuddyLog.d("Voice.listen", "available=${SpeechRecognizer.isRecognitionAvailable(context)}")
        main.post {
            if (!accepting) return@post
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                if (accepting) onFailed("I couldn’t hear you on this phone. Type it instead.")
                return@post
            }
            val next = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
            next.setRecognitionListener(listener)
            next.startListening(listenIntent())
        }
    }

    fun cancelListen() {
        BuddyLog.d("Voice.cancelListen", "acceptingWas=$accepting")
        accepting = false
        onHeard = null
        onListenFailed = null
        main.post { recognizer?.cancel() }
    }

    fun cancelAll() {
        BuddyLog.d("Voice.cancelAll", "acceptingWas=$accepting")
        cancelListen()
        speakGen += 1
        main.post { tts?.stop() }
    }

    fun release() {
        cancelAll()
        main.post {
            recognizer?.destroy()
            recognizer = null
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
        override fun onReadyForSpeech(params: Bundle?) = BuddyLog.d("Voice.stt", "readyForSpeech")
        override fun onBeginningOfSpeech() = BuddyLog.d("Voice.stt", "beginning")
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = BuddyLog.d("Voice.stt", "endOfSpeech")
        override fun onError(error: Int) {
            BuddyLog.d("Voice.sttError", "code=$error accepting=$accepting")
            if (!accepting) return
            accepting = false
            onListenFailed?.invoke("I missed that. Try again, or type it.")
        }
        override fun onResults(results: Bundle?) {
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty()
            BuddyLog.d("Voice.sttResults", "accepting=$accepting text=\"${text.take(80)}\"")
            if (!accepting) return
            accepting = false
            if (text.isBlank()) onListenFailed?.invoke("I missed that. Try again, or type it.")
            else onHeard?.invoke(text)
        }
        override fun onPartialResults(partialResults: Bundle?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }
}
