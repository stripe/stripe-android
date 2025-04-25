package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ExpiryDateVisualTransformation(
    private val fallbackExpiryDate: String? = null
) : VisualTransformation {
    private val separator = " / "

    override fun filter(text: AnnotatedString): TransformedText {
        if (text.text == fallbackExpiryDate) return TransformedText(text, OffsetMapping.Identity)
        /**
         * Depending on the first number is where the separator will be placed
         * If the first number is 2-9 then the slash will come after the
         * 2, if the first number is 11 or 12 it will be after the second digit,
         * if the number is 01 it will be after the second digit.
         */
        val canOnlyBeSingleDigitMonth = text.isNotBlank() && !(text[0] == '0' || text[0] == '1')
        val canOnlyBeJanuary = text.length > 1 && text.text.take(2).toInt() > 12
        val isSingleDigitMonth = canOnlyBeSingleDigitMonth || canOnlyBeJanuary

        val lastIndexOfMonth = if (isSingleDigitMonth) 0 else 1

        val output = buildString {
            for ((index, char) in text.withIndex()) {
                append(char)
                if (index == lastIndexOfMonth) {
                    append(separator)
                }
            }
        }

        val outputOffsets = calculateOutputOffsets(output)
        val separatorIndices = calculateSeparatorOffsets(output)

        val offsetTranslator = object : OffsetMapping {

            override fun originalToTransformed(offset: Int): Int {
                return outputOffsets[offset]
            }

            override fun transformedToOriginal(offset: Int): Int {
                val separatorCharactersBeforeOffset = separatorIndices.count { it < offset }
                return offset - separatorCharactersBeforeOffset
            }
        }

        return TransformedText(AnnotatedString(output), offsetTranslator)
    }

    private fun calculateOutputOffsets(output: String): List<Int> {
        val digitOffsets = output.mapIndexedNotNull { index, char ->
            // +1 because we're looking for offsets, not indices
            index.takeIf { char.isDigit() }?.plus(1)
        }

        // We're adding 0 so that the cursor can be placed at the start of the text,
        // and replace the last digit offset with the length of the output. The latter
        // is so that the offsets are set correctly for text such as "4 / ".
        return listOf(0) + digitOffsets.dropLast(1) + output.length
    }

    private fun calculateSeparatorOffsets(output: String): List<Int> {
        return output.mapIndexedNotNull { index, c ->
            index.takeUnless { c.isDigit() }
        }
    }
}
