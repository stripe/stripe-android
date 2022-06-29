package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

internal class ExpiryDateVisualTransformation : VisualTransformation {
    private val separator = " / "

    override fun filter(text: AnnotatedString): TransformedText {
        /**
         * Depending on the first number is where the separator will be placed
         * If the first number is 2-9 then the slash will come after the
         * 2, if the first number is 11 or 12 it will be after the second digit,
         * if the number is 01 it will be after the second digit.
         */
        var separatorAfterIndex = 1
        if (text.isNotBlank() && !(text[0] == '0' || text[0] == '1')) {
            separatorAfterIndex = 0
        } else if (text.length > 1 &&
            (text[0] == '1' && requireNotNull(text[1].digitToInt()) > 2)
        ) {
            separatorAfterIndex = 0
        }

        var out = ""
        for (i in text.indices) {
            out += text[i]
            if (i == separatorAfterIndex) {
                out += separator
            }
        }

        val offsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) =
                if (offset <= separatorAfterIndex) {
                    offset
                } else {
                    offset + separator.length
                }

            override fun transformedToOriginal(offset: Int) =
                if (offset <= separatorAfterIndex + 1) {
                    offset
                } else {
                    offset - separator.length
                }
        }

        return TransformedText(AnnotatedString(out), offsetTranslator)
    }
}
