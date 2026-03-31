package com.stripe.android.stripecardscan.payment.ml

import com.google.common.truth.Truth.assertThat
import com.google.mlkit.vision.text.Text
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class MLKitTextRecognizerTest {

    @Test
    fun `extractPan returns valid 16-digit PAN`() {
        val text = textWith("4242424242424242")

        val result = MLKitTextRecognizer.extractPan(text)

        assertThat(result).isEqualTo("4242424242424242")
    }

    @Test
    fun `extractPan strips spaces from PAN`() {
        val text = textWith("4242 4242 4242 4242")

        val result = MLKitTextRecognizer.extractPan(text)

        assertThat(result).isEqualTo("4242424242424242")
    }

    @Test
    fun `extractPan strips dashes from PAN`() {
        val text = textWith("4242-4242-4242-4242")

        val result = MLKitTextRecognizer.extractPan(text)

        assertThat(result).isEqualTo("4242424242424242")
    }

    @Test
    fun `extractPan finds PAN among other text`() {
        val text = textWith(
            "JOHN DOE",
            "4242 4242 4242 4242",
            "VALID THRU 12/25",
        )

        val result = MLKitTextRecognizer.extractPan(text)

        assertThat(result).isEqualTo("4242424242424242")
    }

    @Test
    fun `extractPan returns null when no PAN found`() {
        val text = textWith("JOHN DOE", "VALID THRU 12/25")

        val result = MLKitTextRecognizer.extractPan(text)

        assertThat(result).isNull()
    }

    @Test
    fun `extractPan returns null for digits that fail Luhn check`() {
        val text = textWith("1234567890123456")

        val result = MLKitTextRecognizer.extractPan(text)

        assertThat(result).isNull()
    }

    @Test
    fun `extractPan returns null for empty text`() {
        val text = textWith()

        val result = MLKitTextRecognizer.extractPan(text)

        assertThat(result).isNull()
    }

    @Test
    fun `extractPan handles 15-digit Amex PAN`() {
        val text = textWith("3782 822463 10005")

        val result = MLKitTextRecognizer.extractPan(text)

        assertThat(result).isEqualTo("378282246310005")
    }

    @Test
    fun `extractPan returns first valid PAN when multiple candidates exist`() {
        val text = textWith(
            "4242 4242 4242 4242",
            "5500 0000 0000 0004",
        )

        val result = MLKitTextRecognizer.extractPan(text)

        assertThat(result).isEqualTo("4242424242424242")
    }

    /**
     * Helper to build a mock [Text] with one block containing lines for each [lineTexts] entry.
     */
    private fun textWith(vararg lineTexts: String): Text {
        val lines = lineTexts.map { lineText ->
            mock<Text.Line>().also { whenever(it.text).thenReturn(lineText) }
        }
        val block = mock<Text.TextBlock>().also { whenever(it.lines).thenReturn(lines) }
        return mock<Text>().also { whenever(it.textBlocks).thenReturn(listOf(block)) }
    }
}
