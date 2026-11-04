package ai.shamba.app.ui.screens

import ai.shamba.app.R
import ai.shamba.app.ui.AppContent
import ai.shamba.app.ui.LocalLanguage
import ai.shamba.app.ui.ModelState
import ai.shamba.app.ui.ShambaScaffold
import ai.shamba.app.ui.components.PrimaryActionButton
import ai.shamba.app.ui.components.ShambaIcon
import ai.shamba.app.ui.components.ShambaOutlinedButton
import ai.shamba.app.ui.theme.ShambaTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ScanScreen(
    content: AppContent?,
    model: ModelState,
    onCheckLeaf: () -> Unit,
    onPickGallery: () -> Unit,
    tabs: @Composable () -> Unit,
) {
    val language = LocalLanguage.current
    val colors = ShambaTheme.colors
    ShambaScaffold(title = stringResource(R.string.app_name), tabs = tabs) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(88.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            ShambaIcon(R.drawable.ic_leaf, null, Modifier.size(56.dp), tint = colors.primaryAction)
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            stringResource(R.string.scan_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.scan_intro),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                // The camera may open while the model still loads; the scan waits for it.
                val ready = model !is ModelState.Failed
                PrimaryActionButton(
                    text = stringResource(R.string.check_leaf),
                    icon = R.drawable.ic_camera,
                    onClick = onCheckLeaf,
                    enabled = ready,
                )
                TipRow(stringResource(R.string.scan_tip))
                ShambaOutlinedButton(
                    text = stringResource(R.string.pick_gallery),
                    icon = R.drawable.ic_gallery,
                    onClick = onPickGallery,
                    enabled = ready,
                    modifier = Modifier.fillMaxWidth(),
                )

                when (model) {
                    ModelState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.model_loading), style = MaterialTheme.typography.bodyLarge)
                    }
                    is ModelState.Failed -> Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            ShambaIcon(R.drawable.ic_alert, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                stringResource(R.string.model_failed),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                    is ModelState.Ready -> Unit
                }

                content?.let { c ->
                    val crops = c.knowledge.crops.keys.joinToString(", ") { c.knowledge.cropName(it, language) }
                    Text(
                        stringResource(R.string.scan_crops, crops),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.muted,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShambaIcon(R.drawable.ic_shield, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.scan_offline), style = MaterialTheme.typography.bodyMedium, color = colors.muted)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TipRow(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShambaIcon(R.drawable.ic_info, null, Modifier.size(26.dp), tint = ShambaTheme.colors.soil)
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}
