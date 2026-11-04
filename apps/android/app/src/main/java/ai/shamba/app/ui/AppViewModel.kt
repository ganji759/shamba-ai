package ai.shamba.app.ui

import ai.shamba.app.R
import ai.shamba.app.appContainer
import ai.shamba.app.data.ScanRecord
import ai.shamba.app.image.PhotoProcessor
import ai.shamba.app.knowledge.KnowledgeBase
import ai.shamba.app.knowledge.Language
import ai.shamba.app.ml.CropClassifier
import ai.shamba.app.ml.ModelManifest
import ai.shamba.app.ml.Preprocess
import ai.shamba.app.ml.PredictionLogic
import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Screens. The four tabs are roots; the others open on top of a tab. */
sealed interface Screen {
    data object Scan : Screen
    data object History : Screen
    data object Library : Screen
    data object About : Screen
    data object Camera : Screen
    data class Result(val scanId: String) : Screen
    data class Disease(val classId: String) : Screen
}

val TAB_SCREENS: List<Screen> = listOf(Screen.Scan, Screen.History, Screen.Library, Screen.About)

/** Knowledge base and manifest, both read from assets. */
data class AppContent(val knowledge: KnowledgeBase, val manifest: ModelManifest)

sealed interface ModelState {
    data object Loading : ModelState
    data class Ready(val classifier: CropClassifier) : ModelState
    data class Failed(val message: String) : ModelState
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val container = app.appContainer

    val settings = container.settings
    val history = container.history

    /** Simple back stack. It survives activity re-creation (language change, rotation). */
    val backStack = mutableStateListOf<Screen>(Screen.Scan)

    private val _content = MutableStateFlow<AppContent?>(null)
    val content: StateFlow<AppContent?> = _content.asStateFlow()

    private val _model = MutableStateFlow<ModelState>(ModelState.Loading)
    val model: StateFlow<ModelState> = _model.asStateFlow()

    private val _analyzing = MutableStateFlow(false)
    val analyzing: StateFlow<Boolean> = _analyzing.asStateFlow()

    private val _messages = Channel<Int>(Channel.BUFFERED)

    /** One-shot snackbar messages, as string resource ids. */
    val messages = _messages.receiveAsFlow()

    init {
        viewModelScope.launch {
            try {
                _content.value = AppContent(container.knowledge.await(), container.manifest.await())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Could not read the bundled assets", e)
                _model.value = ModelState.Failed(e.message ?: e.javaClass.simpleName)
            }
        }
        viewModelScope.launch {
            _model.value = try {
                ModelState.Ready(container.classifier.await())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) { // UnsatisfiedLinkError is an Error, not an Exception.
                Log.e(TAG, "Could not load the model", e)
                ModelState.Failed(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    val current: Screen get() = backStack.last()

    fun openTab(tab: Screen) {
        backStack.clear()
        backStack.add(tab)
    }

    fun open(screen: Screen) {
        if (backStack.lastOrNull() != screen) backStack.add(screen)
    }

    /** @return false when there is nothing to go back to, so the system can close the app. */
    fun back(): Boolean {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
            return true
        }
        if (backStack.last() != Screen.Scan) {
            openTab(Screen.Scan)
            return true
        }
        return false
    }

    fun setLanguage(language: Language) = settings.setLanguage(language)

    fun deleteScan(id: String) {
        viewModelScope.launch {
            history.delete(id)
            backStack.removeAll { it is Screen.Result && it.scanId == id }
            if (backStack.isEmpty()) backStack.add(Screen.History)
        }
    }

    fun analyzeCapture(file: File) = analyze(
        source = { file.inputStream() },
        cleanup = { file.delete() },
    )

    fun analyzeUri(uri: Uri) = analyze(
        source = { getApplication<Application>().contentResolver.openInputStream(uri) ?: error("Cannot open $uri") },
        cleanup = {},
    )

    fun showMessage(@StringRes message: Int) {
        _messages.trySend(message)
    }

    private fun analyze(source: PhotoProcessor.StreamSource, cleanup: () -> Unit) {
        if (_analyzing.value) return
        _analyzing.value = true
        viewModelScope.launch {
            try {
                val classifier = container.classifier.await()
                val manifest = container.manifest.await()
                val record = withContext(Dispatchers.Default) { runScan(classifier, manifest, source) }
                history.add(record)
                settings.setLastLatency(record.latencyMs)
                // Show the result in place of the camera, so "back" returns to the Scan tab.
                if (backStack.lastOrNull() == Screen.Camera) backStack.removeAt(backStack.lastIndex)
                backStack.add(Screen.Result(record.id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "Scan failed", e)
                _messages.trySend(R.string.analysis_failed)
            } finally {
                withContext(Dispatchers.IO) { runCatching(cleanup) }
                _analyzing.value = false
            }
        }
    }

    /** Runs on Dispatchers.Default: decode, preprocess, infer, then save small copies. */
    private suspend fun runScan(
        classifier: CropClassifier,
        manifest: ModelManifest,
        source: PhotoProcessor.StreamSource,
    ): ScanRecord {
        val bitmap = PhotoProcessor.decode(source)
        try {
            val input = Preprocess.toChw(PhotoProcessor.pixels(bitmap), bitmap.width, bitmap.height)
            val inference = classifier.run(input)
            val prediction = PredictionLogic.predict(inference.output, manifest.classes, manifest.thresholds)
            val time = System.currentTimeMillis()
            val id = "$time-${UUID.randomUUID().toString().take(8)}"
            val photo = "$id.jpg"
            val thumb = "${id}_t.jpg"
            withContext(Dispatchers.IO) {
                PhotoProcessor.saveCopies(bitmap, File(history.scansDir, photo), File(history.scansDir, thumb))
            }
            return ScanRecord(
                id = id,
                timeMillis = time,
                photoFile = photo,
                thumbFile = thumb,
                ranked = prediction.ranked,
                certainty = prediction.certainty,
                latencyMs = inference.latencyMs,
                modelVersion = manifest.version,
            )
        } finally {
            bitmap.recycle()
        }
    }

    private companion object {
        const val TAG = "AppViewModel"
    }
}
