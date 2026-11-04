package ai.shamba.app.data

import ai.shamba.app.ml.Certainty
import ai.shamba.app.ml.ClassScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryCodecTest {
    private val record = ScanRecord(
        id = "1700000000000-abcd1234",
        timeMillis = 1_700_000_000_000,
        photoFile = "1700000000000-abcd1234.jpg",
        thumbFile = "1700000000000-abcd1234_t.jpg",
        ranked = listOf(ClassScore("cassava_mosaic", 0.81f), ClassScore("cassava_healthy", 0.1f), ClassScore("bean_rust", 0.04f)),
        certainty = Certainty.CONFIDENT,
        latencyMs = 87,
        modelVersion = "1.0.0",
    )

    @Test
    fun roundTrip() {
        val decoded = HistoryCodec.decode(HistoryCodec.encode(listOf(record)))
        assertEquals(listOf(record), decoded)
    }

    @Test
    fun skipsBrokenEntriesAndKeepsGoodOnes() {
        val good = HistoryCodec.encode(listOf(record))
        val broken = good.replace("\"scans\":[", "\"scans\":[{\"id\":\"x\"},42,")
        val decoded = HistoryCodec.decode(broken)
        assertEquals(listOf(record), decoded)
    }

    @Test
    fun emptyOrForeignInputGivesEmptyList() {
        assertTrue(HistoryCodec.decode("").isEmpty())
        assertTrue(HistoryCodec.decode("[]").isEmpty())
        assertTrue(HistoryCodec.decode("{\"other\":1}").isEmpty())
    }
}
