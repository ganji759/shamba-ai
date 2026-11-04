package ai.shamba.app.knowledge

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** App languages. [code] is the key in diseases.json and the Android locale tag. */
enum class Language(val code: String, val nativeName: String) {
    EN("en", "English"),
    SW("sw", "Kiswahili"),
    FR("fr", "Français"),
    ;

    companion object {
        val DEFAULT = EN

        fun fromCode(code: String?): Language? = entries.firstOrNull { it.code == code }
    }
}

/** Text in several languages. [get] falls back to English, then to any language. */
data class LocalizedText(val values: Map<String, String>) {
    fun get(language: Language): String =
        values[language.code]?.takeIf { it.isNotBlank() }
            ?: values[Language.DEFAULT.code]?.takeIf { it.isNotBlank() }
            ?: values.values.firstOrNull { it.isNotBlank() }
            ?: ""

    fun has(language: Language): Boolean = !values[language.code].isNullOrBlank()

    companion object {
        val EMPTY = LocalizedText(emptyMap())
    }
}

/** A list in several languages. [get] falls back to English, then to any language. */
data class LocalizedList(val values: Map<String, List<String>>) {
    fun get(language: Language): List<String> =
        values[language.code]?.takeIf { it.isNotEmpty() }
            ?: values[Language.DEFAULT.code]?.takeIf { it.isNotEmpty() }
            ?: values.values.firstOrNull { it.isNotEmpty() }
            ?: emptyList()

    companion object {
        val EMPTY = LocalizedList(emptyMap())
    }
}

enum class Urgency(val key: String, val rank: Int) {
    NONE("none", 0),
    MEDIUM("medium", 1),
    HIGH("high", 2),
    CRITICAL("critical", 3),
    ;

    companion object {
        /** Unknown values read as MEDIUM, so the app never hides a problem as "Healthy". */
        fun fromKey(key: String?): Urgency = entries.firstOrNull { it.key == key } ?: MEDIUM
    }
}

data class Condition(
    val id: String,
    val crop: String,
    /** fungus, bacteria, virus, pest or healthy. */
    val type: String,
    val scientific: String?,
    val urgency: Urgency,
    val name: LocalizedText,
    val summary: LocalizedText,
    val actions: LocalizedList,
    val prevention: LocalizedList,
    /** Null for healthy classes. */
    val escalate: LocalizedText?,
) {
    val isHealthy: Boolean get() = type == "healthy" || urgency == Urgency.NONE
}

data class UiTexts(
    val retakeTips: LocalizedText,
    val safety: LocalizedText,
    val disclaimer: LocalizedText,
)

data class KnowledgeBase(
    val version: String,
    val languages: List<String>,
    val reviewStatus: String?,
    val ui: UiTexts,
    /** crop id to crop name, in file order. */
    val crops: Map<String, LocalizedText>,
    /** class id to condition, in file order. */
    val conditions: Map<String, Condition>,
) {
    fun condition(classId: String): Condition? = conditions[classId]

    fun cropName(cropId: String, language: Language): String = crops[cropId]?.get(language) ?: cropId

    fun conditionsForCrop(cropId: String): List<Condition> = conditions.values.filter { it.crop == cropId }

    /** Class ids from the model that have no entry here. Should be empty. */
    fun missingClasses(classIds: List<String>): List<String> = classIds.filterNot { conditions.containsKey(it) }

    companion object {
        const val ASSET_PATH = "knowledge/diseases.json"

        private val json = Json { ignoreUnknownKeys = true }

        fun parse(source: String): KnowledgeBase {
            val root = json.parseToJsonElement(source) as? JsonObject ?: error("diseases.json is not an object")
            val ui = root["ui"] as? JsonObject
            val crops = (root["crops"] as? JsonObject).orEmpty().mapValues { (_, v) -> text(v) }
            val conditions = (root["conditions"] as? JsonObject ?: error("diseases.json has no \"conditions\""))
                .mapNotNull { (id, v) ->
                    val o = v as? JsonObject ?: return@mapNotNull null
                    id to Condition(
                        id = id,
                        crop = o.string("crop") ?: id.substringBefore('_'),
                        type = o.string("type") ?: "unknown",
                        scientific = o.string("scientific"),
                        urgency = Urgency.fromKey(o.string("urgency")),
                        name = text(o["name"]),
                        summary = text(o["summary"]),
                        actions = list(o["actions"]),
                        prevention = list(o["prevention"]),
                        escalate = o["escalate"]?.let(::text)?.takeIf { it.values.isNotEmpty() },
                    )
                }
                .toMap(LinkedHashMap())
            return KnowledgeBase(
                version = root.string("version") ?: "unknown",
                languages = (root["languages"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                    ?: Language.entries.map { it.code },
                reviewStatus = (root["review"] as? JsonObject).string("status"),
                ui = UiTexts(
                    retakeTips = ui?.get("retake_tips")?.let(::text) ?: LocalizedText.EMPTY,
                    safety = ui?.get("safety")?.let(::text) ?: LocalizedText.EMPTY,
                    disclaimer = ui?.get("disclaimer")?.let(::text) ?: LocalizedText.EMPTY,
                ),
                crops = crops,
                conditions = conditions,
            )
        }

        private fun text(el: JsonElement?): LocalizedText = when (el) {
            is JsonObject -> LocalizedText(
                el.mapNotNull { (k, v) -> (v as? JsonPrimitive)?.contentOrNull?.let { k to it } }.toMap(),
            )
            // A plain string is the same text in every language.
            is JsonPrimitive -> el.contentOrNull?.let { LocalizedText(mapOf(Language.DEFAULT.code to it)) }
                ?: LocalizedText.EMPTY
            else -> LocalizedText.EMPTY
        }

        private fun list(el: JsonElement?): LocalizedList = when (el) {
            is JsonObject -> LocalizedList(
                el.mapNotNull { (k, v) ->
                    (v as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }?.let { k to it }
                }.toMap(),
            )
            is JsonArray -> LocalizedList(
                mapOf(Language.DEFAULT.code to el.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }),
            )
            else -> LocalizedList.EMPTY
        }

        private fun JsonObject?.string(key: String): String? = (this?.get(key) as? JsonPrimitive)?.contentOrNull
    }
}
