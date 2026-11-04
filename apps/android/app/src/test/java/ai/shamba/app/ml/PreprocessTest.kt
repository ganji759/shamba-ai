package ai.shamba.app.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PreprocessTest {
    private val n = Preprocess.SIZE
    private val plane = n * n

    private fun argb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    private fun solid(w: Int, h: Int, color: Int) = IntArray(w * h) { color }

    @Test
    fun outputIsContiguousChwWithExpectedSize() {
        val out = Preprocess.toChw(solid(300, 400, argb(10, 20, 30)), 300, 400)
        assertEquals(3 * 224 * 224, out.size)
        assertEquals(Preprocess.TENSOR_SIZE, out.size)
        assertEquals(listOf(1L, 3L, 224L, 224L), Preprocess.INPUT_SHAPE.toList())
    }

    @Test
    fun channelsArePlanarRgbDividedBy255() {
        val out = Preprocess.toChw(solid(320, 240, argb(255, 128, 0)), 320, 240)
        for (i in 0 until plane) {
            assertEquals(1f, out[i], 1e-5f) // R plane first
            assertEquals(128f / 255f, out[plane + i], 1e-5f) // then G
            assertEquals(0f, out[2 * plane + i], 1e-5f) // then B
        }
    }

    @Test
    fun valuesStayInUnitRange() {
        val w = 517
        val h = 389
        val px = IntArray(w * h) { i -> argb((i * 7) % 256, (i * 13) % 256, (i * 29) % 256) }
        val out = Preprocess.toChw(px, w, h)
        assertTrue(out.all { it in 0f..1f })
    }

    @Test
    fun cropSideIsShortSideTimes224Over256() {
        assertEquals(224.0, Preprocess.cropSide(256, 256), 1e-9)
        assertEquals(224.0, Preprocess.cropSide(400, 256), 1e-9)
        assertEquals(420.0, Preprocess.cropSide(480, 640), 1e-9) // 480 * 224 / 256
    }

    /** 256x256 gives a 224 crop at offset 16 and scale 1, so the output is the exact center pixels. */
    @Test
    fun scaleOneIsAnExactCenterCrop() {
        val w = 256
        val h = 256
        val px = IntArray(w * h) { i -> val x = i % w; val y = i / w; argb(x, y, (x + y) / 2) }
        val out = Preprocess.toChw(px, w, h)
        for (oy in listOf(0, 1, 100, 223)) {
            for (ox in listOf(0, 1, 57, 223)) {
                val sx = ox + 16
                val sy = oy + 16
                assertEquals(sx / 255f, out[oy * n + ox], 1e-5f)
                assertEquals(sy / 255f, out[plane + oy * n + ox], 1e-5f)
                assertEquals(((sx + sy) / 2) / 255f, out[2 * plane + oy * n + ox], 1e-5f)
            }
        }
    }

    /** Pixels outside the center square must not leak into the output. */
    @Test
    fun borderOutsideCenterSquareIsIgnored() {
        val w = 640
        val h = 480
        val side = Preprocess.cropSide(w, h) // 420
        val left = (w - side) / 2 // 110
        val top = (h - side) / 2 // 30
        val green = argb(0, 200, 0)
        val red = argb(255, 0, 0)
        val px = IntArray(w * h) { i ->
            val x = i % w
            val y = i / w
            // A 3 px margin inside the crop keeps the filter footprint clear of the red border.
            if (x >= left + 3 && x < left + side - 3 && y >= top + 3 && y < top + side - 3) green else red
        }
        val out = Preprocess.toChw(px, w, h)
        // Every output pixel away from the edge must be pure green.
        for (oy in 2 until n - 2) for (ox in 2 until n - 2) {
            assertEquals(0f, out[oy * n + ox], 1e-4f)
            assertEquals(200f / 255f, out[plane + oy * n + ox], 1e-4f)
        }
    }

    /** A horizontal linear ramp stays linear after a downscale (bilinear keeps gradients). */
    @Test
    fun downscaleKeepsALinearRamp() {
        val w = 512
        val h = 512
        val px = IntArray(w * h) { i -> val x = i % w; argb(x / 2, 0, 0) }
        val out = Preprocess.toChw(px, w, h)
        // Crop side 448 at offset 32, scale 2. Output x has source center 33 + 2x and the filter
        // covers pixels 31+2x .. 34+2x with weights 1/8, 3/8, 3/8, 1/8. Their values are
        // 15+x, 16+x, 16+x, 17+x, so the result is exactly 16 + x.
        for (ox in 4 until n - 4) {
            assertEquals("ox=$ox", (16 + ox) / 255f, out[ox], 1e-5f)
        }
    }

    /** Upscale (small photo) uses plain bilinear interpolation between neighbours. */
    @Test
    fun upscaleInterpolatesBetweenNeighbours() {
        val w = 128
        val h = 128
        val px = IntArray(w * h) { i -> val x = i % w; argb(if (x < 64) 0 else 255, 0, 0) }
        val out = Preprocess.toChw(px, w, h)
        val row = FloatArray(n) { out[100 * n + it] }
        assertEquals(0f, row[0], 1e-5f)
        assertEquals(1f, row[n - 1], 1e-5f)
        // Monotonic, with at least one value strictly between 0 and 1 at the edge.
        for (i in 1 until n) assertTrue(row[i] >= row[i - 1] - 1e-6f)
        assertTrue(row.any { it > 0.05f && it < 0.95f })
    }

    @Test
    fun filterWeightsSumToOne() {
        for ((side, limit) in listOf(224.0 to 256, 420.0 to 640, 112.0 to 128, 900.0 to 1200)) {
            val taps = Preprocess.filterTaps(srcStart = (limit - side) / 2, srcSide = side, srcLimit = limit)
            for (o in 0 until n) {
                var sum = 0f
                for (k in 0 until taps.count[o]) sum += taps.weights[o * taps.maxTaps + k]
                assertTrue("side=$side o=$o sum=$sum", abs(sum - 1f) < 1e-4f)
                assertTrue(taps.start[o] >= 0 && taps.start[o] + taps.count[o] <= limit)
            }
        }
    }

    @Test
    fun reusesTheOutputBuffer() {
        val buffer = FloatArray(Preprocess.TENSOR_SIZE) { -1f }
        val out = Preprocess.toChw(solid(300, 300, argb(51, 51, 51)), 300, 300, buffer)
        assertTrue(out === buffer)
        assertEquals(0.2f, buffer[12345], 1e-5f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsTooFewPixels() {
        Preprocess.toChw(IntArray(10), 300, 300)
    }
}
