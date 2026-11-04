package ai.shamba.app.knowledge

import ai.shamba.app.ml.ModelManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class KnowledgeBaseTest {
    /** The real repo file, passed in by Gradle (see testOptions in app/build.gradle.kts). */
    private val realKb: KnowledgeBase by lazy {
        val path = System.getProperty("shamba.knowledge") ?: error("shamba.knowledge system property is not set")
        KnowledgeBase.parse(File(path).readText())
    }

    private val sample = """
        {
          "version": "9.9.9",
          "languages": ["en", "sw", "fr"],
          "review": {"status": "draft"},
          "ui": {
            "retake_tips": {"en": "Retake.", "sw": "Piga tena."},
            "safety": {"en": "Wear gloves."},
            "disclaimer": {"en": "Guidance only.", "fr": ""}
          },
          "crops": {"maize": {"en": "Maize", "sw": "Mahindi", "fr": "Maïs"}},
          "conditions": {
            "maize_streak_virus": {
              "crop": "maize", "type": "virus", "scientific": "Maize streak virus", "urgency": "high",
              "name": {"en": "Maize streak", "sw": "Michirizi ya mahindi"},
              "summary": {"en": "Yellow streaks."},
              "actions": {"en": ["Pull out sick plants.", "Control leafhoppers."], "sw": []},
              "prevention": {"en": ["Plant early."], "fr": ["Semez tôt."]},
              "escalate": {"en": "Call if many plants."}
            },
            "maize_healthy": {
              "crop": "maize", "type": "healthy", "urgency": "none",
              "name": {"en": "Healthy maize"}, "summary": {"en": "No disease."},
              "actions": {"en": ["Keep scouting."]}, "prevention": {"en": ["Rotate."]}
            },
            "maize_odd": {"crop": "maize", "type": "pest", "urgency": "urgent!", "name": {"en": "Odd"}}
          }
        }
    """.trimIndent()

    @Test
    fun parsesSampleFields() {
        val kb = KnowledgeBase.parse(sample)
        assertEquals("9.9.9", kb.version)
        assertEquals("draft", kb.reviewStatus)
        val c = kb.condition("maize_streak_virus")!!
        assertEquals("maize", c.crop)
        assertEquals("virus", c.type)
        assertEquals("Maize streak virus", c.scientific)
        assertEquals(Urgency.HIGH, c.urgency)
        assertEquals(listOf("Pull out sick plants.", "Control leafhoppers."), c.actions.get(Language.EN))
        assertNotNull(c.escalate)
        assertFalse(c.isHealthy)
    }

    @Test
    fun languageFallsBackToEnglish() {
        val kb = KnowledgeBase.parse(sample)
        val c = kb.condition("maize_streak_virus")!!
        assertEquals("Michirizi ya mahindi", c.name.get(Language.SW))
        assertEquals("Maize streak", c.name.get(Language.FR)) // no fr: English
        assertEquals("Yellow streaks.", c.summary.get(Language.SW))
        // Empty Swahili list falls back to English.
        assertEquals(listOf("Pull out sick plants.", "Control leafhoppers."), c.actions.get(Language.SW))
        assertEquals(listOf("Semez tôt."), c.prevention.get(Language.FR))
        assertEquals(listOf("Plant early."), c.prevention.get(Language.SW))
        // Blank French text falls back to English.
        assertEquals("Guidance only.", kb.ui.disclaimer.get(Language.FR))
        assertEquals("Piga tena.", kb.ui.retakeTips.get(Language.SW))
        assertEquals("Retake.", kb.ui.retakeTips.get(Language.FR))
        assertEquals("Mahindi", kb.cropName("maize", Language.SW))
        assertEquals("cassava", kb.cropName("cassava", Language.SW)) // unknown crop id shows the id
    }

    @Test
    fun fallbackUsesAnyLanguageWhenEnglishIsMissing() {
        val t = LocalizedText(mapOf("sw" to "Habari"))
        assertEquals("Habari", t.get(Language.FR))
        assertEquals("", LocalizedText.EMPTY.get(Language.EN))
        assertTrue(LocalizedList.EMPTY.get(Language.SW).isEmpty())
    }

    @Test
    fun healthyHasNoEscalationAndUnknownUrgencyIsNotHealthy() {
        val kb = KnowledgeBase.parse(sample)
        val healthy = kb.condition("maize_healthy")!!
        assertNull(healthy.escalate)
        assertNull(healthy.scientific)
        assertTrue(healthy.isHealthy)
        assertEquals(Urgency.NONE, healthy.urgency)
        // An unknown urgency must never read as "Healthy".
        assertEquals(Urgency.MEDIUM, kb.condition("maize_odd")!!.urgency)
        assertTrue(kb.condition("maize_odd")!!.actions.get(Language.EN).isEmpty())
    }

    @Test
    fun realFileHasEveryLanguageForEveryCondition() {
        val kb = realKb
        assertEquals(listOf("en", "sw", "fr"), kb.languages)
        assertEquals(4, kb.crops.size)
        assertEquals(14, kb.conditions.size)
        kb.conditions.values.forEach { c ->
            Language.entries.forEach { lang ->
                assertTrue("${c.id} name ${lang.code}", c.name.has(lang))
                assertTrue("${c.id} summary ${lang.code}", c.summary.has(lang))
                assertTrue("${c.id} actions ${lang.code}", c.actions.values[lang.code].orEmpty().isNotEmpty())
                assertTrue("${c.id} prevention ${lang.code}", c.prevention.values[lang.code].orEmpty().isNotEmpty())
                if (!c.isHealthy) assertTrue("${c.id} escalate ${lang.code}", c.escalate?.has(lang) == true)
            }
            assertTrue("${c.id} crop", kb.crops.containsKey(c.crop))
        }
        Language.entries.forEach { lang ->
            assertTrue(kb.ui.retakeTips.has(lang))
            assertTrue(kb.ui.safety.has(lang))
            assertTrue(kb.ui.disclaimer.has(lang))
        }
    }

    @Test
    fun realFileUrgencies() {
        val kb = realKb
        assertEquals(Urgency.CRITICAL, kb.condition("maize_lethal_necrosis")!!.urgency)
        assertEquals(Urgency.HIGH, kb.condition("cassava_mosaic")!!.urgency)
        assertEquals(Urgency.MEDIUM, kb.condition("bean_rust")!!.urgency)
        assertEquals(Urgency.NONE, kb.condition("banana_healthy")!!.urgency)
        assertEquals(listOf("bean", "cassava", "maize", "banana"), kb.crops.keys.toList())
        assertEquals(3, kb.conditionsForCrop("bean").size)
    }

    /** Contract check: every class the model can output has advice. */
    @Test
    fun everyManifestClassHasAKnowledgeEntry() {
        val path = System.getProperty("shamba.manifest") ?: return // no model artifacts on this machine
        val manifest = ModelManifest.parse(File(path).readText())
        assertEquals(emptyList<String>(), realKb.missingClasses(manifest.classes))
    }
}
