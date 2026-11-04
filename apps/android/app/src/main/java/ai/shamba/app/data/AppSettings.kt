package ai.shamba.app.data

import ai.shamba.app.knowledge.Language
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Small key-value settings on the phone. */
class AppSettings(context: Context) {
    private val prefs: SharedPreferences = prefs(context)

    private val _language = MutableStateFlow(Language.fromCode(prefs.getString(KEY_LANGUAGE, null)))
    private val _lastLatencyMs = MutableStateFlow(prefs.getLong(KEY_LAST_LATENCY, -1L).takeIf { it >= 0 })

    /** Null until the farmer picks a language on first run. */
    val language: StateFlow<Language?> = _language.asStateFlow()

    val lastLatencyMs: StateFlow<Long?> = _lastLatencyMs.asStateFlow()

    fun setLanguage(language: Language) {
        // commit() so the value is on disk before the activity restarts in the new language.
        prefs.edit().putString(KEY_LANGUAGE, language.code).commit()
        _language.value = language
    }

    fun setLastLatency(ms: Long) {
        prefs.edit().putLong(KEY_LAST_LATENCY, ms).apply()
        _lastLatencyMs.value = ms
    }

    /** sha256 of the model file on disk, saved after the first copy so it is not re-computed each start. */
    var installedModelSha: String?
        get() = prefs.getString(KEY_MODEL_SHA, null)
        set(value) {
            prefs.edit().putString(KEY_MODEL_SHA, value).apply()
        }

    var installedModelSource: String?
        get() = prefs.getString(KEY_MODEL_SOURCE, null)
        set(value) {
            prefs.edit().putString(KEY_MODEL_SOURCE, value).apply()
        }

    companion object {
        private const val FILE = "shamba_settings"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_LAST_LATENCY = "last_latency_ms"
        private const val KEY_MODEL_SHA = "model_sha256"
        private const val KEY_MODEL_SOURCE = "model_source_sha256"

        private fun prefs(context: Context): SharedPreferences =
            context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

        /** Reads the language without the app container. Used in Activity.attachBaseContext. */
        fun peekLanguage(context: Context): Language? = Language.fromCode(prefs(context).getString(KEY_LANGUAGE, null))
    }
}
