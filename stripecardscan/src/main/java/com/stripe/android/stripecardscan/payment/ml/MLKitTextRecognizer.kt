package com.stripe.android.stripecardscan.payment.ml

import androidx.annotation.VisibleForTesting
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.stripe.android.camera.framework.Analyzer
import com.stripe.android.camera.framework.AnalyzerFactory
import com.stripe.android.stripecardscan.payment.card.isValidPan
import kotlinx.coroutines.tasks.await
import java.io.Closeable
import java.util.Calendar

/**
 * Regex matching 15-16 contiguous digits (after stripping spaces/dashes).
 */
private val PAN_REGEX = Regex("\\d{15,16}")
private val SEPARATOR_REGEX = Regex("[\\s\\-]")

/**
 * Regex matching slash-separated expiration dates in OCR text, e.g. "01/25" or "01/2025".
 */
private val EXPIRY_SLASH_REGEX = Regex("""\b(\d{1,2})/(\d{2,4})\b""")

/**
 * Fallback regex matching expiration dates with non-slash separators, e.g. "01-25" or "01 25".
 * Anchored for matching individual OCR elements.
 */
private val EXPIRY_SEPARATOR_REGEX = Regex("""^(\d{2})\D{1,3}(\d{2,4})$""")

/**
 * An [Analyzer] that uses Google ML Kit Text Recognition to process card images.
 * Uses [CardOcr.Input] so it can run as an independent loop alongside [SSDOcr].
 *
 * The [TextRecognizer] instance is thread-safe, so a single [MLKitTextRecognizer] can be
 * shared across the analyzer pool.
 */
internal class MLKitTextRecognizer internal constructor(
    private val textRecognizer: TextRecognizer,
) : Analyzer<CardOcr.Input, Any, CardOcr.Prediction>, Closeable {

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun analyze(data: CardOcr.Input, state: Any): CardOcr.Prediction {
        return try {
            val inputImage = InputImage.fromBitmap(data.cardBitmap, 0)
            val text = textRecognizer.process(inputImage).await()
            val pan = extractPan(text)
            val expiry = extractExpiry(text)
            CardOcr.Prediction(
                pan = pan,
                expiryMonth = expiry?.month,
                expiryYear = expiry?.year,
            )
        } catch (e: Exception) {
            CardOcr.Prediction(pan = null)
        }
    }

    override fun close() {
        textRecognizer.close()
    }

    class Factory : AnalyzerFactory<CardOcr.Input, Any, CardOcr.Prediction, MLKitTextRecognizer> {
        override suspend fun newInstance(): MLKitTextRecognizer = MLKitTextRecognizer.newInstance()
    }

    companion object {
        internal fun newInstance(): MLKitTextRecognizer {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            return MLKitTextRecognizer(recognizer)
        }

        /**
         * Extract a valid PAN from ML Kit [Text] output.
         *
         * Uses two strategies:
         * 1. Line-level: strips spaces/dashes and looks for 15-16 contiguous digits
         * 2. Block-pattern: joins element-level tokens and slides over groups looking for
         *    4+4+4+4 (16-digit) or 4+6+5 (15-digit AmEx) patterns
         *
         * Returns the first sequence that passes Luhn + BIN validation via [isValidPan],
         * or null if none is found.
         */
        @VisibleForTesting
        internal fun extractPan(text: Text): String? =
            extractPanFromLines(text) ?: extractPanFromBlocks(text)

        /**
         * Strategy 1: Look for 15-16 contiguous digits within each line after stripping
         * spaces and dashes.
         */
        private fun extractPanFromLines(text: Text): String? {
            for (block in text.textBlocks) {
                for (line in block.lines) {
                    val stripped = line.text.replace(SEPARATOR_REGEX, "")
                    val match = PAN_REGEX.find(stripped)
                    if (match != null && isValidPan(match.value)) {
                        return match.value
                    }
                }
            }
            return null
        }

        /**
         * Strategy 2: Collect digit-only tokens from individual OCR elements and slide
         * over consecutive groups looking for card number patterns.
         *
         * This catches numbers that OCR splits across separate text observations, e.g.
         * "4242 424" and "2 4242 4242" becoming separate elements.
         */
        @Suppress("MagicNumber")
        @VisibleForTesting
        internal fun extractPanFromBlocks(text: Text): String? {
            val tokens = mutableListOf<String>()
            for (block in text.textBlocks) {
                for (line in block.lines) {
                    for (element in line.elements) {
                        val digits = element.text.filter { it.isDigit() }
                        if (digits.isNotEmpty()) {
                            tokens.add(digits)
                        }
                    }
                }
            }

            // 4+4+4+4 pattern (Visa, Mastercard, Discover, etc.)
            if (tokens.size >= 4) {
                for (i in 0..tokens.size - 4) {
                    val candidate = tokens[i] + tokens[i + 1] + tokens[i + 2] + tokens[i + 3]
                    if (isValidPan(candidate)) {
                        return candidate
                    }
                }
            }

            // 4+6+5 pattern (AmEx)
            if (tokens.size >= 3) {
                for (i in 0..tokens.size - 3) {
                    val candidate = tokens[i] + tokens[i + 1] + tokens[i + 2]
                    if (isValidPan(candidate)) {
                        return candidate
                    }
                }
            }

            return null
        }

        /**
         * Extract a valid expiration date from ML Kit [Text] output.
         *
         * Looks for slash-separated dates (e.g. "01/25") at the line level first,
         * then falls back to element-level matching with other separators (e.g. "01-25").
         * Validates that the date is not expired and not more than 10 years in the future.
         *
         * @return a [CardOcr.Expiry], or null if no valid expiry is found.
         */
        @VisibleForTesting
        internal fun extractExpiry(text: Text): CardOcr.Expiry? {
            for (block in text.textBlocks) {
                for (line in block.lines) {
                    // Try slash format on the full line text (handles "VALID THRU 01/25")
                    val lineExpiry = parseExpiry(line.text, EXPIRY_SLASH_REGEX)
                    if (lineExpiry != null && isValidExpiry(lineExpiry)) {
                        return lineExpiry
                    }
                    // Fall back to element-level matching with either regex
                    for (element in line.elements) {
                        val elementExpiry = parseExpiry(element.text, EXPIRY_SLASH_REGEX)
                            ?: parseExpiry(element.text, EXPIRY_SEPARATOR_REGEX)
                        if (elementExpiry != null && isValidExpiry(elementExpiry)) {
                            return elementExpiry
                        }
                    }
                }
            }
            return null
        }

        /**
         * Parse a month and year from the given [text] using the provided [regex].
         *
         * @return a [CardOcr.Expiry], or null if parsing fails.
         */
        @Suppress("MagicNumber")
        @VisibleForTesting
        internal fun parseExpiry(text: String, regex: Regex): CardOcr.Expiry? {
            val match = regex.find(text) ?: return null
            val month = match.groupValues[1].toIntOrNull() ?: return null
            val yearRaw = match.groupValues[2].toIntOrNull() ?: return null
            val year = if (yearRaw < 100) yearRaw + 2000 else yearRaw
            return CardOcr.Expiry(month = month, year = year)
        }

        // Calendar.getInstance() is expensive, so we cache it for the duration of the scan.
        private val cachedCalendar = Calendar.getInstance()
        private val cachedCurrentMonth: Int = cachedCalendar.get(Calendar.MONTH) + 1
        private val cachedCurrentYear: Int = cachedCalendar.get(Calendar.YEAR)

        /**
         * Check whether the given expiration date is valid for OCR purposes.
         *
         * A valid expiry must:
         * - Have a month in 1..12
         * - Not be expired (month/year >= current month/year)
         * - Not be more than 10 years in the future (prevents OCR misreads)
         *
         * [CardOcr.Expiry.year] is expected to be a full 4-digit year.
         * Parameters [currentMonth] and [currentYear] are injectable for testing.
         */
        @Suppress("MagicNumber")
        @VisibleForTesting
        internal fun isValidExpiry(
            expiry: CardOcr.Expiry,
            currentMonth: Int = cachedCurrentMonth,
            currentYear: Int = cachedCurrentYear,
        ): Boolean {
            if (expiry.month !in 1..12) return false
            if (expiry.year < currentYear || (expiry.year == currentYear && expiry.month < currentMonth)) return false
            if (expiry.year > currentYear + 10) return false
            return true
        }
    }
}
