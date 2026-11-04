package ai.shamba.app.ui.components

import ai.shamba.app.R
import ai.shamba.app.knowledge.Urgency
import ai.shamba.app.ml.Certainty
import ai.shamba.app.ui.labelRes
import ai.shamba.app.ui.theme.ShambaTheme
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Minimum touch target. Every tappable element is at least this tall. */
val TouchTarget: Dp = 48.dp

@Composable
fun ShambaIcon(@DrawableRes id: Int, contentDescription: String?, modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
    Icon(
        painter = painterResource(id),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = if (tint == Color.Unspecified) androidx.compose.material3.LocalContentColor.current else tint,
    )
}

/** The one primary action of the app ("Check a leaf"). Sun yellow with ink text. */
@Composable
fun PrimaryActionButton(text: String, @DrawableRes icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = ShambaTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 80.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primaryAction,
            contentColor = colors.onPrimaryAction,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
    ) {
        ShambaIcon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

/** Regular action. Primary green. */
@Composable
fun ShambaButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, @DrawableRes icon: Int? = null, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 56.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) { ButtonContent(text, icon) }
}

@Composable
fun ShambaOutlinedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, @DrawableRes icon: Int? = null, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 56.dp),
        shape = RoundedCornerShape(16.dp),
        border = ButtonDefaults.outlinedButtonBorder(enabled).copy(width = 2.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) { ButtonContent(text, icon) }
}

@Composable
private fun RowScope.ButtonContent(text: String, @DrawableRes icon: Int?) {
    if (icon != null) {
        ShambaIcon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
    }
    Text(text, style = MaterialTheme.typography.labelLarge)
}

fun urgencyColor(urgency: Urgency, colors: ai.shamba.app.ui.theme.ShambaColors): Color = when (urgency) {
    Urgency.CRITICAL -> colors.critical
    Urgency.HIGH -> colors.high
    Urgency.MEDIUM -> colors.medium
    Urgency.NONE -> colors.healthy
}

@DrawableRes
fun urgencyIcon(urgency: Urgency): Int = when (urgency) {
    Urgency.CRITICAL -> R.drawable.ic_alert
    Urgency.HIGH -> R.drawable.ic_warning
    Urgency.MEDIUM -> R.drawable.ic_eye
    Urgency.NONE -> R.drawable.ic_check_circle
}

/** Urgency with icon AND text, so it reads without color too. */
@Composable
fun UrgencyBadge(urgency: Urgency, modifier: Modifier = Modifier, large: Boolean = true) {
    val colors = ShambaTheme.colors
    val label = stringResource(urgency.labelRes())
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(urgencyColor(urgency, colors))
            .heightIn(min = if (large) 44.dp else 32.dp)
            .padding(horizontal = if (large) 16.dp else 10.dp, vertical = 6.dp)
            .clearAndSetSemantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShambaIcon(urgencyIcon(urgency), null, Modifier.size(if (large) 24.dp else 18.dp), tint = colors.onBadge)
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            color = colors.onBadge,
            style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Confidence in words plus a bar. The words carry the meaning; the percent is secondary. */
@Composable
fun CertaintyMeter(certainty: Certainty, probability: Float, percent: String, modifier: Modifier = Modifier) {
    val colors = ShambaTheme.colors
    val words = stringResource(certainty.labelRes())
    val barColor = when (certainty) {
        Certainty.CONFIDENT -> MaterialTheme.colorScheme.primary
        Certainty.LIKELY -> colors.primaryAction
        Certainty.UNCERTAIN -> colors.muted
    }
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(words, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            Text(percent, style = MaterialTheme.typography.titleMedium, color = colors.muted)
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(probability.coerceIn(0.02f, 1f))
                    .height(14.dp)
                    .clip(CircleShape)
                    .background(barColor),
            )
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    container: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShambaIcon(icon, null, Modifier.size(26.dp), tint = accent)
                Spacer(Modifier.width(10.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
            }
            content()
        }
    }
}

/** Numbered steps with a big number badge. */
@Composable
fun NumberedList(items: List<String>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEachIndexed { i, item ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${i + 1}",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(item, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 2.dp).widthIn(max = 640.dp))
            }
        }
    }
}

/** Decodes a small local image off the main thread. */
@Composable
fun rememberFileImage(file: File?, maxSide: Int = 1024): ImageBitmap? {
    val image by produceState<ImageBitmap?>(initialValue = null, file?.path, maxSide) {
        value = if (file == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.path, bounds)
                    var sample = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
                    BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
                }.getOrNull()
            }
        }
    }
    return image
}
