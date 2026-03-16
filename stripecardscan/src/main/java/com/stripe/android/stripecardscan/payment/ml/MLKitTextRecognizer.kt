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
import kotlin.coroutines.resume

/**
 * Regex to find potential card numbers in text. Matches sequences of digits
 * (optionally separated by spaces or dashes) that are 13-19 digits long.
 */
private const val MIN_PAN_LENGTH = 13
private const val MAX_PAN_LENGTH = 19

private val CARD_NUMBER_REGEX = Regex("[0-9](?:[0-9 \\-]{11,22}[0-9])")

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
        return SSDOcr.Prediction(extractCardNumber(text))
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
    }
}
