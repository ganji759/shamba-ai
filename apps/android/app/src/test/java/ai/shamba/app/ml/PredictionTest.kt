package ai.shamba.app.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PredictionTest {
    private val thresholds = Thresholds(confident = 0.70f, uncertainBelow = 0.45f)
    private val classes = listOf("a", "b", "c", "d")

    @Test
    fun certaintyBands() {
        assertEquals(Certainty.CONFIDENT, thresholds.certaintyOf(0.70f)) // inclusive
        assertEquals(Certainty.CONFIDENT, thresholds.certaintyOf(0.99f))
        assertEquals(Certainty.LIKELY, thresholds.certaintyOf(0.6999f))
        assertEquals(Certainty.LIKELY, thresholds.certaintyOf(0.45f)) // 0.45 is not "below"
        assertEquals(Certainty.UNCERTAIN, thresholds.certaintyOf(0.4499f))
        assertEquals(Certainty.UNCERTAIN, thresholds.certaintyOf(0f))
        assertEquals(Certainty.UNCERTAIN, thresholds.certaintyOf(Float.NaN))
    }

    @Test
    fun customThresholdsFromManifest() {
        val t = Thresholds(confident = 0.8f, uncertainBelow = 0.5f)
        assertEquals(Certainty.LIKELY, t.certaintyOf(0.75f))
        assertEquals(Certainty.UNCERTAIN, t.certaintyOf(0.49f))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvertedThresholds() {
        Thresholds(confident = 0.4f, uncertainBelow = 0.6f)
    }

    @Test
    fun ranksTopThreeAndKeepsClassOrder() {
        val p = PredictionLogic.predict(floatArrayOf(0.1f, 0.6f, 0.05f, 0.25f), classes, thresholds)
        assertEquals(listOf("b", "d", "a"), p.ranked.map { it.classId })
        assertEquals("b", p.top.classId)
        assertEquals(listOf("d", "a"), p.others.map { it.classId })
        assertEquals(Certainty.LIKELY, p.certainty)
    }

    @Test
    fun tiesKeepModelOrder() {
        val p = PredictionLogic.predict(floatArrayOf(0.25f, 0.25f, 0.25f, 0.25f), classes, thresholds)
        assertEquals(listOf("a", "b", "c"), p.ranked.map { it.classId })
        assertEquals(Certainty.UNCERTAIN, p.certainty)
    }

    @Test
    fun confidentTopClass() {
        val p = PredictionLogic.predict(floatArrayOf(0.02f, 0.03f, 0.9f, 0.05f), classes, thresholds)
        assertEquals("c", p.top.classId)
        assertEquals(Certainty.CONFIDENT, p.certainty)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOutputSizeMismatch() {
        PredictionLogic.predict(floatArrayOf(0.5f, 0.5f), classes, thresholds)
    }

    @Test
    fun probabilitiesPassThroughUnchanged() {
        val probs = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f)
        assertTrue(probs.contentEquals(PredictionLogic.toProbabilities(probs)))
    }

    @Test
    fun logitsGetSoftmax() {
        val out = PredictionLogic.toProbabilities(floatArrayOf(2f, 1f, 0f, -1f))
        assertEquals(1f, out.sum(), 1e-5f)
        assertTrue(out[0] > out[1] && out[1] > out[2] && out[2] > out[3])
        assertEquals(0.6439f, out[0], 1e-3f)
    }

    @Test
    fun nanOutputReadsAsNotSure() {
        val p = PredictionLogic.predict(floatArrayOf(Float.NaN, 0.5f, 0.3f, 0.2f), classes, thresholds)
        assertEquals(Certainty.UNCERTAIN, p.certainty)
    }

    @Test
    fun certaintyKeyRoundTrip() {
        Certainty.entries.forEach { assertEquals(it, Certainty.fromKey(it.key)) }
        assertEquals(Certainty.UNCERTAIN, Certainty.fromKey("bogus"))
    }
}
