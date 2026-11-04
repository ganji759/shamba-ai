package ai.shamba.app.ui

import ai.shamba.app.R
import ai.shamba.app.data.ScanRecord
import ai.shamba.app.knowledge.Condition
import ai.shamba.app.knowledge.KnowledgeBase
import ai.shamba.app.knowledge.Language
import ai.shamba.app.knowledge.Urgency
import ai.shamba.app.ml.Certainty
import ai.shamba.app.util.formatDate
import ai.shamba.app.util.formatPercent
import ai.shamba.app.util.withLanguage
import android.content.Context
import androidx.annotation.StringRes

@StringRes
fun Certainty.labelRes(): Int = when (this) {
    Certainty.CONFIDENT -> R.string.certainty_confident
    Certainty.LIKELY -> R.string.certainty_likely
    Certainty.UNCERTAIN -> R.string.certainty_uncertain
}

@StringRes
fun Urgency.labelRes(): Int = when (this) {
    Urgency.CRITICAL -> R.string.urgency_critical
    Urgency.HIGH -> R.string.urgency_high
    Urgency.MEDIUM -> R.string.urgency_medium
    Urgency.NONE -> R.string.urgency_none
}

@StringRes
fun conditionTypeRes(type: String): Int? = when (type) {
    "fungus" -> R.string.type_fungus
    "bacteria" -> R.string.type_bacteria
    "virus" -> R.string.type_virus
    "pest" -> R.string.type_pest
    "healthy" -> R.string.type_healthy
    else -> null
}

/** Builds the share text and the read-aloud text. Strings come from [language] resources. */
object ResultText {
    fun conditionName(kb: KnowledgeBase, classId: String, language: Language, ctx: Context): String =
        kb.condition(classId)?.name?.get(language)?.takeIf { it.isNotBlank() } ?: ctx.getString(R.string.unknown_condition)

    /** Short plain text for WhatsApp or SMS to an extension officer. */
    fun share(context: Context, language: Language, kb: KnowledgeBase, record: ScanRecord): String {
        val ctx = context.withLanguage(language)
        val condition = kb.condition(record.top.classId)
        val name = conditionName(kb, record.top.classId, language, ctx)
        val lines = mutableListOf<String>()
        lines += "*${ctx.getString(R.string.share_header)}*"
        condition?.let { lines += ctx.getString(R.string.crop_label, kb.cropName(it.crop, language)) }
        lines += ctx.getString(
            R.string.share_result,
            name,
            ctx.getString(record.certainty.labelRes()),
            formatPercent(record.top.probability, language),
        )
        if (record.certainty == Certainty.UNCERTAIN) {
            lines += ctx.getString(R.string.share_uncertain)
        } else if (condition != null) {
            lines += ctx.getString(R.string.share_urgency, ctx.getString(condition.urgency.labelRes()))
            condition.scientific?.let { lines += it }
        }
        if (record.others.isNotEmpty()) {
            val others = record.others.joinToString(", ") {
                "${conditionName(kb, it.classId, language, ctx)} ${formatPercent(it.probability, language)}"
            }
            lines += ctx.getString(R.string.share_others, others)
        }
        lines += ctx.getString(R.string.share_date, formatDate(record.timeMillis, language))
        lines += ""
        lines += ctx.getString(R.string.share_model, record.modelVersion)
        lines += kb.ui.disclaimer.get(language)
        return lines.joinToString("\n")
    }

    /** Text for TextToSpeech. Numbers are spoken as "1.", "2." so the steps stay clear. */
    fun speech(context: Context, language: Language, kb: KnowledgeBase, record: ScanRecord): String {
        val ctx = context.withLanguage(language)
        val certainty = ctx.getString(record.certainty.labelRes())
        if (record.certainty == Certainty.UNCERTAIN) {
            return listOf(certainty + ".", kb.ui.retakeTips.get(language)).joinToString(" ")
        }
        val condition = kb.condition(record.top.classId)
        val name = conditionName(kb, record.top.classId, language, ctx)
        return speechFor(ctx, language, kb, condition, header = ctx.getString(R.string.speech_result, name, certainty))
    }

    /** Read-aloud text for a library entry. */
    fun speech(context: Context, language: Language, kb: KnowledgeBase, condition: Condition): String {
        val ctx = context.withLanguage(language)
        return speechFor(ctx, language, kb, condition, header = condition.name.get(language) + ".")
    }

    private fun speechFor(ctx: Context, language: Language, kb: KnowledgeBase, condition: Condition?, header: String): String {
        val parts = mutableListOf(header)
        if (condition != null) {
            parts += ctx.getString(condition.urgency.labelRes()) + "."
            parts += condition.summary.get(language)
            val actions = condition.actions.get(language)
            if (actions.isNotEmpty()) {
                parts += ctx.getString(R.string.section_actions) + "."
                actions.forEachIndexed { i, a -> parts += "${i + 1}. $a" }
            }
            condition.escalate?.let {
                parts += ctx.getString(R.string.section_escalate) + "."
                parts += it.get(language)
            }
            if (!condition.isHealthy) parts += kb.ui.safety.get(language)
        }
        return parts.filter { it.isNotBlank() }.joinToString("\n")
    }
}
