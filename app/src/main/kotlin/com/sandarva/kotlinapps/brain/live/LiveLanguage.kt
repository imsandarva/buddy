package com.sandarva.kotlinapps.brain.live

import com.sandarva.kotlinapps.data.Country

/**
 * Turns the country the user picked into (a) a BCP-47 code for Gemini Live's native-voice
 * `speechConfig.languageCode`, when that language is one of the handful Live natively voices, and
 * (b) a plain-words instruction appended to every system prompt so Buddy leans into that language
 * either way — native voice or not.
 */
object LiveLanguage {
    /** Gemini Live's own documented native-audio language list — everything else still gets the
     *  text instruction below, just without an explicit speechConfig hint. */
    private val NATIVE_VOICE: Map<String, String> = mapOf(
        "arabic" to "ar-EG",
        "bengali" to "bn-BD",
        "dutch" to "nl-NL",
        "english" to "en-US",
        "french" to "fr-FR",
        "german" to "de-DE",
        "hindi" to "hi-IN",
        "indonesian" to "id-ID",
        "italian" to "it-IT",
        "japanese" to "ja-JP",
        "korean" to "ko-KR",
        "marathi" to "mr-IN",
        "polish" to "pl-PL",
        "portuguese" to "pt-BR",
        "romanian" to "ro-RO",
        "russian" to "ru-RU",
        "spanish" to "es-US",
        "tamil" to "ta-IN",
        "telugu" to "te-IN",
        "thai" to "th-TH",
        "turkish" to "tr-TR",
        "ukrainian" to "uk-UA",
        "vietnamese" to "vi-VN"
    )

    /** Null when Live has no native voice for this language — the text instruction still carries it. */
    fun speechCode(language: String): String? = NATIVE_VOICE[language.trim().lowercase()]

    /** A section formatted exactly like [LivePrompt]'s own sections, appended right after it. */
    fun instruction(country: Country): String = buildString {
        appendLine()
        appendLine("################################")
        appendLine("# LANGUAGE")
        appendLine("################################")
        appendLine()
        appendLine(
            """
            They chose to talk with you in ${country.language} — the language of ${country.name}.
            Speak with them mostly in ${country.language}. Your very first hello, and every greeting after that, should be in ${country.language} — keep the same warm, short, casual style, just in that language.
            If they switch to a different language mid-talk, follow their lead and answer in that language instead. Never comment on the switch, and never lecture them about which language they should use.
            """.trimIndent()
        )
    }

    /** Short nudge appended to a one-line greeting turn (see [LiveMessages.hello] / [LiveMessages.meet]). */
    fun greetingNudge(country: Country): String = " Greet them in ${country.language}."
}
