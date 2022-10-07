package com.stripe.android.financialconnections.ui.visualtransformation

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * [VisualTransformation] that filters out non-numeric characters.
 */
internal class NumbersOnlyTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            text = AnnotatedString(text.text.filter { it.isDigit() }),
            offsetMapping = object : OffsetMapping {
                val count = text.count { it.isDigit().not() }
                override fun originalToTransformed(offset: Int): Int = offset - count
                override fun transformedToOriginal(offset: Int): Int = offset + count
            }
        )
    }
}
