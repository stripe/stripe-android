package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PostalCodeVisualTransformation(
    val format: PostalCodeConfig.CountryPostalFormat
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        return when (format) {
            is PostalCodeConfig.CountryPostalFormat.CA -> postalForCanada(text)
            is PostalCodeConfig.CountryPostalFormat.GB -> postalForGB(text)
            else -> TransformedText(text, OffsetMapping.Identity)
        }
    }

    private fun postalForGB(text: AnnotatedString): TransformedText {
        val raw = text.text
        // UK postcodes have a space before the last 3 characters (the inward code)
        // e.g., "SW1X7XL" -> "SW1X 7XL"
        if (raw.length <= 3) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val splitIndex = raw.length - 3
        val out = raw.take(splitIndex) + " " + raw.substring(splitIndex)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return if (offset <= splitIndex) offset else offset + 1
            }

            override fun transformedToOriginal(offset: Int): Int {
                return if (offset <= splitIndex) offset else offset - 1
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
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
}
