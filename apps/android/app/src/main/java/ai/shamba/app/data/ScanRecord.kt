package ai.shamba.app.data

import ai.shamba.app.ml.Certainty
import ai.shamba.app.ml.ClassScore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** One past scan, stored on the phone only. */
data class ScanRecord(
    val id: String,
    val timeMillis: Long,
    /** File names inside the scans dir. */
    val photoFile: String,
    val thumbFile: String,
    /** Top class first, then the runners-up. */
    val ranked: List<ClassScore>,
    val certainty: Certainty,
    val latencyMs: Long,
    val modelVersion: String,
) {
    val top: ClassScore get() = ranked.first()
    val others: List<ClassScore> get() = ranked.drop(1)
}

/** JSON encoding for the history file. Pure Kotlin, so it is unit tested. */
object HistoryCodec {
    const val FORMAT_VERSION = 1

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(records: List<ScanRecord>): String {
        val root = buildJsonObject {
            put("format", FORMAT_VERSION)
            putJsonArray("scans") {
                records.forEach { r ->
                    add(
                        buildJsonObject {
                            put("id", r.id)
                            put("time", r.timeMillis)
                            put("photo", r.photoFile)
                            put("thumb", r.thumbFile)
                            put("certainty", r.certainty.key)
                            put("latency_ms", r.latencyMs)
                            put("model_version", r.modelVersion)
                            put(
                                "ranked",
                                buildJsonArray {
                                    r.ranked.forEach { s ->
                                        add(
                                            buildJsonObject {
                                                put("class", s.classId)
                                                put("p", s.probability)
                                            },
                                        )
                                    }
                                },
                            )
                        },
                    )
                }
            }
        }
        return root.toString()
    }

    /** Skips broken entries instead of failing the whole file. */
    fun decode(text: String): List<ScanRecord> {
        if (text.isBlank()) return emptyList()
        val root = json.parseToJsonElement(text) as? JsonObject ?: return emptyList()
        val scans = root["scans"] as? JsonArray ?: return emptyList()
        return scans.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val ranked = (o["ranked"] as? JsonArray).orEmpty().mapNotNull { s ->
                val so = s as? JsonObject ?: return@mapNotNull null
                val cls = so.string("class") ?: return@mapNotNull null
                val p = (so["p"] as? JsonPrimitive)?.floatOrNull ?: return@mapNotNull null
                ClassScore(cls, p)
            }
            if (ranked.isEmpty()) return@mapNotNull null
            ScanRecord(
                id = o.string("id") ?: return@mapNotNull null,
                timeMillis = (o["time"] as? JsonPrimitive)?.longOrNull ?: 0L,
                photoFile = o.string("photo") ?: return@mapNotNull null,
                thumbFile = o.string("thumb") ?: return@mapNotNull null,
                ranked = ranked,
                certainty = Certainty.fromKey(o.string("certainty")),
                latencyMs = (o["latency_ms"] as? JsonPrimitive)?.longOrNull ?: 0L,
                modelVersion = o.string("model_version") ?: "unknown",
            )
        }
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
}
