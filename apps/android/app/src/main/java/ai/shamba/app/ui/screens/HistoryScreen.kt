package ai.shamba.app.ui.screens

import ai.shamba.app.R
import ai.shamba.app.data.ScanRecord
import ai.shamba.app.ml.Certainty
import ai.shamba.app.ui.AppContent
import ai.shamba.app.ui.LocalLanguage
import ai.shamba.app.ui.ResultText
import ai.shamba.app.ui.ShambaScaffold
import ai.shamba.app.ui.components.ShambaIcon
import ai.shamba.app.ui.components.UrgencyBadge
import ai.shamba.app.ui.components.rememberFileImage
import ai.shamba.app.ui.labelRes
import ai.shamba.app.ui.theme.ShambaTheme
import ai.shamba.app.util.formatDate
import ai.shamba.app.util.formatPercent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun HistoryScreen(
    records: List<ScanRecord>,
    content: AppContent?,
    thumbFor: (ScanRecord) -> File,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
    tabs: @Composable () -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    ShambaScaffold(title = stringResource(R.string.history_title), tabs = tabs) { padding ->
        if (records.isEmpty() || content == null) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ShambaIcon(R.drawable.ic_history, null, Modifier.size(64.dp), tint = ShambaTheme.colors.muted)
                    Text(
                        stringResource(R.string.history_empty),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = ShambaTheme.colors.muted,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
            return@ShambaScaffold
        }
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(records, key = { it.id }) { record ->
                    HistoryRow(record, content, thumbFor(record), onOpen = { onOpen(record.id) }, onDelete = { pendingDelete = record.id })
                }
            }
        }
    }
    pendingDelete?.let { id ->
        DeleteDialog(onConfirm = { pendingDelete = null; onDelete(id) }, onDismiss = { pendingDelete = null })
    }
}

@Composable
private fun HistoryRow(record: ScanRecord, content: AppContent, thumb: File, onOpen: () -> Unit, onDelete: () -> Unit) {
    val language = LocalLanguage.current
    val context = LocalContext.current
    val kb = content.knowledge
    val condition = kb.condition(record.top.classId)
    val uncertain = record.certainty == Certainty.UNCERTAIN
    val image = rememberFileImage(thumb, maxSide = 256)
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
    ) {
        Row(Modifier.padding(12.dp).heightIn(min = 72.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(76.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                image?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (uncertain) stringResource(R.string.not_sure_title) else ResultText.conditionName(kb, record.top.classId, language, context),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${stringResource(record.certainty.labelRes())} · ${formatPercent(record.top.probability, language)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ShambaTheme.colors.muted,
                )
                Text(formatDate(record.timeMillis, language), style = MaterialTheme.typography.bodySmall, color = ShambaTheme.colors.muted)
                if (!uncertain && condition != null) UrgencyBadge(condition.urgency, large = false)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(56.dp)) {
                ShambaIcon(R.drawable.ic_delete, stringResource(R.string.delete), Modifier.size(26.dp), tint = ShambaTheme.colors.muted)
            }
        }
    }
}
