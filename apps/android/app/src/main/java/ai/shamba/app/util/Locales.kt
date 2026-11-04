package ai.shamba.app.util

import ai.shamba.app.knowledge.Language
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import kotlin.math.floor

val Language.locale: Locale get() = Locale.forLanguageTag(code)

/** A context whose resources use [language]. The app language does not follow the phone language. */
fun Context.withLanguage(language: Language): Context {
    val config = Configuration(resources.configuration)
    config.setLocales(LocaleList(language.locale))
    return createConfigurationContext(config)
}

fun formatPercent(probability: Float, language: Language): String {
    val format = NumberFormat.getPercentInstance(language.locale)
    format.maximumFractionDigits = 0
    // Round down, so 0.695 shows as "69%" next to "Likely" (the 0.70 threshold) and never as "70%".
    val whole = floor(probability.coerceIn(0f, 1f) * 100.0 + 1e-4)
    return format.format(whole / 100.0)
}

fun formatDate(timeMillis: Long, language: Language): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, language.locale).format(Date(timeMillis))
