package ai.shamba.app.ml

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Converts ARGB pixels to the model input. Pure Kotlin, so JVM unit tests can check the math.
 *
 * Contract (see ml manifest.json "input"):
 * 1. Take the center square with side = shortSide * 224 / 256.
 *    This is the same region as "resize short side to 256, center crop 224".
 * 2. Scale that square to 224 x 224 with bilinear filtering. When the square is larger than
 *    224, the bilinear (triangle) filter widens with the scale factor, so it also smooths
 *    (antialias). This matches PIL and torchvision bilinear resize.
 * 3. Write planar R, G, B channels (CHW, contiguous) with values divided by 255.
 *    Normalization (mean and std) is inside the model.
 */
object Preprocess {
    const val SIZE = 224
    const val RESIZE = 256
    const val CHANNELS = 3

    /** Number of floats in one input tensor: 3 * 224 * 224. */
    const val TENSOR_SIZE = CHANNELS * SIZE * SIZE

    val INPUT_SHAPE: LongArray get() = longArrayOf(1, CHANNELS.toLong(), SIZE.toLong(), SIZE.toLong())

    /** Side of the center square, in source pixels. */
    fun cropSide(width: Int, height: Int): Double = min(width, height).toDouble() * SIZE / RESIZE

    /**
     * @param pixels ARGB_8888 pixels, row-major, as returned by Bitmap.getPixels.
     * @param out optional output buffer with [TENSOR_SIZE] floats, reused to save memory.
     * @return CHW float buffer, R plane first, values in [0, 1].
     */
    fun toChw(pixels: IntArray, width: Int, height: Int, out: FloatArray = FloatArray(TENSOR_SIZE)): FloatArray {
        require(width > 0 && height > 0) { "Image size must be positive: ${width}x$height" }
        require(pixels.size >= width * height) { "Pixel array is too small for ${width}x$height" }
        require(out.size == TENSOR_SIZE) { "Output buffer must have $TENSOR_SIZE floats" }

        val side = cropSide(width, height)
        val left = (width - side) / 2.0
        val top = (height - side) / 2.0

        val xTaps = filterTaps(srcStart = left, srcSide = side, srcLimit = width)
        val yTaps = filterTaps(srcStart = top, srcSide = side, srcLimit = height)

        // Pass 1: horizontal filter for every source row the vertical pass needs.
        val rowFirst = yTaps.minIndex
        val rowLast = yTaps.maxIndex
        val rows = rowLast - rowFirst + 1
        val tmpR = FloatArray(rows * SIZE)
        val tmpG = FloatArray(rows * SIZE)
        val tmpB = FloatArray(rows * SIZE)
        for (ry in 0 until rows) {
            val rowBase = (rowFirst + ry) * width
            val tmpBase = ry * SIZE
            for (ox in 0 until SIZE) {
                var r = 0f
                var g = 0f
                var b = 0f
                val start = xTaps.start[ox]
                val wBase = ox * xTaps.maxTaps
                for (k in 0 until xTaps.count[ox]) {
                    val c = pixels[rowBase + start + k]
                    val w = xTaps.weights[wBase + k]
                    r += ((c shr 16) and 0xFF) * w
                    g += ((c shr 8) and 0xFF) * w
                    b += (c and 0xFF) * w
                }
                tmpR[tmpBase + ox] = r
                tmpG[tmpBase + ox] = g
                tmpB[tmpBase + ox] = b
            }
        }

        // Pass 2: vertical filter, then scale to [0, 1] and write planar channels.
        val plane = SIZE * SIZE
        for (oy in 0 until SIZE) {
            val start = yTaps.start[oy] - rowFirst
            val wBase = oy * yTaps.maxTaps
            for (ox in 0 until SIZE) {
                var r = 0f
                var g = 0f
                var b = 0f
                for (k in 0 until yTaps.count[oy]) {
                    val idx = (start + k) * SIZE + ox
                    val w = yTaps.weights[wBase + k]
                    r += tmpR[idx] * w
                    g += tmpG[idx] * w
                    b += tmpB[idx] * w
                }
                val o = oy * SIZE + ox
                out[o] = clamp01(r / 255f)
                out[plane + o] = clamp01(g / 255f)
                out[2 * plane + o] = clamp01(b / 255f)
            }
        }
        return out
    }

    private fun clamp01(v: Float): Float = if (v < 0f) 0f else if (v > 1f) 1f else v

    /** Filter taps for one axis: for each output index, a run of source indices and weights. */
    internal class Taps(val start: IntArray, val count: IntArray, val weights: FloatArray, val maxTaps: Int) {
        val minIndex: Int get() = start.min()
        val maxIndex: Int get() = (start.indices).maxOf { start[it] + count[it] - 1 }
    }

    /**
     * Triangle (bilinear) filter taps, PIL style. Output pixel centers map to source
     * coordinates in the crop window. Edge samples clamp to the image.
     */
    internal fun filterTaps(srcStart: Double, srcSide: Double, srcLimit: Int): Taps {
        val scale = srcSide / SIZE
        val support = max(scale, 1.0) // filter radius in source pixels
        val maxTaps = ceil(support).toInt() * 2 + 2
        val start = IntArray(SIZE)
        val count = IntArray(SIZE)
        val weights = FloatArray(SIZE * maxTaps)
        val tmp = DoubleArray(maxTaps)
        for (o in 0 until SIZE) {
            val center = srcStart + (o + 0.5) * scale // continuous coordinate, pixel i covers [i, i+1)
            var lo = floor(center - support).toInt()
            var hi = ceil(center + support).toInt() // exclusive
            lo = max(lo, 0)
            hi = min(hi, srcLimit)
            if (hi <= lo) { // crop window outside the image: use the nearest edge pixel
                lo = min(max(floor(center).toInt(), 0), srcLimit - 1)
                hi = lo + 1
            }
            var n = 0
            var sum = 0.0
            for (i in lo until hi) {
                if (n == maxTaps) break
                val d = abs((i + 0.5 - center) / support)
                val w = if (d < 1.0) 1.0 - d else 0.0
                tmp[n++] = w
                sum += w
            }
            if (sum <= 0.0) { // every tap had zero weight: take the nearest pixel
                val nearest = min(max(floor(center).toInt(), lo), hi - 1)
                start[o] = nearest
                count[o] = 1
                weights[o * maxTaps] = 1f
                continue
            }
            // Drop zero-weight taps at both ends so the run stays tight.
            var first = 0
            while (first < n - 1 && tmp[first] == 0.0) first++
            var last = n - 1
            while (last > first && tmp[last] == 0.0) last--
            start[o] = lo + first
            count[o] = last - first + 1
            for (k in first..last) weights[o * maxTaps + (k - first)] = (tmp[k] / sum).toFloat()
        }
        return Taps(start, count, weights, maxTaps)
    }
}
