package ai.shamba.app.util

import ai.shamba.app.knowledge.Language
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Android TextToSpeech with a graceful fallback. Many phones have no Kiswahili voice, so [speak]
 * reports [SpeakResult.MISSING_VOICE] instead of reading Kiswahili text with a wrong voice.
 */
class ReadAloud(context: Context) {
    enum class State { STARTING, READY, SPEAKING, UNAVAILABLE }
    enum class SpeakResult { STARTED, MISSING_VOICE, UNAVAILABLE }

    var state by mutableStateOf(State.STARTING)
        private set

    private val main = Handler(Looper.getMainLooper())
    private var lastUtterance: String? = null
    private var tts: TextToSpeech? = null

    init {
        tts = try {
            TextToSpeech(context.applicationContext) { status ->
                main.post { onInit(status) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "No text-to-speech engine", e)
            state = State.UNAVAILABLE
            null
        }
    }

    private fun onInit(status: Int) {
        val engine = tts
        if (status != TextToSpeech.SUCCESS || engine == null) {
            state = State.UNAVAILABLE
            return
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
                main.post { if (utteranceId == lastUtterance && state == State.SPEAKING) state = State.READY }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                main.post { if (state == State.SPEAKING) state = State.READY }
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                main.post { if (state == State.SPEAKING) state = State.READY }
            }
        })
        state = State.READY
    }

    /** Locales to try, best first. */
    private fun candidates(language: Language): List<Locale> = when (language) {
        Language.EN -> listOf("en-KE", "en-GB", "en-US", "en")
        Language.SW -> listOf("sw-KE", "sw-TZ", "sw")
        Language.FR -> listOf("fr-FR", "fr-CA", "fr")
    }.map(Locale::forLanguageTag)

    fun hasVoice(language: Language): Boolean {
        val engine = tts ?: return false
        if (state == State.UNAVAILABLE || state == State.STARTING) return false
        return candidates(language).any { engine.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE }
    }

    fun speak(text: String, language: Language): SpeakResult {
        val engine = tts
        if (engine == null || state == State.UNAVAILABLE || state == State.STARTING) return SpeakResult.UNAVAILABLE
        val locale = candidates(language).firstOrNull {
            engine.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE
        } ?: return SpeakResult.MISSING_VOICE
        val set = engine.setLanguage(locale)
        if (set == TextToSpeech.LANG_MISSING_DATA || set == TextToSpeech.LANG_NOT_SUPPORTED) return SpeakResult.MISSING_VOICE
        engine.setSpeechRate(0.9f)

        // The engine has a size limit per call, so split long text at sentence ends.
        val chunks = chunk(text, TextToSpeech.getMaxSpeechInputLength() - 100)
        chunks.forEachIndexed { i, part ->
            val id = "shamba-$i-${System.nanoTime()}"
            if (i == chunks.lastIndex) lastUtterance = id
            engine.speak(part, if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, id)
        }
        state = State.SPEAKING
        return SpeakResult.STARTED
    }

    fun stop() {
        tts?.stop()
        if (state == State.SPEAKING) state = State.READY
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun chunk(text: String, max: Int): List<String> {
        if (text.length <= max) return listOf(text)
        val out = mutableListOf<String>()
        val current = StringBuilder()
        text.split(Regex("(?<=[.!?\\n])\\s+")).forEach { sentence ->
            if (current.length + sentence.length + 1 > max && current.isNotEmpty()) {
                out += current.toString()
                current.clear()
            }
            current.append(sentence.take(max)).append(' ')
        }
        if (current.isNotBlank()) out += current.toString()
        return out
    }

    private companion object {
        const val TAG = "ReadAloud"
    }
}
