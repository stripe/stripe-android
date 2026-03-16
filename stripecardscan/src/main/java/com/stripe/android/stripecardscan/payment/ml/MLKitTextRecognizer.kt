package com.stripe.android.stripecardscan.payment.ml

import android.graphics.Bitmap
import androidx.annotation.VisibleForTesting
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.stripe.android.camera.framework.Analyzer
import com.stripe.android.stripecardscan.payment.card.isValidPan
import com.stripe.android.stripecardscan.payment.card.normalizeCardNumber
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.Closeable
import java.util.Calendar
import kotlin.coroutines.resume

/**
 * Regex to find potential card numbers in text. Matches sequences of digits
 * (optionally separated by spaces or dashes) that are 13-19 digits long.
 */
private const val MIN_PAN_LENGTH = 13
private const val MAX_PAN_LENGTH = 19

private val CARD_NUMBER_REGEX = Regex("[0-9](?:[0-9 \\-]{11,22}[0-9])")
private val EXPIRY_REGEX = Regex("""(0[1-9]|1[0-2])\s*[/\-\.]\s*(\d{2}(?:\d{2})?)""")
private const val TWO_DIGIT_YEAR_THRESHOLD = 100
private const val TWO_DIGIT_YEAR_BASE = 2000

/**
 * An analyzer that uses Google ML Kit Text Recognition to extract card numbers
 * from bitmap images. This serves as a fallback when the primary SSD OCR model
 * fails to detect card numbers.
 */
internal class MLKitTextRecognizer(
    private val textRecognizer: TextRecognizer
) : Analyzer<SSDOcr.Input, Any, SSDOcr.Prediction>, Closeable {

    override suspend fun analyze(data: SSDOcr.Input, state: Any): SSDOcr.Prediction {
        val text = recognizeText(data.cardBitmap) ?: return SSDOcr.Prediction(null)
        return SSDOcr.Prediction(pan = extractCardNumber(text), expiry = extractExpiry(text))
    }

    suspend fun analyzeForExpiryOnly(data: SSDOcr.Input): SSDOcr.ExpiryDate? {
        val text = recognizeText(data.cardBitmap) ?: return null
        return extractExpiry(text)
    }

    private suspend fun recognizeText(bitmap: Bitmap): String? {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        return suspendCancellableCoroutine { continuation ->
            textRecognizer.process(inputImage)
                .addOnSuccessListener { result ->
                    continuation.resume(result.text)
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        }
    }

    override fun close() {
        textRecognizer.close()
    }

    companion object {
        fun createTextRecognizer(): TextRecognizer =
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        /**
         * Extract a valid card number from recognized text.
         *
         * Finds candidate card number strings using regex, normalizes them
         * by stripping non-digit characters, and validates each candidate
         * using Luhn check and issuer validation.
         *
         * @return the first valid PAN found, or null if none found
         */
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal fun extractCardNumber(text: String): String? {
            return CARD_NUMBER_REGEX.findAll(text)
                .map { normalizeCardNumber(it.value) }
                .filter { it.length in MIN_PAN_LENGTH..MAX_PAN_LENGTH }
                .firstOrNull { isValidPan(it) }
        }

        /**
         * Extract a valid expiration date from recognized text.
         *
         * Matches MM/YY or MM/YYYY patterns with slash, dash, or dot separators.
         * Validates that the month is 01-12 and the date is not in the past.
         *
         * @return the first valid expiry found, or null if none found
         */
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal fun extractExpiry(text: String): SSDOcr.ExpiryDate? {
            val now = Calendar.getInstance()
            val currentYear = now.get(Calendar.YEAR)
            val currentMonth = now.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based

            return EXPIRY_REGEX.findAll(text)
                .mapNotNull { match ->
                    val month = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
                    val rawYear = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
                    val year = if (rawYear < TWO_DIGIT_YEAR_THRESHOLD) TWO_DIGIT_YEAR_BASE + rawYear else rawYear

                    if (year < currentYear || (year == currentYear && month < currentMonth)) {
                        null // expired
                    } else {
                        SSDOcr.ExpiryDate(month, year)
                    }
                }
                .firstOrNull()
        }
    }
}
