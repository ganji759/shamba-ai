package ai.shamba.app.ui.screens

import ai.shamba.app.R
import ai.shamba.app.knowledge.Language
import ai.shamba.app.ui.components.ShambaIcon
import ai.shamba.app.util.withLanguage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** First run: pick the language. The title shows in all three languages. */
@Composable
fun LanguageScreen(onPick: (Language) -> Unit) {
    val context = LocalContext.current
    val titles = remember(context) {
        Language.entries.map { context.withLanguage(it).getString(R.string.language_title) }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                ShambaIcon(R.drawable.ic_leaf, null, Modifier.size(60.dp), tint = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(24.dp))
            titles.distinct().forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(Modifier.height(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                Language.entries.forEach { language ->
                    Button(
                        onClick = { onPick(language) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(true).copy(width = 2.dp),
                    ) {
                        Text(language.nativeName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
