package ai.shamba.app

import ai.shamba.app.data.AppSettings
import ai.shamba.app.data.HistoryStore
import ai.shamba.app.knowledge.KnowledgeBase
import ai.shamba.app.ml.CropClassifier
import ai.shamba.app.ml.ModelManifest
import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/** App-wide objects. The model loads once per process and stays in memory. */
class AppContainer(private val context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val settings = AppSettings(context)
    val history = HistoryStore(context.filesDir)

    val manifest: Deferred<ModelManifest> = scope.async(Dispatchers.IO) {
        ModelManifest.parse(readAsset("${ModelManifest.ASSET_DIR}/manifest.json"))
    }

    val knowledge: Deferred<KnowledgeBase> = scope.async(Dispatchers.IO) {
        KnowledgeBase.parse(readAsset(KnowledgeBase.ASSET_PATH))
    }

    val classifier: Deferred<CropClassifier> = scope.async(Dispatchers.Default) {
        CropClassifier.load(context, manifest.await(), settings)
    }

    init {
        scope.launch { history.load() }
    }

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
}

class ShambaApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.appContainer: AppContainer get() = (applicationContext as ShambaApp).container
