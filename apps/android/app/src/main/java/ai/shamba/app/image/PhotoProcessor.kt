package ai.shamba.app.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Decodes a photo with low memory use, applies the EXIF rotation, and saves small copies
 * for the history list. Call from a background dispatcher.
 */
object PhotoProcessor {
    /** Decode so the short side stays at or above this. Enough for the 224 model input and the result screen. */
    private const val DECODE_MIN_SHORT_SIDE = 448
    private const val PHOTO_MAX_SIDE = 960
    private const val THUMB_SIDE = 192

    /** Opens a fresh stream on each call. The photo is read three times: bounds, EXIF, pixels. */
    fun interface StreamSource {
        fun open(): InputStream
    }

    fun decode(source: StreamSource): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        source.open().use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Not an image" }

        var sample = 1
        val shortSide = min(bounds.outWidth, bounds.outHeight)
        while (shortSide / (sample * 2) >= DECODE_MIN_SHORT_SIDE) sample *= 2

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = source.open().use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Could not decode the image")

        val orientation = try {
            source.open().use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        return applyOrientation(decoded, orientation)
    }

    fun pixels(bitmap: Bitmap): IntArray {
        val px = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(px, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return px
    }

    /** Saves a display copy (long side up to 960 px) and a square thumbnail. */
    fun saveCopies(bitmap: Bitmap, photo: File, thumb: File) {
        photo.parentFile?.mkdirs()
        val scale = min(1f, PHOTO_MAX_SIDE.toFloat() / max(bitmap.width, bitmap.height))
        val display = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).roundToInt(), (bitmap.height * scale).roundToInt(), true)
        } else {
            bitmap
        }
        photo.outputStream().use { display.compress(Bitmap.CompressFormat.JPEG, 82, it) }
        if (display !== bitmap) display.recycle()

        val side = min(bitmap.width, bitmap.height)
        val square = Bitmap.createBitmap(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side)
        val small = Bitmap.createScaledBitmap(square, THUMB_SIDE, THUMB_SIDE, true)
        thumb.outputStream().use { small.compress(Bitmap.CompressFormat.JPEG, 75, it) }
        if (small !== square) small.recycle()
        if (square !== bitmap) square.recycle()
    }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                m.postRotate(90f)
                m.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                m.postRotate(270f)
                m.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }
}
