package ai.shamba.app.ui

import ai.shamba.app.R
import ai.shamba.app.knowledge.Language
import ai.shamba.app.ui.components.ShambaIcon
import ai.shamba.app.ui.screens.AboutScreen
import ai.shamba.app.ui.screens.CameraScreen
import ai.shamba.app.ui.screens.DiseaseScreen
import ai.shamba.app.ui.screens.HistoryScreen
import ai.shamba.app.ui.screens.LanguageScreen
import ai.shamba.app.ui.screens.LibraryScreen
import ai.shamba.app.ui.screens.ResultScreen
import ai.shamba.app.ui.screens.ScanScreen
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Snackbar host shared by all screens. */
val LocalSnackbar = staticCompositionLocalOf { SnackbarHostState() }

/** The app language, chosen on first run. */
val LocalLanguage = staticCompositionLocalOf { Language.DEFAULT }

@Composable
fun ShambaRoot(vm: AppViewModel, onLanguageChange: (Language) -> Unit) {
    val language by vm.settings.language.collectAsStateWithLifecycle()
    val chosen = language
    if (chosen == null) {
        LanguageScreen(onPick = onLanguageChange)
        return
    }

    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(vm) {
        vm.messages.collect { snackbar.showSnackbar(context.getString(it)) }
    }

    val content by vm.content.collectAsStateWithLifecycle()
    val model by vm.model.collectAsStateWithLifecycle()
    val analyzing by vm.analyzing.collectAsStateWithLifecycle()
    val records by vm.history.records.collectAsStateWithLifecycle()

    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.analyzeUri(uri)
    }
    val pickFromGallery = {
        galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    BackHandler(enabled = vm.backStack.size > 1 || vm.current != Screen.Scan) { vm.back() }

    CompositionLocalProvider(LocalSnackbar provides snackbar, LocalLanguage provides chosen) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when (val screen = vm.current) {
                Screen.Scan -> ScanScreen(
                    content = content,
                    model = model,
                    onCheckLeaf = { vm.open(Screen.Camera) },
                    onPickGallery = pickFromGallery,
                    tabs = tabBar(vm),
                )
                Screen.Camera -> CameraScreen(
                    onCaptured = vm::analyzeCapture,
                    onPickGallery = pickFromGallery,
                    onClose = { vm.back() },
                    onError = vm::showMessage,
                )
                is Screen.Result -> ResultScreen(
                    record = records.firstOrNull { it.id == screen.scanId },
                    content = content,
                    photoFileFor = vm.history::photoFile,
                    onBack = { vm.back() },
                    onRetake = {
                        vm.back()
                        vm.open(Screen.Camera)
                    },
                    onDelete = { vm.deleteScan(screen.scanId) },
                )
                Screen.History -> HistoryScreen(
                    records = records,
                    content = content,
                    thumbFor = vm.history::thumbFile,
                    onOpen = { vm.open(Screen.Result(it)) },
                    onDelete = vm::deleteScan,
                    tabs = tabBar(vm),
                )
                Screen.Library -> LibraryScreen(
                    content = content,
                    onOpen = { vm.open(Screen.Disease(it)) },
                    tabs = tabBar(vm),
                )
                is Screen.Disease -> DiseaseScreen(
                    classId = screen.classId,
                    content = content,
                    onBack = { vm.back() },
                )
                Screen.About -> AboutScreen(
                    content = content,
                    model = model,
                    lastLatencyMs = vm.settings.lastLatencyMs.collectAsStateWithLifecycle().value,
                    onLanguageChange = onLanguageChange,
                    tabs = tabBar(vm),
                )
            }
            if (analyzing) AnalyzingOverlay()
        }
    }
}

private data class Tab(val screen: Screen, @DrawableRes val icon: Int, val label: Int)

private val TABS = listOf(
    Tab(Screen.Scan, R.drawable.ic_camera, R.string.tab_scan),
    Tab(Screen.History, R.drawable.ic_history, R.string.tab_history),
    Tab(Screen.Library, R.drawable.ic_library, R.string.tab_library),
    Tab(Screen.About, R.drawable.ic_info, R.string.tab_about),
)

/** Bottom navigation, passed to tab screens as a slot. */
private fun tabBar(vm: AppViewModel): @Composable () -> Unit = {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        TABS.forEach { tab ->
            NavigationBarItem(
                selected = vm.current == tab.screen,
                onClick = { vm.openTab(tab.screen) },
                icon = { ShambaIcon(tab.icon, null, Modifier.size(26.dp)) },
                label = {
                    Text(
                        stringResource(tab.label),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

/** Standard screen frame: large top bar title, optional back button, optional tab bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShambaScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    tabs: (@Composable () -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(LocalSnackbar.current) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() },
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack, modifier = Modifier.size(56.dp)) {
                            ShambaIcon(R.drawable.ic_back, stringResource(R.string.back), Modifier.size(28.dp))
                        }
                    }
                },
                actions = { actions() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = { tabs?.invoke() },
        content = content,
    )
}

@Composable
private fun AnalyzingOverlay() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            // Swallow taps so nothing starts a second scan.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary, strokeWidth = 6.dp, modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.analyzing),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp).semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}
