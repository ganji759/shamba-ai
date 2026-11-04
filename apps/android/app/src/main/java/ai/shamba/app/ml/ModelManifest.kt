package ai.shamba.app.ml

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

/** The parts of ml manifest.json that the app uses. */
data class ModelManifest(
    val name: String,
    val version: String,
    val backbone: String?,
    val classes: List<String>,
    val thresholds: Thresholds,
    val inputShape: List<Long>,
    val files: List<ModelFileInfo>,
    val executorchExportVersion: String?,
) {
    fun file(name: String): ModelFileInfo? = files.firstOrNull { it.file == name }

    companion object {
        const val MODEL_FILE = "shamba_crop_health_xnnpack_int8.pte"
        const val ASSET_DIR = "model"

        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): ModelManifest {
            val root = json.parseToJsonElement(text).jsonObject
            val classes = root["classes"]?.jsonArray?.map { (it as JsonPrimitive).content }
                ?: error("manifest.json has no \"classes\"")
            require(classes.isNotEmpty()) { "manifest.json \"classes\" is empty" }
            require(classes.toSet().size == classes.size) { "manifest.json \"classes\" has duplicates" }

            val defaults = Thresholds()
            val t = root["thresholds"] as? JsonObject
            val thresholds = Thresholds(
                confident = t.float("confident") ?: defaults.confident,
                uncertainBelow = t.float("uncertain_below") ?: defaults.uncertainBelow,
            )
            val input = root["input"] as? JsonObject
            val shape = (input?.get("shape") as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.longOrNull }
                ?: listOf(1L, 3L, 224L, 224L)
            val files = (root["files"] as? JsonArray).orEmpty().mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                ModelFileInfo(
                    file = o.string("file") ?: return@mapNotNull null,
                    target = o.string("target"),
                    bytes = (o["bytes"] as? JsonPrimitive)?.longOrNull,
                    sha256 = o.string("sha256"),
                    dtype = o.string("dtype"),
                )
            }
            val builtWith = root["built_with"] as? JsonObject
            return ModelManifest(
                name = root.string("name") ?: "shamba-crop-health",
                version = root.string("version") ?: "unknown",
                backbone = root.string("backbone"),
                classes = classes,
                thresholds = thresholds,
                inputShape = shape,
                files = files,
                executorchExportVersion = builtWith.string("executorch"),
            )
        }

        private fun JsonObject?.string(key: String): String? = (this?.get(key) as? JsonPrimitive)?.contentOrNull

        private fun JsonObject?.float(key: String): Float? = (this?.get(key) as? JsonPrimitive)?.floatOrNull
    }
}

data class ModelFileInfo(
    val file: String,
    val target: String?,
    val bytes: Long?,
    val sha256: String?,
    val dtype: String?,
)
