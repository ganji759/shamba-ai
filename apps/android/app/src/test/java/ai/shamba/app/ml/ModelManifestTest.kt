package ai.shamba.app.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ModelManifestTest {
    @Test
    fun parsesMinimalManifestWithDefaults() {
        val m = ModelManifest.parse("""{"classes": ["x", "y"]}""")
        assertEquals(listOf("x", "y"), m.classes)
        assertEquals(0.70f, m.thresholds.confident, 1e-6f)
        assertEquals(0.45f, m.thresholds.uncertainBelow, 1e-6f)
        assertEquals("unknown", m.version)
        assertEquals(listOf(1L, 3L, 224L, 224L), m.inputShape)
    }

    @Test
    fun readsThresholdsAndFiles() {
        val m = ModelManifest.parse(
            """
            {"name": "n", "version": "2.1.0", "classes": ["a"],
             "thresholds": {"confident": 0.8, "uncertain_below": 0.5},
             "files": [{"file": "shamba_crop_health_xnnpack_int8.pte", "target": "xnnpack-int8",
                        "bytes": 123, "sha256": "ab", "dtype": "int8"}],
             "built_with": {"executorch": "1.5.1+cpu"}}
            """.trimIndent(),
        )
        assertEquals("2.1.0", m.version)
        assertEquals(0.8f, m.thresholds.confident, 1e-6f)
        assertEquals(0.5f, m.thresholds.uncertainBelow, 1e-6f)
        val f = m.file(ModelManifest.MODEL_FILE)
        assertNotNull(f)
        assertEquals(123L, f!!.bytes)
        assertEquals("ab", f.sha256)
        assertEquals("1.5.1+cpu", m.executorchExportVersion)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDuplicateClasses() {
        ModelManifest.parse("""{"classes": ["a", "a"]}""")
    }

    @Test
    fun realManifestMatchesTheAppContract() {
        val path = System.getProperty("shamba.manifest") ?: return
        val m = ModelManifest.parse(File(path).readText())
        assertEquals(14, m.classes.size)
        assertEquals(listOf(1L, 3L, 224L, 224L), m.inputShape)
        assertEquals(0.70f, m.thresholds.confident, 1e-6f)
        assertEquals(0.45f, m.thresholds.uncertainBelow, 1e-6f)
        val f = m.file(ModelManifest.MODEL_FILE)
        assertNotNull("manifest must list ${ModelManifest.MODEL_FILE}", f)
        assertTrue(f!!.sha256!!.matches(Regex("[0-9a-f]{64}")))
    }
}
