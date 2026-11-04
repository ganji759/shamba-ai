package ai.shamba.app.ui.screens

import ai.shamba.app.R
import ai.shamba.app.data.ScanRecord
import ai.shamba.app.knowledge.Condition
import ai.shamba.app.knowledge.KnowledgeBase
import ai.shamba.app.knowledge.Language
import ai.shamba.app.ml.Certainty
import ai.shamba.app.ui.AppContent
import ai.shamba.app.ui.LocalLanguage
import ai.shamba.app.ui.LocalSnackbar
import ai.shamba.app.ui.ResultText
import ai.shamba.app.ui.ShambaScaffold
import ai.shamba.app.ui.components.CertaintyMeter
import ai.shamba.app.ui.components.NumberedList
import ai.shamba.app.ui.components.SectionCard
import ai.shamba.app.ui.components.ShambaButton
import ai.shamba.app.ui.components.ShambaIcon
import ai.shamba.app.ui.components.ShambaOutlinedButton
import ai.shamba.app.ui.components.UrgencyBadge
import ai.shamba.app.ui.components.rememberFileImage
import ai.shamba.app.ui.conditionTypeRes
import ai.shamba.app.ui.theme.ShambaTheme
import ai.shamba.app.util.ReadAloud
import ai.shamba.app.util.formatDate
import ai.shamba.app.util.formatPercent
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ResultScreen(
    record: ScanRecord?,
    content: AppContent?,
    photoFileFor: (ScanRecord) -> File,
    onBack: () -> Unit,
    onRetake: () -> Unit,
    onDelete: () -> Unit,
) {
    val language = LocalLanguage.current
    val context = LocalContext.current
    val readAloud = rememberReadAloud()
    var confirmDelete by remember { mutableStateOf(false) }

    ShambaScaffold(
        title = stringResource(R.string.result_title),
        onBack = onBack,
        actions = {
            if (record != null) {
                IconButton(onClick = { confirmDelete = true }, modifier = Modifier.size(56.dp)) {
                    ShambaIcon(R.drawable.ic_delete, stringResource(R.string.delete), Modifier.size(26.dp))
                }
            }
        },
    ) { padding ->
        if (record == null || content == null) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@ShambaScaffold
        }
        val kb = content.knowledge
        val condition = kb.condition(record.top.classId)
        val uncertain = record.certainty == Certainty.UNCERTAIN

        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val photo = rememberFileImage(photoFileFor(record))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    photo?.let {
                        Image(it, stringResource(R.string.photo_description), Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }

                // Header: crop, name, certainty, urgency.
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (uncertain) {
                        Text(stringResource(R.string.not_sure_title), style = MaterialTheme.typography.headlineLarge)
                    } else {
                        condition?.let {
                            Text(
                                stringResource(R.string.crop_label, kb.cropName(it.crop, language)),
                                style = MaterialTheme.typography.titleMedium,
                                color = ShambaTheme.colors.soil,
                            )
                        }
                        Text(
                            ResultText.conditionName(kb, record.top.classId, language, context),
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        condition?.scientific?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = ShambaTheme.colors.muted)
                        }
                    }
                    CertaintyMeter(record.certainty, record.top.probability, formatPercent(record.top.probability, language))
                    if (!uncertain && condition != null) UrgencyBadge(condition.urgency)
                    Text(formatDate(record.timeMillis, language), style = MaterialTheme.typography.bodyMedium, color = ShambaTheme.colors.muted)
                }

                ActionRow(
                    readAloud = readAloud,
                    onRead = { lang -> ResultText.speech(context, lang, kb, record) },
                    onShare = { shareText(context, ResultText.share(context, language, kb, record)) },
                )

                if (uncertain) {
                    SectionCard(stringResource(R.string.section_retake), R.drawable.ic_camera, accent = ShambaTheme.colors.medium) {
                        Text(kb.ui.retakeTips.get(language), style = MaterialTheme.typography.bodyLarge)
                        ShambaButton(stringResource(R.string.retake_button), onRetake, Modifier.fillMaxWidth(), icon = R.drawable.ic_refresh)
                    }
                    PossibilitiesCard(stringResource(R.string.might_be), record.ranked, kb, language, context)
                } else {
                    if (condition != null) AdviceSections(condition, kb, language)
                    if (record.others.isNotEmpty()) {
                        PossibilitiesCard(stringResource(R.string.section_other), record.others, kb, language, context)
                    }
                    ShambaOutlinedButton(stringResource(R.string.retake_button), onRetake, Modifier.fillMaxWidth(), icon = R.drawable.ic_refresh)
                }

                Disclaimer(kb.ui.disclaimer.get(language))
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (confirmDelete) {
        DeleteDialog(onConfirm = { confirmDelete = false; onDelete() }, onDismiss = { confirmDelete = false })
    }
}

/** Library detail: the same advice without a photo. */
@Composable
fun DiseaseScreen(classId: String, content: AppContent?, onBack: () -> Unit) {
    val language = LocalLanguage.current
    val context = LocalContext.current
    val readAloud = rememberReadAloud()
    val condition = content?.knowledge?.condition(classId)
    ShambaScaffold(title = condition?.name?.get(language) ?: "", onBack = onBack) { padding ->
        if (content == null || condition == null) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@ShambaScaffold
        }
        val kb = content.knowledge
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.crop_label, kb.cropName(condition.crop, language)),
                        style = MaterialTheme.typography.titleMedium,
                        color = ShambaTheme.colors.soil,
                    )
                    Text(condition.name.get(language), style = MaterialTheme.typography.headlineLarge)
                    val type = conditionTypeRes(condition.type)?.let { stringResource(it) }
                    val subtitle = listOfNotNull(type, condition.scientific).joinToString(" · ")
                    if (subtitle.isNotBlank()) {
                        Text(subtitle, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = ShambaTheme.colors.muted)
                    }
                    UrgencyBadge(condition.urgency)
                }
                ActionRow(
                    readAloud = readAloud,
                    onRead = { lang -> ResultText.speech(context, lang, kb, condition) },
                    onShare = null,
                )
                AdviceSections(condition, kb, language)
                Disclaimer(kb.ui.disclaimer.get(language))
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ColumnScope.AdviceSections(condition: Condition, kb: KnowledgeBase, language: Language) {
    val colors = ShambaTheme.colors
    Text(condition.summary.get(language), style = MaterialTheme.typography.bodyLarge)
    val actions = condition.actions.get(language)
    if (actions.isNotEmpty()) {
        SectionCard(stringResource(R.string.section_actions), R.drawable.ic_check_circle) { NumberedList(actions) }
    }
    val prevention = condition.prevention.get(language)
    if (prevention.isNotEmpty()) {
        SectionCard(stringResource(R.string.section_prevention), R.drawable.ic_shield, accent = colors.soil) { NumberedList(prevention) }
    }
    condition.escalate?.let { escalate ->
        SectionCard(stringResource(R.string.section_escalate), R.drawable.ic_phone, accent = colors.high) {
            Text(escalate.get(language), style = MaterialTheme.typography.bodyLarge)
        }
    }
    if (!condition.isHealthy) {
        SectionCard(
            stringResource(R.string.section_safety),
            R.drawable.ic_warning,
            accent = colors.medium,
            container = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Text(kb.ui.safety.get(language), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun PossibilitiesCard(
    title: String,
    scores: List<ai.shamba.app.ml.ClassScore>,
    kb: KnowledgeBase,
    language: Language,
    context: Context,
) {
    SectionCard(title, R.drawable.ic_leaf, accent = ShambaTheme.colors.muted) {
        scores.forEach { s ->
            Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                val condition = kb.condition(s.classId)
                Column(Modifier.weight(1f)) {
                    Text(ResultText.conditionName(kb, s.classId, language, context), style = MaterialTheme.typography.titleMedium)
                    condition?.let {
                        Text(kb.cropName(it.crop, language), style = MaterialTheme.typography.bodySmall, color = ShambaTheme.colors.muted)
                    }
                }
                Text(formatPercent(s.probability, language), style = MaterialTheme.typography.titleMedium, color = ShambaTheme.colors.muted)
            }
        }
    }
}

@Composable
private fun ActionRow(readAloud: ReadAloud, onRead: (Language) -> String, onShare: (() -> Unit)?) {
    val language = LocalLanguage.current
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val speaking = readAloud.state == ReadAloud.State.SPEAKING
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        ShambaButton(
            text = stringResource(if (speaking) R.string.stop_reading else R.string.read_aloud),
            icon = if (speaking) R.drawable.ic_stop else R.drawable.ic_volume,
            modifier = Modifier.weight(1f),
            enabled = readAloud.state != ReadAloud.State.STARTING,
            onClick = {
                if (speaking) {
                    readAloud.stop()
                    return@ShambaButton
                }
                when (readAloud.speak(onRead(language), language)) {
                    ReadAloud.SpeakResult.STARTED -> Unit
                    ReadAloud.SpeakResult.UNAVAILABLE -> scope.launch {
                        snackbar.showSnackbar(context.getString(R.string.tts_unavailable))
                    }
                    ReadAloud.SpeakResult.MISSING_VOICE -> scope.launch {
                        val message = context.getString(R.string.tts_missing_voice, language.nativeName)
                        // Fallback: offer English, which almost every phone has.
                        if (language != Language.EN && readAloud.hasVoice(Language.EN)) {
                            val result = snackbar.showSnackbar(
                                message = message,
                                actionLabel = context.getString(R.string.tts_read_english),
                                duration = SnackbarDuration.Long,
                            )
                            if (result == SnackbarResult.ActionPerformed) readAloud.speak(onRead(Language.EN), Language.EN)
                        } else {
                            snackbar.showSnackbar(message, duration = SnackbarDuration.Long)
                        }
                    }
                }
            },
        )
        if (onShare != null) {
            ShambaOutlinedButton(
                text = stringResource(R.string.share),
                icon = R.drawable.ic_share,
                modifier = Modifier.weight(1f),
                onClick = onShare,
            )
        }
    }
}

@Composable
private fun Disclaimer(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        ShambaIcon(R.drawable.ic_info, null, Modifier.size(22.dp), tint = ShambaTheme.colors.muted)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun DeleteDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { ShambaIcon(R.drawable.ic_delete, null, Modifier.size(28.dp)) },
        title = { Text(stringResource(R.string.delete_title)) },
        text = { Text(stringResource(R.string.delete_body), style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.delete), color = ShambaTheme.colors.critical, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun rememberReadAloud(): ReadAloud {
    val context = LocalContext.current
    val readAloud = remember { ReadAloud(context) }
    DisposableEffect(readAloud) { onDispose { readAloud.shutdown() } }
    return readAloud
}

private fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, context.getString(R.string.share_chooser)))
}
