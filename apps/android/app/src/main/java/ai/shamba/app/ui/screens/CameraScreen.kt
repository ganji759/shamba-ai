package ai.shamba.app.ui.screens

import ai.shamba.app.R
import ai.shamba.app.ml.Preprocess
import ai.shamba.app.ui.LocalSnackbar
import ai.shamba.app.ui.components.ShambaButton
import ai.shamba.app.ui.components.ShambaIcon
import ai.shamba.app.ui.components.ShambaOutlinedButton
import ai.shamba.app.ui.theme.ShambaTheme
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeoSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

@Composable
fun CameraScreen(
    onCaptured: (File) -> Unit,
    onPickGallery: () -> Unit,
    onClose: () -> Unit,
    onError: (Int) -> Unit,
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var asked by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        asked = true
    }
    LaunchedEffect(Unit) {
        if (!granted && !asked) permission.launch(Manifest.permission.CAMERA)
    }
    // The farmer may allow the camera in phone settings and come back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    val activity = context as? Activity
    val allow = {
        val canAsk = activity?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ?: true
        if (asked && !canAsk) {
            // "Don't ask again" was chosen: only the settings page can grant it now.
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } else {
            permission.launch(Manifest.permission.CAMERA)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (granted) {
            CameraContent(onCaptured, onPickGallery, onClose, onError)
        } else {
            PermissionNeeded(
                onAllow = allow,
                onPickGallery = onPickGallery,
                onClose = onClose,
            )
        }
        SnackbarHost(LocalSnackbar.current, Modifier.align(Alignment.BottomCenter).systemBarsPadding())
    }
}

@Composable
private fun CameraContent(
    onCaptured: (File) -> Unit,
    onPickGallery: () -> Unit,
    onClose: () -> Unit,
    onError: (Int) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var capturing by remember { mutableStateOf(false) }

    // 4:3 at about 1600x1200: sharp enough for the 224 px model, light for low-end phones.
    val selector = remember {
        ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(
                ResolutionStrategy(Size(1600, 1200), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER),
            )
            .build()
    }
    val previewView = remember {
        PreviewView(context).apply {
            // FIT_CENTER shows the whole frame, so the guide square matches what the model sees.
            scaleType = PreviewView.ScaleType.FIT_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setResolutionSelector(selector)
            .build()
    }

    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        future.addListener({
            try {
                val p = future.get()
                provider = p
                val preview = Preview.Builder().setResolutionSelector(selector).build()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                p.unbindAll()
                p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch (e: Exception) {
                Log.e("CameraScreen", "Camera bind failed", e)
                onError(R.string.camera_failed)
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose { provider?.unbindAll() }
    }

    val takePhoto = {
        if (!capturing) {
            capturing = true
            val file = File(context.cacheDir, "capture-${System.currentTimeMillis()}.jpg")
            imageCapture.takePicture(
                ImageCapture.OutputFileOptions.Builder(file).build(),
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        capturing = false
                        onCaptured(file)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e("CameraScreen", "Capture failed", exception)
                        capturing = false
                        file.delete()
                        onError(R.string.capture_failed)
                    }
                },
            )
        }
    }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose, modifier = Modifier.size(56.dp)) {
                ShambaIcon(R.drawable.ic_close, stringResource(R.string.camera_close), Modifier.size(30.dp), tint = Color.White)
            }
            Spacer(Modifier.width(4.dp))
            Text(
                stringResource(R.string.scan_tip),
                color = ShambaTheme.colors.onPrimaryAction,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(ShambaTheme.colors.primaryAction)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.aspectRatio(3f / 4f)) {
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                FramingGuide(Modifier.fillMaxSize())
                Text(
                    stringResource(R.string.camera_guide),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedButton(
                onClick = onPickGallery,
                modifier = Modifier.height(56.dp).widthIn(min = 96.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = ButtonDefaults.outlinedButtonBorder(true).copy(width = 2.dp, brush = androidx.compose.ui.graphics.SolidColor(Color.White)),
            ) {
                ShambaIcon(R.drawable.ic_gallery, null, Modifier.size(22.dp), tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.camera_gallery), style = MaterialTheme.typography.labelLarge)
            }
            ShutterButton(enabled = !capturing, onClick = takePhoto)
            Spacer(Modifier.width(96.dp))
        }
    }
}

@Composable
private fun ShutterButton(enabled: Boolean, onClick: () -> Unit) {
    val label = stringResource(R.string.camera_take_photo)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(84.dp)
                .clip(CircleShape)
                .border(4.dp, Color.White, CircleShape)
                .padding(8.dp)
                .clip(CircleShape)
                .background(if (enabled) MaterialTheme.colorScheme.primary else Color.Gray)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            ShambaIcon(R.drawable.ic_camera, null, Modifier.size(34.dp), tint = Color.White)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Square guide over the preview. Its side is 224/256 of the short side, the same center square
 * that [Preprocess] gives the model. Outside the square is dimmed.
 */
@Composable
private fun FramingGuide(modifier: Modifier) {
    val accent = ShambaTheme.colors.primaryAction
    Canvas(modifier) {
        val side = minOf(size.width, size.height) * Preprocess.SIZE / Preprocess.RESIZE
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        val dim = Color.Black.copy(alpha = 0.45f)
        drawRect(dim, Offset.Zero, GeoSize(size.width, top))
        drawRect(dim, Offset(0f, top + side), GeoSize(size.width, size.height - top - side))
        drawRect(dim, Offset(0f, top), GeoSize(left, side))
        drawRect(dim, Offset(left + side, top), GeoSize(size.width - left - side, side))
        drawRoundRect(
            color = accent,
            topLeft = Offset(left, top),
            size = GeoSize(side, side),
            cornerRadius = CornerRadius(24.dp.toPx()),
            style = Stroke(width = 4.dp.toPx()),
        )
    }
}

@Composable
private fun PermissionNeeded(onAllow: () -> Unit, onPickGallery: () -> Unit, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().systemBarsPadding().padding(20.dp), contentAlignment = Alignment.Center) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.widthIn(max = 520.dp),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ShambaIcon(R.drawable.ic_camera, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.camera_permission_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.camera_permission_body), style = MaterialTheme.typography.bodyLarge)
                ShambaButton(stringResource(R.string.camera_allow), onAllow, Modifier.fillMaxWidth(), icon = R.drawable.ic_camera)
                ShambaOutlinedButton(stringResource(R.string.pick_gallery), onPickGallery, Modifier.fillMaxWidth(), icon = R.drawable.ic_gallery)
                ShambaOutlinedButton(stringResource(R.string.camera_close), onClose, Modifier.fillMaxWidth(), icon = R.drawable.ic_close)
            }
        }
    }
}
