package ai.shamba.app.ui.screens

import ai.shamba.app.R
import ai.shamba.app.knowledge.Language
import ai.shamba.app.ml.ModelManifest
import ai.shamba.app.ui.AppContent
import ai.shamba.app.ui.LocalLanguage
import ai.shamba.app.ui.ModelState
import ai.shamba.app.ui.ShambaScaffold
import ai.shamba.app.ui.components.SectionCard
import ai.shamba.app.ui.components.ShambaIcon
import ai.shamba.app.ui.theme.MonoStyle
import ai.shamba.app.ui.theme.ShambaTheme
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen(
    content: AppContent?,
    model: ModelState,
    lastLatencyMs: Long?,
    onLanguageChange: (Language) -> Unit,
    tabs: @Composable () -> Unit,
) {
    val language = LocalLanguage.current
    val context = LocalContext.current
    val colors = ShambaTheme.colors
    val appVersion = remember {
        runCatching {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, 0)
            }
            info.versionName
        }.getOrNull() ?: "?"
    }

    ShambaScaffold(title = stringResource(R.string.about_title), tabs = tabs) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SectionCard(stringResource(R.string.about_language), R.drawable.ic_language) {
                    Column(Modifier.selectableGroup()) {
                        Language.entries.forEach { option ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .selectable(
                                        selected = option == language,
                                        onClick = { onLanguageChange(option) },
                                        role = Role.RadioButton,
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = option == language, onClick = null)
                                Spacer(Modifier.width(12.dp))
                                Text(option.nativeName, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }

                SectionCard(stringResource(R.string.about_privacy_title), R.drawable.ic_shield) {
                    Text(stringResource(R.string.about_privacy), style = MaterialTheme.typography.bodyLarge)
                }

                SectionCard(stringResource(R.string.about_model), R.drawable.ic_leaf) {
                    val manifest = content?.manifest
                    val fileInfo = manifest?.file(ModelManifest.MODEL_FILE)
                    InfoRow(stringResource(R.string.about_backend), stringResource(R.string.backend_name))
                    InfoRow(
                        stringResource(R.string.about_runtime),
                        stringResource(R.string.about_runtime_value, stringResource(R.string.executorch_version)),
                    )
                    InfoRow(
                        stringResource(R.string.about_latency),
                        lastLatencyMs?.let { stringResource(R.string.about_latency_value, it.toInt()) }
                            ?: stringResource(R.string.about_latency_none),
                    )
                    HorizontalDivider()
                    if (manifest == null) {
                        Text(stringResource(R.string.about_loading), style = MaterialTheme.typography.bodyLarge)
                    } else {
                        InfoRow(stringResource(R.string.about_version), "${manifest.name} ${manifest.version}")
                        InfoRow(stringResource(R.string.about_file), ModelManifest.MODEL_FILE, mono = true)
                        fileInfo?.bytes?.let { InfoRow(stringResource(R.string.about_size), Formatter.formatShortFileSize(context, it)) }
                        InfoRow(stringResource(R.string.about_classes), manifest.classes.size.toString())
                    }
                    val sha = (model as? ModelState.Ready)?.classifier?.sha256 ?: fileInfo?.sha256
                    if (sha != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.about_sha), style = MaterialTheme.typography.titleSmall, color = colors.muted)
                            SelectionContainer { Text(sha, style = MonoStyle) }
                            if (model is ModelState.Ready && fileInfo?.sha256 != null) {
                                val ok = fileInfo.sha256.equals(model.classifier.sha256, ignoreCase = true)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ShambaIcon(
                                        if (ok) R.drawable.ic_check_circle else R.drawable.ic_alert,
                                        null,
                                        Modifier.size(20.dp),
                                        tint = if (ok) colors.healthy else colors.critical,
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        stringResource(if (ok) R.string.about_sha_ok else R.string.about_sha_bad),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (ok) colors.healthy else colors.critical,
                                    )
                                }
                            }
                        }
                    }
                    if (model is ModelState.Failed) {
                        Text(model.message, style = MonoStyle, color = colors.critical)
                    }
                }

                content?.knowledge?.let { kb ->
                    SectionCard(stringResource(R.string.about_advice), R.drawable.ic_library, accent = colors.soil) {
                        InfoRow(stringResource(R.string.about_version), stringResource(R.string.about_advice_value, kb.version))
                        if (kb.reviewStatus == "draft") {
                            Text(stringResource(R.string.about_advice_draft), style = MaterialTheme.typography.bodyLarge, color = colors.medium)
                        }
                    }
                }

                InfoRow(stringResource(R.string.about_app_version), "${stringResource(R.string.app_name)} $appVersion")
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, mono: Boolean = false) {
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = ShambaTheme.colors.muted)
        Text(value, style = if (mono) MonoStyle else MaterialTheme.typography.bodyLarge)
    }
}
