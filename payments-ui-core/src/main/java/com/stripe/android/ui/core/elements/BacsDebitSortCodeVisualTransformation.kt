package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

internal object BacsDebitSortCodeVisualTransformation : VisualTransformation {
    private const val SEPARATOR = "-"

    override fun filter(text: AnnotatedString): TransformedText {
        val internalText = text.text
        val transformedText = format(internalText)

        return TransformedText(
            text = AnnotatedString(transformedText),
            offsetMapping = SortCodeOffsetMapping
        )
    }

    private fun format(text: String): String {
        return text.chunked(size = 2).joinToString(separator = SEPARATOR)
    }

    private object SortCodeOffsetMapping : OffsetMapping {
        private const val DIVISIBLE = 2

        override fun originalToTransformed(offset: Int): Int {
            return when (offset) {
                0 -> 0
                else -> {
                    val newOffset = offset + offset / DIVISIBLE

                    return when (offset % DIVISIBLE) {
                        0 -> newOffset - 1
                        else -> newOffset
                    }
                }
            }
        }

        override fun transformedToOriginal(offset: Int): Int {
            return when (offset) {
                0 -> 0
                else -> offset - offset / (DIVISIBLE + 1)
            }
        }
    }
}
