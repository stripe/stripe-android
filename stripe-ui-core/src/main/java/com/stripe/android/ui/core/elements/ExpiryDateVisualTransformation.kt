package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

internal class ExpiryDateVisualTransformation() : VisualTransformation {
    private val separator = " / "

    override fun filter(text: AnnotatedString): TransformedText {
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

        return TransformedText(AnnotatedString(out), creditCardOffsetTranslator)
    }
}
