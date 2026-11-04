package ai.shamba.app.ml

import android.content.Context
import android.os.SystemClock
import android.util.Log
import ai.shamba.app.data.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.pytorch.executorch.EValue
import org.pytorch.executorch.Module
import org.pytorch.executorch.Tensor
import java.io.File
import java.security.DigestInputStream
import java.security.MessageDigest

/** Result of one forward pass. */
class Inference(val output: FloatArray, val latencyMs: Long)

/**
 * The crop-health model on ExecuTorch (XNNPACK backend, int8).
 * Load it once with [load]. [run] always executes on Dispatchers.Default and never on the main thread.
 * ExecuTorch Module is not safe for parallel calls, so a mutex keeps calls one at a time.
 */
class CropClassifier private constructor(
    private val module: Module,
    val modelFile: File,
    val sha256: String,
) {
    private val mutex = Mutex()

    suspend fun run(input: FloatArray): Inference = withContext(Dispatchers.Default) {
        require(input.size == Preprocess.TENSOR_SIZE) { "Input must have ${Preprocess.TENSOR_SIZE} floats" }
        mutex.withLock {
            val start = SystemClock.elapsedRealtimeNanos()
            val tensor = Tensor.fromBlob(input, Preprocess.INPUT_SHAPE)
            val outputs = module.forward(EValue.from(tensor))
            val result = outputs[0].toTensor().dataAsFloatArray
            val ms = (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000
            Inference(result, ms)
        }
    }

    companion object {
        private const val TAG = "CropClassifier"

        /**
         * Copies the model from assets to app storage (ExecuTorch needs a file path), loads it and
         * runs one warm-up pass so the first real scan is not slow. Call from a background dispatcher.
         */
        suspend fun load(context: Context, manifest: ModelManifest, settings: AppSettings): CropClassifier =
            withContext(Dispatchers.Default) {
                val (file, sha) = ModelInstaller.install(context, manifest, settings)
                val start = SystemClock.elapsedRealtime()
                val module = Module.load(file.absolutePath)
                module.loadMethod("forward")
                val classifier = CropClassifier(module, file, sha)
                val warm = classifier.run(FloatArray(Preprocess.TENSOR_SIZE))
                Log.i(TAG, "Model ready in ${SystemClock.elapsedRealtime() - start} ms, warm-up ${warm.latencyMs} ms")
                classifier
            }
    }
}

/** Copies the .pte out of the APK once, and again only when the bundled model changes. */
internal object ModelInstaller {
    private const val ASSET_PATH = "${ModelManifest.ASSET_DIR}/${ModelManifest.MODEL_FILE}"

    fun install(context: Context, manifest: ModelManifest, settings: AppSettings): Pair<File, String> {
        val dir = File(context.filesDir, "model").apply { mkdirs() }
        val target = File(dir, ModelManifest.MODEL_FILE)
        val expected = manifest.file(ModelManifest.MODEL_FILE)
        val sourceKey = expected?.sha256 ?: manifest.version
        val knownSha = settings.installedModelSha
        val sizeOk = expected?.bytes == null || target.length() == expected.bytes
        if (target.isFile && knownSha != null && settings.installedModelSource == sourceKey && sizeOk) {
            return target to knownSha
        }
        val tmp = File(dir, "${ModelManifest.MODEL_FILE}.tmp")
        val digest = MessageDigest.getInstance("SHA-256")
        context.assets.open(ASSET_PATH).use { raw ->
            DigestInputStream(raw, digest).use { input ->
                tmp.outputStream().use { out -> input.copyTo(out, bufferSize = 1 shl 16) }
            }
        }
        if (!tmp.renameTo(target)) {
            target.delete()
            check(tmp.renameTo(target)) { "Could not install the model file" }
        }
        val sha = digest.digest().joinToString("") { "%02x".format(it) }
        settings.installedModelSha = sha
        settings.installedModelSource = sourceKey
        return target to sha
    }
}
