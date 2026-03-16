package com.stripe.android.stripecardscan.payment.ml

import android.graphics.Bitmap
import androidx.annotation.VisibleForTesting
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.stripe.android.camera.framework.Analyzer
import com.stripe.android.camera.framework.AnalyzerFactory
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
private const val MAX_FUTURE_YEARS = 10

/**
 * An analyzer that uses Google ML Kit Text Recognition to extract card numbers
 * from bitmap images.
 */
internal class MLKitTextRecognizer(
    private val textRecognizer: TextRecognizer
) : Analyzer<SSDOcr.Input, Any, SSDOcr.Prediction>, Closeable {

    override suspend fun analyze(data: SSDOcr.Input, state: Any): SSDOcr.Prediction {
        val textResult = recognizeText(data.cardBitmap) ?: return SSDOcr.Prediction(null)
        return SSDOcr.Prediction(
            pan = extractCardNumber(textResult),
            expiry = extractExpiry(textResult.text)
        )
    }

    suspend fun analyzeForExpiryOnly(data: SSDOcr.Input): SSDOcr.ExpiryDate? {
        val textResult = recognizeText(data.cardBitmap) ?: return null
        return extractExpiry(textResult.text)
    }

    private suspend fun recognizeText(bitmap: Bitmap): Text? {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        return suspendCancellableCoroutine { continuation ->
            textRecognizer.process(inputImage)
                .addOnSuccessListener { result ->
                    continuation.resume(result)
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        }
    }

    override fun close() {
        textRecognizer.close()
    }

    /**
     * Factory that creates [MLKitTextRecognizer] pool instances. A single
     * [MLKitTextRecognizer] is shared across the pool because ML Kit's
     * [TextRecognizer] is thread-safe.
     */
    class Factory :
        AnalyzerFactory<SSDOcr.Input, Any, SSDOcr.Prediction, MLKitTextRecognizer>, Closeable {

        private val shared = MLKitTextRecognizer(createTextRecognizer())

        override suspend fun newInstance(): MLKitTextRecognizer = shared

        override fun close() {
            shared.close()
        }
    }

    companion object {
        fun createTextRecognizer(): TextRecognizer =
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        private const val MIN_DIGITS_PER_GROUP = 4
        private const val STANDARD_GROUP_COUNT = 4 // Visa, MC, etc. — 4 groups of 4 digits
        private const val AMEX_GROUP_COUNT = 3 // Amex — 3 groups (4-6-5)

        /**
         * Extract a valid card number from a structured ML Kit [Text] result.
         *
         * Strategy 1: try the flat-string regex on the full text.
         * Strategy 2: if strategy 1 fails, attempt to assemble a PAN from
         * consecutive digit groups across text blocks/lines.
         *
         * @return the first valid PAN found, or null if none found
         */
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal fun extractCardNumber(textResult: Text): String? {
            return extractCardNumber(textResult.text)
                ?: extractCardNumberFromGroups(textResult)
        }

        /**
         * Assemble a valid card number from consecutive digit groups found
         * across ML Kit text blocks and lines.
         *
         * ML Kit sometimes splits a card number across multiple text blocks
         * (e.g., ["4847 1860", "9511 8770"]). This method collects all digit
         * groups from lines containing 4+ digits and tries joining consecutive
         * runs of 4 groups (16-digit PANs) or 3 groups (15-digit PANs like Amex).
         *
         * @return the first valid PAN assembled from groups, or null
         */
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal fun extractCardNumberFromGroups(textResult: Text): String? {
            val digitGroups = textResult.textBlocks
                .flatMap { block -> block.lines }
                .filter { line -> line.text.count { it.isDigit() } >= MIN_DIGITS_PER_GROUP }
                .joinToString(" ") { it.text }
                .split("\\s+".toRegex())
                .map { it.filter { ch -> ch.isDigit() } }
                .filter { it.isNotEmpty() }

            // Try 4 consecutive groups (Visa, MC, etc. — typically 16 digits)
            for (i in 0..digitGroups.size - STANDARD_GROUP_COUNT) {
                val candidate = digitGroups.subList(i, i + STANDARD_GROUP_COUNT).joinToString("")
                if (candidate.length in MIN_PAN_LENGTH..MAX_PAN_LENGTH && isValidPan(candidate)) {
                    return candidate
                }
            }

            // Try 3 consecutive groups (Amex — typically 15 digits)
            for (i in 0..digitGroups.size - AMEX_GROUP_COUNT) {
                val candidate = digitGroups.subList(i, i + AMEX_GROUP_COUNT).joinToString("")
                if (candidate.length in MIN_PAN_LENGTH..MAX_PAN_LENGTH && isValidPan(candidate)) {
                    return candidate
                }
            }

            return null
        }

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

                    when {
                        year < currentYear || (year == currentYear && month < currentMonth) ->
                            null // expired
                        year > currentYear + MAX_FUTURE_YEARS ->
                            null // too far in the future — likely OCR misread
                        else ->
                            SSDOcr.ExpiryDate(month, year)
                    }
                }
                .firstOrNull()
        }
    }
}
