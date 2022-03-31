package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.model.CardBrand

internal class CardNumberVisualTransformation(private val separator: Char) :
    VisualTransformation {

    internal var binBasedMaxPan: Int? = null

    override fun filter(text: AnnotatedString): TransformedText {
        val cardBrand = CardBrand.fromCardNumber(text.text)
        val panLength = binBasedMaxPan ?: cardBrand.getMaxLengthForCardNumber(text.text)
        return if (panLength == 14 || panLength == 15) {
            space4and11(text)
        } else if (panLength == 16) {
            space4and9and14(text)
        } else if (panLength == 19) {
            space4and9and14and19(text)
        } else {
            space4and9and14(text)
        }
    }

    private fun space4and11(text: AnnotatedString): TransformedText {
        var out = ""
        for (i in text.indices) {
            out += text[i]
            if (i == 3 || i == 9) out += separator
        }

        /**
         * The offset translator should ignore the hyphen characters, so conversion from
         *  original offset to transformed text works like
         *  - The 4th char of the original text is 5th char in the transformed text.
         *  - The 13th char of the original text is 15th char in the transformed text.
         *  Similarly, the reverse conversion works like
         *  - The 5th char of the transformed text is 4th char in the original text.
         *  - The 12th char of the transformed text is 10th char in the original text.
         */
        val creditCardOffsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 3) return offset
                if (offset <= 9) return offset + 1
                return offset + 2
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 4) return offset
                if (offset <= 11) return offset - 1
                return offset - 2
            }
        }

        return TransformedText(AnnotatedString(out), creditCardOffsetTranslator)
    }

    private fun space4and9and14(text: AnnotatedString): TransformedText {
        var out = ""
        for (i in text.indices) {
            out += text[i]
            if (i % 4 == 3 && i < 15) out += separator
        }

        /**
         * The offset translator should ignore the hyphen characters, so conversion from
         *  original offset to transformed text works like
         *  - The 4th char of the original text is 5th char in the transformed text.
         *  - The 13th char of the original text is 15th char in the transformed text.
         *  Similarly, the reverse conversion works like
         *  - The 5th char of the transformed text is 4th char in the original text.
         *  - The 12th char of the transformed text is 10th char in the original text.
         */
        val creditCardOffsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 3) return offset
                if (offset <= 7) return offset + 1
                if (offset <= 11) return offset + 2
                return offset + 3
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 4) return offset
                if (offset <= 9) return offset - 1
                if (offset <= 14) return offset - 2
                return offset - 3
            }
        }

        return TransformedText(AnnotatedString(out), creditCardOffsetTranslator)
    }

    private fun space4and9and14and19(text: AnnotatedString): TransformedText {
        var out = ""
        for (i in text.indices) {
            out += text[i]
            if (i % 4 == 3 && i < 19) out += separator
        }

        /**
         * The offset translator should ignore the hyphen characters, so conversion from
         *  original offset to transformed text works like
         *  - The 4th char of the original text is 5th char in the transformed text.
         *  - The 13th char of the original text is 15th char in the transformed text.
         *  Similarly, the reverse conversion works like
         *  - The 5th char of the transformed text is 4th char in the original text.
         *  - The 12th char of the transformed text is 10th char in the original text.
         */
        val creditCardOffsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 3) return offset
                if (offset <= 7) return offset + 1
                if (offset <= 11) return offset + 2
                if (offset <= 15) return offset + 3
                return offset + 4
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 4) return offset
                if (offset <= 9) return offset - 1
                if (offset <= 14) return offset - 2
                if (offset <= 19) return offset - 3
                return offset - 4
            }
        }

        return TransformedText(AnnotatedString(out), creditCardOffsetTranslator)
    }
}
