package com.stripe.android.stripecardscan.payment.ml

import com.google.common.truth.Truth.assertThat
import com.google.mlkit.vision.text.Text
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class MLKitTextRecognizerTest {

    // --- extractPan: line-level strategy ---

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

    // --- extractPan: block-pattern strategy ---

    @Test
    fun `extractPanFromBlocks finds 4+4+4+4 Visa pattern`() {
        val text = textWithElements("4242", "4242", "4242", "4242")

        val result = MLKitTextRecognizer.extractPanFromBlocks(text)

        assertThat(result).isEqualTo("4242424242424242")
    }

    @Test
    fun `extractPanFromBlocks finds AmEx 4+6+5 pattern`() {
        val text = textWithElements("3782", "822463", "10005")

        val result = MLKitTextRecognizer.extractPanFromBlocks(text)

        assertThat(result).isEqualTo("378282246310005")
    }

    @Test
    fun `extractPanFromBlocks returns null for invalid Luhn`() {
        val text = textWithElements("1234", "5678", "9012", "3456")

        val result = MLKitTextRecognizer.extractPanFromBlocks(text)

        assertThat(result).isNull()
    }

    @Test
    fun `extractPanFromBlocks returns null when too few elements`() {
        val text = textWithElements("4242", "4242")

        val result = MLKitTextRecognizer.extractPanFromBlocks(text)

        assertThat(result).isNull()
    }

    @Test
    fun `extractPanFromBlocks skips non-digit text in elements`() {
        val text = textWithElements("JOHN", "4242", "4242", "4242", "4242")

        val result = MLKitTextRecognizer.extractPanFromBlocks(text)

        assertThat(result).isEqualTo("4242424242424242")
    }

    @Test
    fun `extractPan falls back to block strategy when line strategy fails`() {
        // Digits are split across lines so no single line contains a full PAN,
        // but the block strategy can still reassemble elements into 4+4+4+4.
        val text = textWithMultiLineElements(
            listOf("4242", "4242"),  // line text "4242 4242" — only 8 digits
            listOf("4242", "4242"),  // line text "4242 4242" — only 8 digits
        )

        val result = MLKitTextRecognizer.extractPan(text)

        assertThat(result).isEqualTo("4242424242424242")
    }

    @Test
    fun `extractPan block strategy handles messy OCR with interleaved non-PAN digits`() {
        // Realistic OCR mess: expiry and name text scattered around the card number,
        // which is itself split across two lines. The "12/25" produces a junk digit
        // token "1225" that the sliding window must skip past.
        val text = textWithMultiLineElements(
            listOf("VALID", "THRU", "12/25"),  // "1225" junk digit token
            listOf("JOHN", "DOE"),              // no digits, filtered out
            listOf("4242", "4242"),             // first half of PAN
            listOf("4242", "4242"),             // second half of PAN
        )

        val result = MLKitTextRecognizer.extractPan(text)

        assertThat(result).isEqualTo("4242424242424242")
    }

    // --- Helpers ---

    /**
     * Build a mock [Text] with one block containing lines for each [lineTexts] entry.
     * Each line has no elements (empty list).
     */
    private fun textWith(vararg lineTexts: String): Text {
        val lines = lineTexts.map { lineText ->
            mock<Text.Line>().also {
                whenever(it.text).thenReturn(lineText)
                whenever(it.elements).thenReturn(emptyList())
            }
        }
        val block = mock<Text.TextBlock>().also { whenever(it.lines).thenReturn(lines) }
        return mock<Text>().also { whenever(it.textBlocks).thenReturn(listOf(block)) }
    }

    /**
     * Build a mock [Text] with one block, one line, and individual [Text.Element] objects
     * for each entry in [elementTexts]. Useful for testing block-pattern PAN matching.
     */
    private fun textWithElements(vararg elementTexts: String): Text {
        val elements = elementTexts.map { text ->
            mock<Text.Element>().also { whenever(it.text).thenReturn(text) }
        }
        val line = mock<Text.Line>().also {
            whenever(it.text).thenReturn(elementTexts.joinToString(" "))
            whenever(it.elements).thenReturn(elements)
        }
        val block = mock<Text.TextBlock>().also { whenever(it.lines).thenReturn(listOf(line)) }
        return mock<Text>().also { whenever(it.textBlocks).thenReturn(listOf(block)) }
    }

    /**
     * Build a mock [Text] with one block containing multiple lines, each with its own elements.
     * Useful for testing the fallback from line strategy to block strategy: no single line
     * contains a full PAN, but the block strategy collects elements across all lines.
     */
    private fun textWithMultiLineElements(vararg linesElements: List<String>): Text {
        val lines = linesElements.map { elementTexts ->
            val elements = elementTexts.map { text ->
                mock<Text.Element>().also { whenever(it.text).thenReturn(text) }
            }
            mock<Text.Line>().also {
                whenever(it.text).thenReturn(elementTexts.joinToString(" "))
                whenever(it.elements).thenReturn(elements)
            }
        }
        val block = mock<Text.TextBlock>().also { whenever(it.lines).thenReturn(lines) }
        return mock<Text>().also { whenever(it.textBlocks).thenReturn(listOf(block)) }
    }
}
