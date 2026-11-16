package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

object ImageUtils {
    private val inrFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }

    fun formatInr(amount: Double): String = inrFormat.format(amount.roundToInt())

    fun formatInrExact(amount: Double): String {
        val exactFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }
        return exactFormat.format(amount)
    }

    /**
     * Reads the administrator's real chosen photograph from [uri], scales it safely,
     * and compresses to JPEG (< ~90 KB) as a data URI so the exact uploaded image
     * is persisted in central Firestore and shown identically on every customer device.
     */
    fun compressUriToBase64DataUri(context: Context, uri: Uri, maxDimension: Int = 720): Result<String> {
        return runCatching {
            val resolver = context.contentResolver
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            } ?: error("Unable to open selected image")

            val width = boundsOpts.outWidth
            val height = boundsOpts.outHeight
            if (width <= 0 || height <= 0) error("Invalid image dimensions")

            var sampleSize = 1
            val largest = max(width, height)
            while (largest / sampleSize > maxDimension * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: error("Failed to decode selected image")

            val scale = maxDimension.toFloat() / max(decoded.width, decoded.height).toFloat()
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * scale).roundToInt().coerceAtLeast(1),
                    (decoded.height * scale).roundToInt().coerceAtLeast(1),
                    true
                )
            } else {
                decoded
            }

            var quality = 82
            val baos = ByteArrayOutputStream()
            do {
                baos.reset()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                quality -= 10
            } while (baos.size() > 95_000 && quality >= 32)

            if (scaled !== decoded) decoded.recycle()
            scaled.recycle()

            val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        }
    }

    fun decodeDataUriToImageBitmap(dataUri: String): ImageBitmap? {
        if (!dataUri.startsWith("data:image")) return null
        return try {
            val commaIdx = dataUri.indexOf(',')
            if (commaIdx == -1) return null
            val base64Part = dataUri.substring(commaIdx + 1)
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            bitmap.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}
