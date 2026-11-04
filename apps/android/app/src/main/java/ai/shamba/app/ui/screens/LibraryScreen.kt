package ai.shamba.app.ui.screens

import ai.shamba.app.R
import ai.shamba.app.knowledge.Condition
import ai.shamba.app.ui.AppContent
import ai.shamba.app.ui.LocalLanguage
import ai.shamba.app.ui.ShambaScaffold
import ai.shamba.app.ui.components.ShambaIcon
import ai.shamba.app.ui.components.UrgencyBadge
import ai.shamba.app.ui.theme.ShambaTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Browse diseases by crop. All text comes from the knowledge base in the app language. */
@Composable
fun LibraryScreen(content: AppContent?, onOpen: (String) -> Unit, tabs: @Composable () -> Unit) {
    val language = LocalLanguage.current
    ShambaScaffold(title = stringResource(R.string.library_title), tabs = tabs) { padding ->
        val kb = content?.knowledge
        if (kb == null) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@ShambaScaffold
        }
        var crop by rememberSaveable { mutableStateOf(kb.crops.keys.firstOrNull()) }
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    kb.crops.keys.forEach { id ->
                        FilterChip(
                            selected = crop == id,
                            onClick = { crop = id },
                            label = { Text(kb.cropName(id, language), style = MaterialTheme.typography.titleMedium) },
                            modifier = Modifier.heightIn(min = 48.dp),
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            stringResource(R.string.library_intro),
                            style = MaterialTheme.typography.bodyLarge,
                            color = ShambaTheme.colors.muted,
                        )
                    }
                    items(kb.conditionsForCrop(crop ?: ""), key = { it.id }) { condition ->
                        ConditionRow(condition, onClick = { onOpen(condition.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ConditionRow(condition: Condition, onClick: () -> Unit) {
    val language = LocalLanguage.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(16.dp).heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(condition.name.get(language), style = MaterialTheme.typography.titleLarge)
                Text(
                    condition.summary.get(language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ShambaTheme.colors.muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                UrgencyBadge(condition.urgency, large = false)
            }
            Spacer(Modifier.width(8.dp))
            ShambaIcon(R.drawable.ic_chevron_right, null, Modifier.size(28.dp), tint = ShambaTheme.colors.muted)
        }
    }
}
