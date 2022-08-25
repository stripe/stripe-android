package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

internal class PostalCodeVisualTransformation(
    val format: PostalCodeConfig.CountryPostalFormat
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        return when (format) {
            is PostalCodeConfig.CountryPostalFormat.CA -> postalForCanada(text)
            is PostalCodeConfig.CountryPostalFormat.US -> postalForUS(text)
            else -> TransformedText(text, OffsetMapping.Identity)
        }
    }

    private fun postalForCanada(text: AnnotatedString): TransformedText {
        var out = ""

        for (i in text.text.indices) {
            out += text.text[i].uppercaseChar()
            if (i == 2) out += " "
        }

        val postalCodeOffsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 5) return offset + 1
                return 7
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 3) return offset
                if (offset <= 6) return offset - 1
                return 6
            }
        }

        return TransformedText(AnnotatedString(out), postalCodeOffsetTranslator)
    }

    private fun postalForUS(text: AnnotatedString): TransformedText {
        var out = ""

        for (i in text.text.indices) {
            out += text.text[i]
            if (i == 4 && i < text.text.length - 1) out += "-"
        }

        val postalCodeOffsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 4) return offset
                if (offset <= 8) return offset + 1
                return 10
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 5) return offset
                if (offset <= 9) return offset - 1
                return 9
            }
        }

        return TransformedText(AnnotatedString(out), postalCodeOffsetTranslator)
    }
}
